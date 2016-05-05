package com.example.bithavoc.myapplication.login

import com.example.bithavoc.myapplication.foundation.*

class LogonHandler : ActionHandler() {
    @ServiceAction
    public fun login(@Input loginCredentials:LoginCredentials, @ActionState result: LoginResult, @GlobalState state:LogonStateData = LogonStateData(loggedIn = false)) : Any? {
        var state = state.copy(attempts = state.attempts + 1)
        if(loginCredentials.email != "johan@firebase.co" && loginCredentials.password != "letmein") {
            return object {
                @ActionState
                val login = LoginResult(emailError = "Email or password incorrect ${state.attempts}")

                @GlobalState
                val logonState = state.copy(loggedIn = false)
            }
        }

        return object {
            @GlobalState
            val logonState = state.copy(loggedIn = true, attempts = 0)

            @ActionState
            val login = result.copy(emailError = null, passwordError = null)
        }
    }
}