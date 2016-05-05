package com.example.bithavoc.myapplication.foundation

/**
 * Created by bithavoc on 5/4/16.
 */
class ActionTrigger<T>(val state: T) {
    var actionInput: Any? = null
    fun input(inputFetch:() -> Any?) {
        this.actionInput = inputFetch()
    }

    var actionState:Any? = null

    fun state(stateFetch:(state:T) -> Any?) {
        this.actionState = stateFetch(state)
    }
}