package com.example.bithavoc.myapplication.foundation

import android.content.Intent
import android.support.v4.content.LocalBroadcastManager

class LocalBroadcaster : Broadcaster {
    var localBroadcastManager: LocalBroadcastManager? = null

    override fun publish(intent: Intent) {
        localBroadcastManager?.sendBroadcast(intent)
    }
}