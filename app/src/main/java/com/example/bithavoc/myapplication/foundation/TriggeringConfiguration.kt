package com.example.bithavoc.myapplication.foundation

/**
 * Created by bithavoc on 5/7/16.
 */
class TriggeringConfiguration<TState>(val state:TState) {
    private val transitions = mutableListOf<StateTransitionIndicator>()
    fun transition(transition: StateTransitionIndicator) {
        this.transitions.add(transition)
    }

    fun then(callback:(state:TState) -> Unit) {
        callback(state)
    }
}