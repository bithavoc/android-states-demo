package com.example.bithavoc.myapplication.foundation

import android.app.Fragment
import android.content.*
import android.os.*
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.example.bithavoc.myapplication.foundation.internal.*

class ActionRouterFragment<T, TServiceClass>(val serviceClass: Class<TServiceClass>, stateInit: () -> T) : Fragment()  where T:Any, TServiceClass:BackendService {
    var reacter = StateReacter<T>(stateInit=stateInit)
    private var serviceClient : Messenger? = null
    private lateinit var localBroadcastManager:LocalBroadcastManager
    private var pendingInitializations = mutableListOf<InitializationStep>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pendingInitializations.add(ServiceInitializationStep())
        localBroadcastManager = LocalBroadcastManager.getInstance(context)
        localBroadcastManager.registerReceiver(broadcastReceiver, IntentFilter(Broadcaster.PUBLISH_ACTION_NAME))
        this.activity.bindService(Intent(this.activity, serviceClass), connection, Context.BIND_AUTO_CREATE)
        prepareCallback?.invoke(this)

        if(reacter.globalStateProperties.count() > 0) {
            reacter.globalStateProperties.keys.forEach { globalProp ->
                pendingInitializations.add(GlobalStateIdentifierStepInitialization(id = globalProp))
            }
        }
        playStateTransitionPhase(starting = true)
    }

    override fun onDestroy() {
        super.onDestroy()
        if(connection.bound) {
            this.activity.unbindService(connection)
        }
        localBroadcastManager.unregisterReceiver(broadcastReceiver)
    }

    private var stateInitializerTransitions = mutableListOf<StateTransitionIndicator>()

    fun initializingState(transition: StateTransitionIndicator) {
        stateInitializerTransitions.add(transition)
    }

    private fun playStateTransitionPhase(starting:Boolean) {
        stateInitializerTransitions.forEach {
            if(starting) {
                it.start()
            } else {
                it.end()
            }
        }
    }
    private var prepareCallback: ((ActionRouterFragment<T, TServiceClass>) -> Unit)? = null

    fun prepare(callback: (ActionRouterFragment<T, TServiceClass>) -> Unit) {
        this.prepareCallback = callback
    }

    private fun sendMessage(msg:Message) {
        msg.replyTo = incomingMessenger
        serviceClient?.send(msg)
    }

    private val connection = object : ServiceConnection {
        var bound = false
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d("ActionRouterFragment", "Service Connected")
            bound = true
            serviceClient = Messenger(service)
            satisfyPendingInitialization(serviceClient)
            requestInitialGlobalStates()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d("ActionRouterFragment", "Service Disconnected")
            bound = false
            serviceClient = null
        }
    }

    val incomingMessenger = Messenger(object : Handler() {
        override fun handleMessage(msg: Message?) {
            if(msg == null) {
                return
            }
            Log.d("ActionRouterFragment", "Incoming Reply Message")
            processReply(msg)
            super.handleMessage(msg)
        }
    })

    val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("ActionRouterFragment", "onReceive local notification")
            if(intent == null) {
                return
            }
            val globalStateId = intent.getStringExtra(GlobalStateChangeNotification.GLOBAL_STATE_IDENTIFIER)
            if(reacter.isWatchingGlobalState(globalStateId)) {
                val payload = intent.getStringExtra(Broadcaster.PUBLISH_ACTION_PAYLOAD_KEY)
                val notification = CommandMapping.deserialize(payload, GlobalStateChangeNotification::class.java)
                val id = notification.id!!
                reacter.updateLocalGlobalState(id, notification.state)
                satisfyPendingInitialization(id)
            }
        }
    }

    private fun requestInitialGlobalStates() {
        reacter.globalStateProperties.keys.forEach { stateId ->
            val request = GlobalStateRepublishRequest(id = stateId)
            sendMessage(request.toMessage())
        }
    }

    private fun satisfyPendingInitialization(result:Any?) {
        Log.d("ActionRouterFragment", "Satisfy pending with ${result}")
        val step = pendingInitializations.find { it.satisfiedBy(result) } ?: return
        pendingInitializations.remove(step)
        if(pendingInitializations.count() == 0) {
            playStateTransitionPhase(starting = false)
        }
    }
    private var globalRequestId = 0L;
    private var pendingRequests = mutableMapOf<Long, ActionExecutionRequest>()

    private fun processReply(msg:Message) {
        val response = CommandMapping.fromMessage(msg)
        if (response is ActionExecutionResponse) {
            processResponse(response)
        } else {
            Log.d("ActionRouterFragment", "Got unknown reply response $response")
        }
    }

    private fun processResponse(response: ActionExecutionResponse) {
        val originalRequest = pendingRequests.remove(response.requestId) ?: return
        reacter.updateActionState(oldActionState = originalRequest.state, newActionState = response.state)
    }

    fun fire(actionPath:ActionPath, prepare: (ActionTrigger<T>.() -> Unit)? = null) {
        val config = ActionTrigger(state = reacter.state)
        prepare?.invoke(config)
        var input = config.actionInput
        var state = config.actionState
        val requestId = globalRequestId++
        val request = ActionExecutionRequest(actionPath=actionPath, input = input, state = state, requestId = requestId)
        sendMessage(request.toMessage())
        pendingRequests.set(request.requestId, request)
    }
}