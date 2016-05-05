package com.example.bithavoc.myapplication.foundation

@Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY)
annotation class GlobalState(val instanceName:String = "singleton")


fun GlobalState.toIndentifier(name: String) : GlobalStateIdentifier {
    return GlobalStateIdentifier(name, instanceName=instanceName)
}