package com.example.bithavoc.myapplication.home

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.example.bithavoc.myapplication.MyBackendService
import com.example.bithavoc.myapplication.R
import com.example.bithavoc.myapplication.foundation.*
import com.example.bithavoc.myapplication.foundation.indicators.ProgressDialogLoaderIndicator
import com.example.bithavoc.myapplication.login.LoginActivity
import com.example.bithavoc.myapplication.login.LoginCredentials
import com.example.bithavoc.myapplication.login.LoginResult
import com.example.bithavoc.myapplication.login.LogonStateData
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_login.*

/**
 * Created by bithavoc on 5/4/16.
 */
class HomeActivity : Activity() {
    private lateinit var routerFragment: ActionRouterFragment
    private val reacter = StateReacter() {
        object {
            @GlobalState
            var logon:LogonStateData = LogonStateData(loggedIn = true)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        logout_button.setOnClickListener {
            reacter.fire(ActionPath(handler = "logon", action = "logout"))
        }

        reacter.reacting { newState, oldState ->
            /*if(!newState.logon.loggedIn) {
                startActivity(Intent(this, LoginActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK))
                finish()
            }*/
        }

        var fragment = fragmentManager.findFragmentByTag("fragmentRouter") as? ActionRouterFragment
        if(fragment == null) {
            fragment = ActionRouterFragment()
            val transaction = this.fragmentManager.beginTransaction()
            transaction.add(fragment, "fragmentRouter")
            transaction.commit()
        }
        routerFragment = fragment
        routerFragment.initializingState(ProgressDialogLoaderIndicator(this))
        routerFragment.prepare(MyBackendService::class.java, reacter)
    }
}