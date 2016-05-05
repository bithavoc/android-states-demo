package com.example.bithavoc.myapplication.foundation

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.example.bithavoc.myapplication.foundation.internal.CommandMapping
import com.example.bithavoc.myapplication.foundation.internal.GlobalStateChangeNotification
import com.example.bithavoc.myapplication.foundation.internal.toJSON

class GlobalStateBag(val broadcaster:Broadcaster, val context:Context) {
    private var innerState = mutableMapOf<String, Any?>()
    private val preferences = context.getSharedPreferences(GlobalStateBag.globalStatePreferenceName, Context.MODE_PRIVATE)
    init {
        initStateFromPersistence()
    }

    private fun initStateFromPersistence() {
        val state = preferences.getString("state", null)
        if(state == null) {
            return
        }
        innerState = CommandMapping.deserialize(state, innerState.javaClass)
    }

    fun publish(id: GlobalStateIdentifier, state: Any?) {
        innerState[id.toUniqueId()] = state
        persist()
        republish(id)
    }

    private fun persist() {
        val editor = preferences.edit()
        val state = CommandMapping.json.serialize(innerState)
        editor.putString("state", state)
        editor.commit()
    }

    fun retrieve(id: GlobalStateIdentifier) : Any? {
        return innerState[id.toUniqueId()]
    }

    fun republish(id: GlobalStateIdentifier) {
        val intent = Intent(Broadcaster.PUBLISH_ACTION_NAME)
        val state = innerState[id.toUniqueId()]
        intent.putExtra(Broadcaster.PUBLISH_ACTION_PAYLOAD_KEY,  GlobalStateChangeNotification(id = id, state = state).toJSON())
        intent.putExtra(GlobalStateChangeNotification.GLOBAL_STATE_IDENTIFIER, id.toString())
        broadcaster.publish(intent)
    }

    companion object {
        val globalStatePreferenceName = "globalStateBackend"
    }
}