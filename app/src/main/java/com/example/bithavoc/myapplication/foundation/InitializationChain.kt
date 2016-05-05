package com.example.bithavoc.myapplication.foundation

/**
 * Created by bithavoc on 5/5/16.
 */
interface InitializationChain {
    fun satisfyPendingInitialization(result:Any?)
    fun requireInitiazation(step:InitializationStep)
}