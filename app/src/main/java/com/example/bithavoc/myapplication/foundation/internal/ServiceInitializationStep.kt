package com.example.bithavoc.myapplication.foundation.internal

import android.os.Messenger
import com.example.bithavoc.myapplication.foundation.InitializationStep

/**
 * Created by bithavoc on 5/4/16.
 */
internal class ServiceInitializationStep : InitializationStep {
    override fun satisfiedBy(result: Any?): Boolean {
        return result is Messenger
    }
}