package com.frozy.mindmap.mapeditor

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.frozy.mindmap.MindMapApplication
import com.frozy.mindmap.mapeditor.models.MapItem
import com.frozy.mindmap.mapeditor.space.camera.SpaceCameraState
import com.frozy.mindmap.mapeditor.space.models.SpaceObject
import com.frozy.mindmap.mapeditor.space.node.constants.NodeValues.MIN_NODE_HEIGHT
import com.frozy.mindmap.mapeditor.space.node.constants.NodeValues.MIN_NODE_WIDTH
import com.frozy.mindmap.mapeditor.space.node.resizehandle.NodeResizeHandleType
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

class MapEditorViewModel(application: Application) : AndroidViewModel(application) {

    //just in case I need context
    val context = getApplication<Application>()

    //typecast to MindMapApplication (Application inherits to it)
    val repository = (application as MindMapApplication).mapRepository

    private val _isEditorModeEnabled = MutableStateFlow(value = false)
    val isEditorModeEnabled: StateFlow<Boolean> = _isEditorModeEnabled.asStateFlow()
    fun changeEditorModeState(value: Boolean) {
        _isEditorModeEnabled.update { value }
    }

    private val _mapItemPagerList = MutableStateFlow(value = emptyList<MapItem>())
    val mapItemPagerList: StateFlow<List<MapItem>> = _mapItemPagerList.asStateFlow()

    //includes nodes from other spaces offscreen
    val allSelectedNodes: StateFlow<List<SpaceObject.Node>> =
        mapItemPagerList.map { mipl ->
            mipl
                .filterIsInstance<MapItem.Space>()
                .flatMap { it.objectInfo.filterIsInstance<SpaceObject.Node>() }
                .filter { it.isSelected }
        }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    //these control the visibility of some ModalBottomSheets: these Are in the VM because they are used across different
    // files and it is most convenient to do it like this
    private val _isItemAdderSheetVisible = MutableStateFlow(value = false)
    val isItemAdderSheetVisible = _isItemAdderSheetVisible.asStateFlow()
    fun changeNodeSheetVisibility(value: Boolean) {
        _isItemAdderSheetVisible.update { value }
    }

    private val _isNodeEditorSheetVisible = MutableStateFlow(value = false)
    val isNodeEditorSheetVisible = _isNodeEditorSheetVisible.asStateFlow()
    fun changeNodeEditorSheetVisibility(value: Boolean) {
        _isNodeEditorSheetVisible.update { value }
    }


    private val _initialPageIndex = MutableStateFlow(value = 0)
    val initialPageIndex: StateFlow<Int> = _initialPageIndex.asStateFlow()

    private val _isMapLoadingFinished = MutableStateFlow(value = false)
    val isMapLoadingFinished: StateFlow<Boolean> = _isMapLoadingFinished.asStateFlow()



