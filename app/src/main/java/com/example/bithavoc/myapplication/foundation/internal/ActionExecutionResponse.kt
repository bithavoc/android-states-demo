package com.example.bithavoc.myapplication.foundation.internal

import com.example.bithavoc.myapplication.foundation.ActionPath

/**
 * Created by bithavoc on 5/4/16.
 */

data class ActionExecutionResponse(var requestId:Long = 0, var state:Any? = null) : MessageEntity