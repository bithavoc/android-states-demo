package com.example.bithavoc.myapplication.foundation.internal

import com.example.bithavoc.myapplication.foundation.ActionPath

data class ActionExecutionRequest(var requestId:Long = 0, var actionPath: ActionPath? = null, var input: Any? = null, var state: Any? = null) : MessageEntity