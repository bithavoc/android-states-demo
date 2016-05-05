package com.example.bithavoc.myapplication.foundation

class HandlerRegistry {
    private var handlers = mutableMapOf<String, ActionHandler>()
    public fun register(name:String, handler :ActionHandler) {
        handlers[name] = handler
    }

    public fun getHandler(name:String) : ActionHandler? {
        return handlers[name]
    }
}