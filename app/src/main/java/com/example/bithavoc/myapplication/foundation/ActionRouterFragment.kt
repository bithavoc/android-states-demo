package com.example.bithavoc.myapplication.foundation

import android.app.Activity
import android.app.Fragment
import android.content.*
import android.os.*
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.example.bithavoc.myapplication.foundation.internal.*

class ActionRouterFragment : Fragment(), MessageBus, InitializationChain {
    private var serviceClient : Messenger? = null
    private var localBroadcastManager:LocalBroadcastManager? = null
    private var pendingInitializations = mutableListOf<InitializationStep>(ServiceInitializationStep())

    private var reacter:StateReacter<*>? = null
        set(value) {
            field = value
            value?.messageBus = this
            value?.initializationChain = this
            wireDependencices()
        }
    private var existentState:Any? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(savedInstanceState != null) {
            val stateJSON = savedInstanceState.getString(ActionRouterFragment.STATE_BUNDLE_KEY)
            val stateClassName = savedInstanceState.getString(ActionRouterFragment.STATE_CLASS_NAME_BUNDLE_KEY)
            val stateClass = Class.forName(stateClassName)
            existentState = CommandMapping.json.deserialize(stateJSON, stateClass)
        }
        if(existentState == null && reacter == null) {
            throw Exception("${this.javaClass.name} requires preparation")
        }
        wireDependencices()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
    }

    private var wired=false
    private var created=false

    private fun wireDependencices() {
        if(wired) {
            return
        }
        if(serviceClass == null) {
            return
        }
        if(!isAdded) {
            return
        }
        val reacter = this.reacter ?: return
        wired = true
        if(this.existentState != null) {
            reacter.restoreState(this.existentState)
            this.existentState = null
        }

        localBroadcastManager = LocalBroadcastManager.getInstance(context)
        localBroadcastManager?.registerReceiver(broadcastReceiver, IntentFilter(Broadcaster.PUBLISH_ACTION_NAME))
        this.context.bindService(Intent(this.context, serviceClass), connection, Context.BIND_AUTO_CREATE)

        playStateTransitionPhase(starting = true)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        if(outState != null) {
            exportCurrentStateToBundle(outState)
        }
        super.onSaveInstanceState(outState)
    }

    private fun exportCurrentStateToBundle(outState:Bundle) {
        val currentState = reacter?.state ?: return
        val stateJSON = CommandMapping.json.serialize(currentState)
        outState.putString(ActionRouterFragment.STATE_BUNDLE_KEY, stateJSON)
        val stateClassName = currentState.javaClass.name
        outState.putString(ActionRouterFragment.STATE_CLASS_NAME_BUNDLE_KEY, stateClassName)
    }

    override fun onDestroy() {
        super.onDestroy()
        if(connection.bound) {
            this.activity.unbindService(connection)
        }
        localBroadcastManager?.unregisterReceiver(broadcastReceiver)
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
    private var serviceClass:Class<*>? = null
    fun <TServiceClass:BackendService>prepare(serviceClass: Class<TServiceClass>, reacter:StateReacter<*>)  {
        this.serviceClass = serviceClass
        this.reacter = reacter
    }

    override fun sendMessage(msg:Message) {
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
            replyReceivedCallback?.invoke(msg)
            super.handleMessage(msg)
        }
    })

    val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("ActionRouterFragment", "onReceive local notification")
            if(intent == null) {
                return
            }
            broadcastReceivedCallback?.invoke(intent)
        }
    }

    private fun requestInitialGlobalStates() {
        val reacter = this.reacter ?: return
        reacter.globalStateProperties.keys.forEach { stateId ->
            val request = GlobalStateRepublishRequest(id = stateId)
            sendMessage(request.toMessage())
        }
    }

    override fun satisfyPendingInitialization(result:Any?) {
        Log.d("ActionRouterFragment", "Satisfy pending with ${result}")
        val step = pendingInitializations.find { it.satisfiedBy(result) } ?: return
        pendingInitializations.remove(step)
        if(pendingInitializations.count() == 0) {
            playStateTransitionPhase(starting = false)
        }
    }

    override fun requireInitiazation(step: InitializationStep) {
        pendingInitializations.add(step)
    }

    private var broadcastReceivedCallback: ((Intent) -> Unit)? = null
    override fun broadcastReceived(callback: (Intent) -> Unit) {
        broadcastReceivedCallback = callback
    }

    private var replyReceivedCallback: ((Message) -> Unit)? = null
    override fun replyReceived(callback: (Message) -> Unit) {
        replyReceivedCallback = callback
    }

    companion object {
        private val STATE_BUNDLE_KEY="routerState"
        private val STATE_CLASS_NAME_BUNDLE_KEY="routerStateClassName"
    }
}