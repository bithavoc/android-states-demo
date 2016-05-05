package com.example.bithavoc.myapplication

import com.example.bithavoc.myapplication.foundation.BackendService
import com.example.bithavoc.myapplication.login.LogonHandler

class MyBackendService : BackendService() {

    override fun configureHandlers() {
        super.configureHandlers()
        backend.handlers.register("logon", LogonHandler())
    }
}