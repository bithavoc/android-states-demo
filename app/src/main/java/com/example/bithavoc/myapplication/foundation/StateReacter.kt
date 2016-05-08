package com.example.bithavoc.myapplication.foundation
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.example.bithavoc.myapplication.foundation.internal.*
import de.danielbechler.diff.ObjectDiffer
import de.danielbechler.diff.ObjectDifferBuilder
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.memberProperties

class StateReacter<T>(val stateInit: () -> T) where T:Any {
    internal val globalStateProperties = mutableMapOf<GlobalStateIdentifier, KMutableProperty1<Any, Any?>>()
    internal val actionStateProperties = mutableSetOf<KMutableProperty1<Any, Any?>>()
    val differ: ObjectDiffer
    init {
        val builder = ObjectDifferBuilder.startBuilding()
        builder.introspection().setDefaultIntrospector(DataClassIntrospector())
        differ = builder.build()
    }

    var messageBus: MessageBus? = null
        set(value) {
            val bus = value ?: return
            bus.broadcastReceived { intent ->
                Log.d("StateReacter", "broadcastReceived")
                val globalStateId = intent.getStringExtra(GlobalStateChangeNotification.GLOBAL_STATE_IDENTIFIER)
                if(isWatchingGlobalState(globalStateId)) {
                    val payload = intent.getStringExtra(Broadcaster.PUBLISH_ACTION_PAYLOAD_KEY)
                    val notification = CommandMapping.deserialize(payload, GlobalStateChangeNotification::class.java)
                    val id = notification.id!!
                    updateLocalGlobalState(id, notification.state)
                    initializationChain?.satisfyPendingInitialization(id)
                }
            }
            bus.replyReceived { msg ->
                processReply(msg)
            }
            field = value
        }

    var initializationChain:InitializationChain? = null
        set(chain) {
            addGlobalStateInitializationSteps()
            field = chain
        }

    private fun addGlobalStateInitializationSteps() {
        val chain = this.initializationChain ?: return
        globalStateProperties.keys.forEach { globalProp ->
            chain.requireInitiazation(GlobalStateIdentifierStepInitialization(id = globalProp))
        }
    }
    //
    // Current state
    //
    var state :T = stateInit()
        get() = field
        set(value) {
            val old = field
            val new = value
            val diff = differ.compare(new, old)
            if(diff.isChanged) {
                field = new
                triggerChanged(new, old)
            }
        }

    fun restoreState(restoredState:Any?) {
        this.state = restoredState as T
    }

    private var watchingGlobalState:Boolean = false
    fun ensureWatchingGlobalState() {
        if(watchingGlobalState) {
            return
        }
        watchingGlobalState = true
        if(state != null) {
            for(property in state.javaClass.kotlin.memberProperties) {
                property.annotations.map {it as? GlobalState}.filterNotNull().forEach { globalStateAnnotation ->
                    val globalStateId = globalStateAnnotation.toIndentifier(name=property.returnType.toString())
                    val prop = (property as? KMutableProperty1<Any, Any?>) ?: throw Exception("Watched Global state property ${property.name} is not writable")
                    globalStateProperties[globalStateId] = prop
                }
            }
            var actionStateProps = state.javaClass.kotlin.memberProperties.map{ prop -> prop as? KMutableProperty1<Any, Any?>}.filterNotNull().filter { prop -> prop.annotations.filter{ it is ActionState }.isNotEmpty() }
            actionStateProperties.addAll(actionStateProps)

            for(prop in actionStateProperties) {
                var existingValue = prop.get(state)

                if(existingValue == null) {
                    throw Exception("${ActionState::class.simpleName} on property ${prop.name} is required to have an initial value")
                }
            }
        }
    }

    private fun triggerInitialChange() {
        triggerChanged(state, state)
    }
    private fun triggerChanged(new:T, old:T) {
        changingLoopHandler?.post { changedDelegate?.invoke(new, old) }
        decisions.check(new)
    }
    private var changedDelegate : ((new:T, old:T) -> Unit)? = null
    private var changingLoopHandler : Handler? = null

    fun reacting(init: (new:T, old:T) -> Unit) {
        changingLoopHandler = Handler(Looper.myLooper())
        changedDelegate = init
        triggerInitialChange()
        ensureWatchingGlobalState()
    }

    private fun isWatchingGlobalState(id:String) : Boolean {
        return globalStateProperties.keys.any { it.toString().equals(id, ignoreCase = true) }
    }

    private fun duplicateState() : T {
        val stateData = CommandMapping.json.serialize(this.state)
        return CommandMapping.json.deserialize(stateData, this.state.javaClass)
    }

    private fun updateLocalGlobalState(id:GlobalStateIdentifier, state:Any?) {
        val prop = globalStateProperties[id] ?: return
        val newState = duplicateState()

        if(state != null || prop.returnType.isMarkedNullable) {
            prop.set(receiver = newState, value = state)
        }
        this.state = newState
    }

    private fun updateActionState(oldActionState:Any?, newActionState:Any?) {
        if(oldActionState == null) {
            return
        }
        val newState = this.duplicateState()

        for(prop in actionStateProperties) {
            var existingValue = prop.get(newState)
            if(existingValue == null) {
                return
            }
            if(existingValue.equals(oldActionState)) {
                prop.set(receiver = newState, value = newActionState)
            }
        }
        this.state = newState
    }

    fun fire(actionPath:ActionPath, prepare: (ActionTrigger<T>.() -> Unit)? = null) {
        val config = ActionTrigger(state = this.state)
        prepare?.invoke(config)
        var input = config.actionInput
        var state = config.actionState
        val requestId = globalRequestId++
        val request = ActionExecutionRequest(actionPath = actionPath, input = input, state = state, requestId = requestId)
        messageBus?.sendMessage(request.toMessage())
        pendingRequests.set(request.requestId, request)
    }

    private val decisions = ReacterDecisionTree<T>(null, "")
    init {
        decisions.on { true }
    }
    fun decide(name:String, build: ReacterDecisionTree<T>.() -> Unit) {
        decisions.so(name, build)
    }

    fun triggering(action:ActionPath, build:TriggeringConfiguration<T>.() -> Unit) {
        val tree = TriggeringConfiguration<T>(state)
        tree.build()
    }

    private fun processResponse(response: ActionExecutionResponse) {
        val originalRequest = pendingRequests.remove(response.requestId) ?: return
        updateActionState(oldActionState = originalRequest.state, newActionState = response.state)
    }

    private fun processReply(msg: Message) {
        val response = CommandMapping.fromMessage(msg)
        if (response is ActionExecutionResponse) {
            processResponse(response)
        } else {
            Log.d("ActionRouterFragment", "Got unknown reply response $response")
        }
    }

    private var globalRequestId = 0L;
    private var pendingRequests = mutableMapOf<Long, ActionExecutionRequest>()
}