package com.example.bithavoc.myapplication.foundation

/**
 * Created by bithavoc on 5/4/16.
 */
interface InitializationStep {
    fun satisfiedBy(result: Any?) : Boolean
}