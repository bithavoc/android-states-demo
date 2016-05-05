package com.example.bithavoc.myapplication.foundation

import kotlin.reflect.KParameter

internal class GlobalStateArgumentPlaceholder(val id: GlobalStateIdentifier, val parameter:KParameter) {
    val optional:Boolean
        get() = parameter.type.isMarkedNullable || parameter.isOptional
}