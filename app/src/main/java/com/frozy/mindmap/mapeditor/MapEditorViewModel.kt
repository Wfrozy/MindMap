package com.frozy.mindmap.mapeditor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.frozy.mindmap.mapeditor.models.MapItem
import com.frozy.mindmap.mapeditor.models.MapItemObject
import com.frozy.mindmap.mapeditor.models.NodeLayout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class MapEditorViewModel(application: Application) : AndroidViewModel(application) {

    //just in case I need context
    val context = getApplication<Application>()
    private val _isEditorModeEnabled = MutableStateFlow(value = false)
    val isEditorModeEnabled: StateFlow<Boolean> = _isEditorModeEnabled.asStateFlow()
    fun changeEditorModeState(value: Boolean) { _isEditorModeEnabled.value = value }


    private val _mapItemPagerList = MutableStateFlow(value = emptyList<MapItem>())
    val mapItemPagerList: StateFlow<List<MapItem>> = _mapItemPagerList.asStateFlow()

    init { //todo
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

    fun miplRemoveSpaceNodeFromSpace(
        mapItemUUID: UUID,
        nodeUUID: UUID
    ){
        _mapItemPagerList.update { list ->
            list.map { mapItem ->
                if (mapItem is MapItem.Space && mapItem.uuid == mapItemUUID) {
                    return@map mapItem.copy(
                        spaceNodeInfo = mapItem.spaceNodeInfo.filterNot {
                            it.uuid == nodeUUID
                        }
                    )
                } else {
                    return@map mapItem
                }
            }
        }
    }

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

