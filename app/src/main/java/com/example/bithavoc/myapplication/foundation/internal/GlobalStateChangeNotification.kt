package com.example.bithavoc.myapplication.foundation.internal

import com.example.bithavoc.myapplication.foundation.GlobalStateIdentifier

data class GlobalStateChangeNotification(var id:GlobalStateIdentifier? = null, var state:Any? = null) :MessageEntity {
    companion object {
        val GLOBAL_STATE_IDENTIFIER = "GlobalStateChangeNotification.Identifier"
    }
}