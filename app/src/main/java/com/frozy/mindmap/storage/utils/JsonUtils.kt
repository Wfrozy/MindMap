package com.frozy.mindmap.storage.utils

import org.json.JSONObject

//todo do smth with this legacy code
fun JSONObject.applyBasicContent(): JSONObject {
    this.apply {
        put("creationTimestamp", System.currentTimeMillis())
    }
    return this
}