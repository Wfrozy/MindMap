package com.frozy.mindmap.mapeditor

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.frozy.mindmap.MindMapApplication
import com.frozy.mindmap.mapeditor.models.MapItem
import com.frozy.mindmap.mapeditor.models.MapItemObject
import com.frozy.mindmap.mapeditor.space.node.resizehandle.NodeResizeHandleType
import com.frozy.mindmap.mapeditor.space.camera.SpaceCameraState
import com.frozy.mindmap.mapeditor.space.node.constants.NodeValues.MIN_NODE_HEIGHT
import com.frozy.mindmap.mapeditor.space.node.constants.NodeValues.MIN_NODE_WIDTH
import com.frozy.mindmap.mapeditor.space.node.side.NodeSideType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

//todo fix IndexOutOfBoundsCrash when a map item is deleted
//todo fix crash when you delete the last map item

class MapEditorViewModel(application: Application) : AndroidViewModel(application) {

    //just in case I need context
    val context = getApplication<Application>()

    //typecast to MindMapApplication (Application inherits to it)
    val repository = (application as MindMapApplication).mapRepository

    private val _isEditorModeEnabled = MutableStateFlow(value = false)
    val isEditorModeEnabled: StateFlow<Boolean> = _isEditorModeEnabled.asStateFlow()
    fun changeEditorModeState(value: Boolean) { _isEditorModeEnabled.update { value } }

    private val _mapItemPagerList = MutableStateFlow(value = emptyList<MapItem>())
    val mapItemPagerList: StateFlow<List<MapItem>> = _mapItemPagerList.asStateFlow()

    val allSelectedNodes: StateFlow<List<MapItemObject.SpaceNode>> =
        mapItemPagerList.map { mipl ->
            mipl
                .filterIsInstance<MapItem.Space>()
                .flatMap { it.spaceNodeInfo }
                .filter { it.isSelected }
        }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

//    val allOccupiedNodeSides: StateFlow<Map<UUID, Set<NodeSideType>>> =
//        mapItemPagerList.map { mipl ->
//            mipl
//                .filterIsInstance<MapItem.Space>()
//                .flatMap { space -> space.nodeLinks }
//                .groupBy { nodeLink -> nodeLink.toNodeUUID }
//                .mapValues { (_, links) ->
//                    links.map { it.toNodeSide }.toSet()
//                }
//        }
//        .distinctUntilChanged()
//        .stateIn(
//            scope = viewModelScope,
//            started = SharingStarted.WhileSubscribed(5000),
//            initialValue = emptyMap()
//        )

    //these control the visibility of some ModalBottomSheets: these Are in the VM because they are used across different
    // files and it is most convenient to do it like this
    private val _isItemAdderSheetVisible = MutableStateFlow(value = false)
    val isItemAdderSheetVisible = _isItemAdderSheetVisible.asStateFlow()
    fun changeNodeSheetVisibility(value: Boolean) { _isItemAdderSheetVisible.update { value }}

    private val _isNodeEditorSheetVisible = MutableStateFlow(value = false)
    val isNodeEditorSheetVisible = _isNodeEditorSheetVisible.asStateFlow()
    fun changeNodeEditorSheetVisibility(value: Boolean) { _isNodeEditorSheetVisible.update { value }}



    private val _initialPageIndex = MutableStateFlow(value = 0)
    val initialPageIndex: StateFlow<Int> = _initialPageIndex.asStateFlow()

    private val _isMapLoadingFinished = MutableStateFlow(value = false)
    val isMapLoadingFinished: StateFlow<Boolean> = _isMapLoadingFinished.asStateFlow()

    fun occupiedSidesForNode(
        mapItemUUID: UUID,
        nodeUUID: UUID
    ): StateFlow<Set<NodeSideType>> {
        return mapItemPagerList
            .map { mipl ->
                val space = mipl
                    .filterIsInstance<MapItem.Space>()
                    .firstOrNull { it.uuid == mapItemUUID }
                    ?: return@map emptySet()

                space.nodeLinkInfo
                    .filter { it.toNodeUUID == nodeUUID }
                    .map { it.toNodeSide }
                    .toSet()
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = emptySet()
            )
    }


    fun miplAddMapItem(mapItem: MapItem){
        _mapItemPagerList.update { list ->
            list + mapItem
        }
    }

    fun miplRemoveMapItem(mapItemUUID: UUID){
        _mapItemPagerList.update { list ->
            list.filterNot { it.uuid == mapItemUUID }
        }
    }

    fun miplAddSpaceNodeToSpace(
        mapItemUUID: UUID,
        node: MapItemObject.SpaceNode
    ){
        _mapItemPagerList.update { list ->
            list.map { mapItem ->
                if (mapItem is MapItem.Space && mapItem.uuid == mapItemUUID) {
                    return@map mapItem.copy(
                        spaceNodeInfo = mapItem.spaceNodeInfo + node
                    )
                } else {
                    return@map mapItem
                }
            }
        }
    }

