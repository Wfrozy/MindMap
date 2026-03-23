package com.frozy.mindmap.mapeditor

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.frozy.mindmap.mapeditor.space.models.MapItem
import com.frozy.mindmap.mapeditor.space.models.MapItemObject
import com.frozy.mindmap.mapeditor.space.models.ResizeHandleType
import com.frozy.mindmap.mapeditor.space.models.SpaceCameraState
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
            started = SharingStarted.WhileSubscribed(),
            initialValue = emptyList()
        )

    //these control the visibility of some ModalBottomSheets: these Are in the VM because they are used across different
    // files and it is most convenient to do it like this
    private val _isItemAdderSheetVisible = MutableStateFlow(value = false)
    val isItemAdderSheetVisible = _isItemAdderSheetVisible.asStateFlow()
    fun changeNodeSheetVisibility(value: Boolean) { _isItemAdderSheetVisible.update { value }}

    private val _isNodeEditorSheetVisible = MutableStateFlow(value = false)
    val isNodeEditorSheetVisible = _isNodeEditorSheetVisible.asStateFlow()
    fun changeNodeEditorSheetVisibility(value: Boolean) { _isNodeEditorSheetVisible.update { value }}

    init { //todo file stuff
        viewModelScope.launch {

        }
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

    fun miplRemoveSpaceNodesFromSpace(
        mapItemUUID: UUID,
        vararg nodeUUIDs: UUID
    ) {
        _mapItemPagerList.update { list ->
            list.map { mapItem ->
                if (mapItem is MapItem.Space && mapItem.uuid == mapItemUUID) {
                    return@map mapItem.copy(
                        spaceNodeInfo = mapItem.spaceNodeInfo.filterNot {
                            it.uuid in nodeUUIDs
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
        handleType: ResizeHandleType,
        worldDragDelta: Offset,
        startNodeWidth: Float,
        startNodeHeight: Float,
        startNodeOffset: Offset
    ) {
        val minWidth = 100f
        val minHeight = 60f

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
                            ResizeHandleType.TOP_LEFT -> {
                                newWidth  = (startNodeWidth  - worldDragDelta.x).coerceAtLeast(minWidth)
                                newHeight = (startNodeHeight - worldDragDelta.y).coerceAtLeast(minHeight)
                                newOffsetX = startNodeOffset.x + startNodeWidth  - newWidth
                                newOffsetY = startNodeOffset.y + startNodeHeight - newHeight
                            }
                            ResizeHandleType.TOP_RIGHT -> {
                                newWidth  = (startNodeWidth  + worldDragDelta.x).coerceAtLeast(minWidth)
                                newHeight = (startNodeHeight - worldDragDelta.y).coerceAtLeast(minHeight)
                                newOffsetX = startNodeOffset.x
                                newOffsetY = startNodeOffset.y + startNodeHeight - newHeight
                            }
                            ResizeHandleType.BOTTOM_LEFT -> {
                                newWidth  = (startNodeWidth  - worldDragDelta.x).coerceAtLeast(minWidth)
                                newHeight = (startNodeHeight + worldDragDelta.y).coerceAtLeast(minHeight)
                                newOffsetX = startNodeOffset.x + startNodeWidth - newWidth
                                newOffsetY = startNodeOffset.y
                            }
                            ResizeHandleType.BOTTOM_RIGHT -> {
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

    fun updateArrowDragPreview(
        fromNodeUUID: UUID,
        screenPos: Offset,
        camera: SpaceCameraState
    ) {
    }

    fun miplCreateEdge(
        mapItemUUID: UUID,
        fromNodeUUID: UUID,
        toNodeUUID: UUID
    ){

    }

    fun clearArrowDragPreview(){}

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

}

