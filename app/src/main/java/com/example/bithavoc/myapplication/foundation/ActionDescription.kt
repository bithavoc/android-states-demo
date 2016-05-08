package com.example.bithavoc.myapplication.foundation

import kotlin.reflect.KCallable
import kotlin.reflect.KParameter

internal class ActionDescription(
        val methodImpl:KCallable<*>,
        val instanceParam: KParameter,
        val inputParam: KParameter?,
        val stateParam: KParameter?,
        val inGlobalStates:List<GlobalStateArgumentPlaceholder>) {
    fun canAcceptInputCheckingNullability(input:Any?) : Boolean {
        var acceptNullInputs = inputParam == null || inputParam.type.isMarkedNullable
        if(acceptNullInputs) {
            return true
        }
        return input != null
    }

    fun canAcceptStateCheckingNullability(state:Any?) : Boolean {
        var acceptNullInputs = stateParam == null || stateParam.type.isMarkedNullable
        if(acceptNullInputs) {
            return true
        }
        return state != null
    }

    fun shouldAddInput(input:Any?) : Boolean {
        return this.inputParam != null && canAcceptInputCheckingNullability(input)
    }

    fun shouldAddState(state:Any?) : Boolean {
        return this.stateParam != null && canAcceptStateCheckingNullability(state)
    }
}