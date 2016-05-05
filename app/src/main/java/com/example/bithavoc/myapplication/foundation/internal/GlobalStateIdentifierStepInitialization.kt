package com.example.bithavoc.myapplication.foundation.internal

import com.example.bithavoc.myapplication.foundation.GlobalStateIdentifier
import com.example.bithavoc.myapplication.foundation.InitializationStep

/**
 * Created by bithavoc on 5/4/16.
 */
class GlobalStateIdentifierStepInitialization(val id: GlobalStateIdentifier) : InitializationStep {
    override fun satisfiedBy(result: Any?): Boolean {
        if(result !is GlobalStateIdentifier) {
            return false
        }
        return id.toUniqueId().equals(result.toUniqueId())
    }
}