    fun miplChangeSpaceNode(
        mapItemUUID: UUID,
        nodeUUID: UUID,
        newNode: MapItemObject.SpaceNode
    ){
        _mapItemPagerList.update { list ->
            list.map { mapItem ->
                if(mapItem is MapItem.Space && mapItem.uuid == mapItemUUID){
                    return@map mapItem.copy(
                        spaceNodeInfo = mapItem.spaceNodeInfo.map { node ->
                            if (node.uuid == nodeUUID) {
                                return@map newNode
                            } else return@map node
                        }
                    )
                } else {
                    return@map mapItem
                }
            }
        }
    }

    fun miplSelectSpaceNode(
        mapItemUUID: UUID,
        nodeUUID: UUID
    ) {
        _mapItemPagerList.update { list ->
            list.map { mapItem ->
                if (mapItem is MapItem.Space && mapItem.uuid == mapItemUUID) {
                    mapItem.copy(
                        spaceNodeInfo = mapItem.spaceNodeInfo.map { node ->
                            return@map node.copy(
                                isSelected = node.uuid == nodeUUID
                            )
                        }
                    )
                } else {
                    return@map mapItem
                }
            }
        }
    }

    fun miplDeselectAllSpaceNodes(
        mapItemUUID: UUID,
    ) {
        _mapItemPagerList.update { list ->
            list.map { mapItem ->
                if (mapItem is MapItem.Space && mapItem.uuid == mapItemUUID) {
                    mapItem.copy(
                        spaceNodeInfo = mapItem.spaceNodeInfo.map { node ->
                            return@map node.copy(
                                isSelected = false
                            )
                        }
                    )
                } else {
                    return@map mapItem
                }
            }
        }
    }

    fun miplRemoveSpaceNodes(
        mapItemUUID: UUID,
        vararg nodeUUIDs: UUID
    ) {
        _mapItemPagerList.update { list ->
            list.map { mapItem ->
                if (mapItem is MapItem.Space && mapItem.uuid == mapItemUUID) {
                    return@map mapItem.copy(
                        spaceNodeInfo = mapItem.spaceNodeInfo.filterNot {
                            it.uuid in nodeUUIDs
                        },
                        nodeLinkInfo = mapItem.nodeLinkInfo.filterNot { link ->
                            link.toNodeUUID in nodeUUIDs ||
                            link.fromNodeUUID in nodeUUIDs
                        }
                    )
                } else {
                    return@map mapItem
                }
            }
        }
    }

    fun miplMoveSpaceNode(
        mapItemUUID: UUID,
        nodeUUID: UUID,
        delta: Offset
    ) {
        _mapItemPagerList.update { mipl ->
            mipl.map { mapItem ->
                if (mapItem.uuid != mapItemUUID || mapItem !is MapItem.Space) {
                    return@map mapItem
                }
                mapItem.copy(
                    spaceNodeInfo = mapItem.spaceNodeInfo.map { node ->
                        if (node.uuid == nodeUUID) {
                            node.copy(offset = node.offset + delta)
                        } else {
                            node
                        }
                    }
                )
            }
        }
    }

    fun miplResizeSpaceNode(
        mapItemUUID: UUID,
        nodeUUID: UUID,
        handleType: NodeResizeHandleType,
        worldDragDelta: Offset,
        startNodeWidth: Float,
        startNodeHeight: Float,
        startNodeOffset: Offset
    ) {
        val minWidth = MIN_NODE_WIDTH
        val minHeight = MIN_NODE_HEIGHT

        _mapItemPagerList.update { mipl ->
            mipl.map { mapItem ->
                if (mapItem.uuid != mapItemUUID || mapItem !is MapItem.Space) return@map mapItem
                mapItem.copy(
                    spaceNodeInfo = mapItem.spaceNodeInfo.map { node ->
                        if (node.uuid != nodeUUID) return@map node

                        val newWidth: Float
                        val newHeight: Float
                        val newOffsetX: Float
                        val newOffsetY: Float

                        //different calculations for different handleTypes
                        when (handleType) {
                            NodeResizeHandleType.TOP_LEFT -> {
                                newWidth  = (startNodeWidth  - worldDragDelta.x).coerceAtLeast(minWidth)
                                newHeight = (startNodeHeight - worldDragDelta.y).coerceAtLeast(minHeight)
                                newOffsetX = startNodeOffset.x + startNodeWidth  - newWidth
                                newOffsetY = startNodeOffset.y + startNodeHeight - newHeight
                            }
                            NodeResizeHandleType.TOP_RIGHT -> {
                                newWidth  = (startNodeWidth  + worldDragDelta.x).coerceAtLeast(minWidth)
                                newHeight = (startNodeHeight - worldDragDelta.y).coerceAtLeast(minHeight)
                                newOffsetX = startNodeOffset.x
                                newOffsetY = startNodeOffset.y + startNodeHeight - newHeight
                            }
                            NodeResizeHandleType.BOTTOM_LEFT -> {
                                newWidth  = (startNodeWidth  - worldDragDelta.x).coerceAtLeast(minWidth)
                                newHeight = (startNodeHeight + worldDragDelta.y).coerceAtLeast(minHeight)
                                newOffsetX = startNodeOffset.x + startNodeWidth - newWidth
                                newOffsetY = startNodeOffset.y
                            }
                            NodeResizeHandleType.BOTTOM_RIGHT -> {
                                newWidth  = (startNodeWidth  + worldDragDelta.x).coerceAtLeast(minWidth)
                                newHeight = (startNodeHeight + worldDragDelta.y).coerceAtLeast(minHeight)
                                newOffsetX = startNodeOffset.x
                                newOffsetY = startNodeOffset.y
                            }
                        }

                        node.copy(
                            width  = newWidth,
                            height = newHeight,
                            offset = Offset(x = newOffsetX, y = newOffsetY)
                        )
                    }
                )
            }
        }
    }