    //-###---- Map saving and loading ----###-//

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
            _mapItemPagerList.update {
                loadedMapData?.items ?: emptyList()
            }
            _initialPageIndex.update {
                loadedMapData?.lastPageIndex ?: 0
            }
            _isMapLoadingFinished.update { true }
        }
    }



    //-###---- Map misc operations ----###-//

    fun updateCurrentPageIndex(currentPage: Int) {
        _initialPageIndex.update { currentPage }
    }



    //-###---- Map add and delete MapItems ----###-//

    fun addMapItem(mapItem: MapItem) {
        _mapItemPagerList.update { mipl ->
            mipl + mapItem
        }
    }

    fun deleteMapItem(mapItemUUID: UUID) {
        _mapItemPagerList.update { mipl ->
            mipl.filterNot {
                it.uuid == mapItemUUID
            }
        }
    }



    //-###---- Spaces: Node operations ----###-//

    fun addNode(
        spaceUUID: UUID,
        newNode: SpaceObject.Node
    ) {
        _mapItemPagerList.update { mipl ->
            mipl.map { mapItem ->
                if (mapItem !is MapItem.Space || mapItem.uuid != spaceUUID) return@map mapItem
                return@map mapItem.copy(
                    objectInfo = mapItem.objectInfo + newNode
                )
            }
        }
    }

    fun deleteSpaceNodes(
        spaceUUID: UUID,
        vararg nodeUUIDs: UUID
    ) {
        _mapItemPagerList.update { mipl ->
            mipl.map { mapItem ->
                if (mapItem !is MapItem.Space || mapItem.uuid != spaceUUID) return@map mapItem
                return@map mapItem.copy(
                    objectInfo = mapItem.objectInfo.filterNot { obj ->
                        when(obj) {
                            is SpaceObject.Node -> obj.uuid in nodeUUIDs
                            is SpaceObject.NodeLink -> obj.toNodeUUID in nodeUUIDs || obj.fromNodeUUID in nodeUUIDs
                        }
                    }
                )
            }
        }
    }

    fun changeNode(
        spaceUUID: UUID,
        nodeUUID: UUID,
        newNode: SpaceObject.Node
    ) {
        _mapItemPagerList.update { mipl ->
            mipl.map { mapItem ->
                if (mapItem !is MapItem.Space || mapItem.uuid != spaceUUID) return@map mapItem
                return@map mapItem.copy(
                    objectInfo = mapItem.objectInfo.map { obj ->
                        if (obj.uuid != nodeUUID || obj !is SpaceObject.Node) return@map obj
                        return@map newNode
                    }
                )
            }
        }
    }

    fun selectNode(
        spaceUUID: UUID,
        nodeUUID: UUID
    ) {
        _mapItemPagerList.update { mipl ->
            mipl.map { mapItem ->
                if (mapItem is MapItem.Space && mapItem.uuid == spaceUUID) {
                    mapItem.copy(
                        objectInfo = mapItem.objectInfo.map { obj ->
                            if(obj.uuid != nodeUUID || obj !is SpaceObject.Node) return@map obj
                            return@map when(obj){
                                is SpaceObject.Node.TextNode -> {
                                    obj.copy(
                                        isSelected = true
                                    )
                                }
                                is SpaceObject.Node.ImageNode -> {
                                    obj.copy(
                                        isSelected = true
                                    )
                                }
                            }
                        }
                    )
                } else {
                    return@map mapItem
                }
            }
        }
    }

    fun deselectAllNodes(
        spaceUUID: UUID,
    ) {
        _mapItemPagerList.update { mipl ->
            mipl.map { mapItem ->
                if (mapItem is MapItem.Space && mapItem.uuid == spaceUUID) {
                    mapItem.copy(
                        objectInfo = mapItem.objectInfo.map { obj ->
                            if(obj !is SpaceObject.Node) return@map obj
                            return@map when(obj){
                                is SpaceObject.Node.ImageNode -> {
                                    obj.copy(
                                        isSelected = false
                                    )
                                }
                                is SpaceObject.Node.TextNode -> {
                                    obj.copy(
                                        isSelected = false
                                    )
                                }
                            }
                        }
                    )
                } else {
                    return@map mapItem
                }
            }
        }
    }


    fun moveSpaceNode(
        spaceUUID: UUID,
        nodeUUID: UUID,
        delta: Offset
    ) {
        _mapItemPagerList.update { mipl ->
            mipl.map { mapItem ->
                if (mapItem.uuid != spaceUUID || mapItem !is MapItem.Space) {
                    return@map mapItem
                }
                mapItem.copy(
                    objectInfo = mapItem.objectInfo.map { node ->
                        if (node.uuid != nodeUUID || node !is SpaceObject.Node) return@map node

                        when(node){
                            is SpaceObject.Node.ImageNode -> {
                                node.copy(offset = node.offset + delta)
                            }
                            is SpaceObject.Node.TextNode -> {
                                node.copy(offset = node.offset + delta)
                            }
                        }
                    }
                )
            }
        }
    }

    fun resizeSpaceNode(
        spaceUUID: UUID,
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
                if (mapItem.uuid != spaceUUID || mapItem !is MapItem.Space) return@map mapItem
                mapItem.copy(
                    objectInfo = mapItem.objectInfo.map { node ->
                        if (node.uuid != nodeUUID || node !is SpaceObject.Node) return@map node

                        val newWidth: Float
                        val newHeight: Float
                        val newOffsetX: Float
                        val newOffsetY: Float

                        //different calculations for different handleTypes
                        when (handleType) {
                            NodeResizeHandleType.TOP_LEFT -> {
                                newWidth = (startNodeWidth - worldDragDelta.x).coerceAtLeast(minWidth)
                                newHeight = (startNodeHeight - worldDragDelta.y).coerceAtLeast(minHeight)
                                newOffsetX = startNodeOffset.x + startNodeWidth - newWidth
                                newOffsetY = startNodeOffset.y + startNodeHeight - newHeight
                            }

                            NodeResizeHandleType.TOP_RIGHT -> {
                                newWidth = (startNodeWidth + worldDragDelta.x).coerceAtLeast(minWidth)
                                newHeight = (startNodeHeight - worldDragDelta.y).coerceAtLeast(minHeight)
                                newOffsetX = startNodeOffset.x
                                newOffsetY = startNodeOffset.y + startNodeHeight - newHeight
                            }

                            NodeResizeHandleType.BOTTOM_LEFT -> {
                                newWidth = (startNodeWidth - worldDragDelta.x).coerceAtLeast(minWidth)
                                newHeight = (startNodeHeight + worldDragDelta.y).coerceAtLeast(minHeight)
                                newOffsetX = startNodeOffset.x + startNodeWidth - newWidth
                                newOffsetY = startNodeOffset.y
                            }

                            NodeResizeHandleType.BOTTOM_RIGHT -> {
                                newWidth = (startNodeWidth + worldDragDelta.x).coerceAtLeast(minWidth)
                                newHeight = (startNodeHeight + worldDragDelta.y).coerceAtLeast(minHeight)
                                newOffsetX = startNodeOffset.x
                                newOffsetY = startNodeOffset.y
                            }
                        }

                        when(node){
                            is SpaceObject.Node.ImageNode -> {
                                node.copy(
                                    width = newWidth,
                                    height = newHeight,
                                    offset = Offset(x = newOffsetX, y = newOffsetY)
                                )
                            }
                            is SpaceObject.Node.TextNode -> {
                                node.copy(
                                    width = newWidth,
                                    height = newHeight,
                                    offset = Offset(x = newOffsetX, y = newOffsetY)
                                )
                            }
                        }
                    }
                )
            }
        }
    }



    //-###---- Spaces: NodeLink operations ----###-//

    fun addNodeLink(
        spaceUUID: UUID,
        fromNodeUUID: UUID,
        fromNodeSide: NodeSideType,
        toNodeUUID: UUID,
        toNodeSide: NodeSideType
    ) {
        _mapItemPagerList.update { mipl ->
            mipl.map { mapItem ->
                //cannot be the same to avoid creating weird looking links
                if (fromNodeSide == toNodeSide) return@map mapItem

                if (mapItem.uuid != spaceUUID || mapItem !is MapItem.Space) return@map mapItem

                val alreadyExists = mapItem.objectInfo.filterIsInstance<SpaceObject.NodeLink>().any { link ->
                    link.fromNodeSide == fromNodeSide &&
                    link.toNodeSide == toNodeSide &&
                    link.toNodeUUID == toNodeUUID &&
                    link.fromNodeUUID == fromNodeUUID
                }
                if (alreadyExists) return@map mapItem

                mapItem.copy(
                    objectInfo = mapItem.objectInfo + SpaceObject.NodeLink(
                        fromNodeUUID = fromNodeUUID,
                        fromNodeSide = fromNodeSide,
                        toNodeUUID = toNodeUUID,
                        toNodeSide = toNodeSide
                    )
                )
            }
        }
    }

    fun deleteNodeLink(
        spaceUUID: UUID,
        linkUUID: UUID
    ) {
        _mapItemPagerList.update { mipl ->
            mipl.map { mapItem ->
                if (mapItem.uuid != spaceUUID || mapItem !is MapItem.Space) return@map mapItem
                mapItem.copy(
                    objectInfo = mapItem.objectInfo
                        .filterIsInstance<SpaceObject.NodeLink>()
                        .filterNot { it.uuid == linkUUID }
                )
            }
        }
    }

    //-###---- Spaces: Misc operations ----###-//

    fun updateSpaceCamera(
        spaceUUID: UUID,
        camera: SpaceCameraState
    ) {
        _mapItemPagerList.update { mipl ->
            mipl.map { mapItem ->
                if (mapItem is MapItem.Space && mapItem.uuid == spaceUUID) {
                    mapItem.copy(cameraState = camera)
                } else {
                    mapItem
                }
            }
        }
    }

    //-###---- Note operations ----###-//

    fun changeNoteTitle(
        noteUUID: UUID,
        newTitle: String
    ) {
        _mapItemPagerList.update { mipl ->
            mipl.map { mapItem ->
                if (mapItem is MapItem.Note && mapItem.uuid == noteUUID) {
                    return@map mapItem.copy(
                        titleText = newTitle
                    )
                } else {
                    return@map mapItem
                }
            }
        }
    }

    fun changeNoteContent(
        noteUUID: UUID,
        newContent: String
    ) {
        _mapItemPagerList.update { mipl ->
            mipl.map { mapItem ->
                if (mapItem is MapItem.Note && mapItem.uuid == noteUUID) {

                    return@map mapItem.copy(
                        contentText = newContent
                    )
                } else return@map mapItem
            }
        }
    }
}
