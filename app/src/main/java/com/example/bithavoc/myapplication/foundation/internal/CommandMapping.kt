package com.example.bithavoc.myapplication.foundation.internal

import android.os.Bundle
import android.os.Message
import android.util.Log
import com.owlike.genson.Genson
import com.owlike.genson.GensonBuilder

internal object CommandMapping {
    val PAYLOAD_KEY = "PAYLOAD"
    val json:Genson
    init {
        json = GensonBuilder().useRuntimeType(true).useClassMetadata(true).create()
    }
    val mapping = setOf<Class<*>>(
            ActionExecutionRequest::class.java,
            GlobalStateRepublishRequest::class.java,
            ActionExecutionResponse::class.java
    )

    fun commandFor(klass:Class<*>) : Int {
        return mapping.indexOf(klass)
    }

    fun fromMessage(msg:Message) : MessageEntity {
        val klass = mapping.elementAt(msg.what)
        val requestJSON = msg.data.getString(PAYLOAD_KEY)
        Log.d("CommandMapping", "Request JSON '$requestJSON'")
        val request = deserialize(requestJSON, klass)
        return request as MessageEntity
    }
    fun <T>deserialize(content:String, klass:Class<T>) : T {
        return json.deserialize(content, klass)
    }
}

fun MessageEntity.toJSON() : String {
    return CommandMapping.json.serialize(this)
}

fun MessageEntity.toMessage() : Message {
    var msg = Message.obtain(null, CommandMapping.commandFor(this.javaClass))
    var thisJSON = this.toJSON()
    var thisBundle = Bundle()
    thisBundle.putString(CommandMapping.PAYLOAD_KEY, thisJSON)
    msg.data = thisBundle
    return msg
}