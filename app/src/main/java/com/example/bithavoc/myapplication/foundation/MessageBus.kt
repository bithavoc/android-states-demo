package com.example.bithavoc.myapplication.foundation

import android.content.Intent
import android.os.Message

/**
 * Created by bithavoc on 5/4/16.
 */
interface MessageBus {
    fun sendMessage(msg: Message)
    fun broadcastReceived(callback:(intent: Intent) -> Unit)
    fun replyReceived(callback:(msg: Message) -> Unit)
}