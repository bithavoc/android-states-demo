package com.example.bithavoc.myapplication.foundation
import android.os.Handler
import android.os.Looper
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

    //
    // Current state
    //
    var state :T = stateInit()
        get() = field
        set(value) {
            val old = field
            val new = value
            val diff = differ.compare(new, old)
            field = new
            triggerChanged(new, old)
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
    }
    private var changedDelegate : ((new:T, old:T) -> Unit)? = null
    private var changingLoopHandler : Handler? = null

    fun reacting(init: (new:T, old:T) -> Unit) {
        changingLoopHandler = Handler(Looper.myLooper())
        changedDelegate = init
        triggerInitialChange()
        ensureWatchingGlobalState()
    }

    fun isWatchingGlobalState(id:String) : Boolean {
        return globalStateProperties.keys.any { it.toString().equals(id, ignoreCase = true) }
    }

    fun updateLocalGlobalState(id:GlobalStateIdentifier, state:Any?) {
        val prop = globalStateProperties[id] ?: return
        val currentState = this.state

        if(state != null || prop.returnType.isMarkedNullable) {
            prop.set(receiver = currentState, value = state)
        }
        this.state = currentState
    }

    fun updateActionState(oldActionState:Any?, newActionState:Any?) {
        if(oldActionState == null) {
            return
        }
        val currentState = this.state

        for(prop in actionStateProperties) {
            var existingValue = prop.get(currentState)
            if(existingValue == null) {
                return
            }
            if(existingValue.equals(oldActionState)) {
                prop.set(receiver = currentState, value = newActionState)
            }
        }
        this.state = currentState
    }
}