    fun miplCreateNodeLink(
        mapItemUUID: UUID,
        fromNodeUUID: UUID,
        fromNodeSide: NodeSideType,
        toNodeUUID: UUID,
        toNodeSide: NodeSideType
    ) {
        _mapItemPagerList.update { mipl ->
            mipl.map { mapItem ->
                //cannot be the same to avoid creating weird looking links
                if(fromNodeSide == toNodeSide) return@map mapItem

                if (mapItem.uuid != mapItemUUID || mapItem !is MapItem.Space) return@map mapItem

                val alreadyExists = mapItem.nodeLinkInfo.any { link ->
                    link.fromNodeSide == fromNodeSide &&
                    link.toNodeSide == toNodeSide
                }
                if (alreadyExists) return@map mapItem

                mapItem.copy(
                    nodeLinkInfo = mapItem.nodeLinkInfo + MapItemObject.SpaceNodeLink(
                        fromNodeUUID = fromNodeUUID,
                        fromNodeSide = fromNodeSide,
                        toNodeUUID = toNodeUUID,
                        toNodeSide = toNodeSide
                    )
                )
            }
        }
    }

    fun miplRemoveNodeLink(
        mapItemUUID: UUID,
        linkUUID: UUID
    ) {
        _mapItemPagerList.update { mipl ->
            mipl.map { mapItem ->
                if (mapItem.uuid != mapItemUUID || mapItem !is MapItem.Space) return@map mapItem
                mapItem.copy(
                    nodeLinkInfo = mapItem.nodeLinkInfo.filterNot { it.uuid == linkUUID }
                )
            }
        }
    }

    fun miplUpdateSpaceCamera(
        mapItemUUID: UUID,
        camera: SpaceCameraState
    ) {
        _mapItemPagerList.update { list ->
            list.map { mapItem ->
                if (mapItem is MapItem.Space && mapItem.uuid == mapItemUUID) {
                    mapItem.copy(cameraState = camera)
                } else {
                    mapItem
                }
            }
        }
    }

    //Note stuff --------------------------

    fun miplChangeNoteTitle(
        mapItemUUID: UUID,
        newTitle: String
    ) {
        _mapItemPagerList.update { list ->
            list.map { mapItem ->
                if (mapItem is MapItem.Note && mapItem.uuid == mapItemUUID) {
                    return@map mapItem.copy(
                        titleText = newTitle
                    )
                } else {
                    return@map mapItem
                }
            }
        }
    }

    fun miplChangeNoteContent(
        mapItemUUID: UUID,
        newContent: String
    ) {
        _mapItemPagerList.update { list ->
            list.map { mapItem ->
                if (mapItem is MapItem.Note && mapItem.uuid == mapItemUUID) {

                    return@map mapItem.copy(
                        contentText = newContent
                    )
                } else return@map mapItem
            }
        }
    }

    fun updateCurrentPageIndex(currentPage: Int) {
        _initialPageIndex.update { currentPage }
    }

    fun saveMap(entryUUID: UUID) {
        viewModelScope.launch {
            repository.saveMap(
                entryUUID,
                mapItems = _mapItemPagerList.value,
                lastPageIndex = _initialPageIndex.value
            )
        }
    }

    fun loadMap(entryUUID: UUID) {
        viewModelScope.launch {
            val loadedMapData = repository.loadMap(
                entryUUID,
                lastPageIndex = _initialPageIndex.value
            )
            //todo toasts
            _mapItemPagerList.update {
                loadedMapData?.items ?: emptyList()
            }
            _initialPageIndex.update {
                loadedMapData?.lastPageIndex ?: 0
            }
            _isMapLoadingFinished.update { true }
        }
    }
}

