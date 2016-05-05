package com.example.bithavoc.myapplication.foundation

import android.content.Intent

interface Broadcaster {
    fun publish(intent: Intent)
    companion object {
        val PUBLISH_ACTION_NAME = "Broadcaster.Publish"
        val PUBLISH_ACTION_PAYLOAD_KEY = "PublishPayload"
    }
}