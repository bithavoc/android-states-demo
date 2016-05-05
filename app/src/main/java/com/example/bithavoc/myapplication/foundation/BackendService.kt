package com.example.bithavoc.myapplication.foundation

import android.app.Service
import android.content.Intent
import android.os.*
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.example.bithavoc.myapplication.foundation.internal.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

open class BackendService : Service() {
    private val broadcaster = LocalBroadcaster()
    private lateinit var broadcastManager:LocalBroadcastManager
    val handlerThread = HandlerThread("Backend ${javaClass.name}")
    lateinit var backend:Backend

    override fun onCreate() {
        handlerThread.start()
        incomingMessenger = Messenger(object : Handler(handlerThread.looper) {
            override fun handleMessage(msg: Message?) {
                if(msg == null) {
                    return
                }
                Log.d("BackendService", "Incoming Message")
                processMessage(msg)
                super.handleMessage(msg)
            }
        })
        backend = Backend(broadcaster, context = applicationContext)
        configureHandlers()
        broadcaster.localBroadcastManager = LocalBroadcastManager.getInstance(applicationContext)
        super.onCreate()
    }

    open fun configureHandlers() {

    }

    private fun processMessage(msg: Message) {
        val request = CommandMapping.fromMessage(msg)
        if (request is ActionExecutionRequest) {
            val result = backend.execute(actionPath = request.actionPath!!, input = request.input, state = request.state)
            val response = ActionExecutionResponse(requestId = request.requestId, state = result)
            msg.replyTo.send(response.toMessage())
        } else if (request is GlobalStateRepublishRequest) {
            backend.republishGlobalState(request.id!!)
        } else {
            Log.d("BackendService", "Got unknown request $request")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    lateinit var incomingMessenger : Messenger

    override fun onBind(intent: Intent?): IBinder? {
        return incomingMessenger.binder;
    }
}