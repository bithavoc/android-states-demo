package com.example.bithavoc.myapplication.login

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.os.*
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.TextView
import com.example.bithavoc.myapplication.MyBackendService
import com.example.bithavoc.myapplication.R
import com.example.bithavoc.myapplication.foundation.*
import com.example.bithavoc.myapplication.foundation.indicators.ProgressDialogLoaderIndicator
import com.example.bithavoc.myapplication.home.HomeActivity
import kotlinx.android.synthetic.main.activity_login.*;

class LoginActivity : AppCompatActivity() {
    private lateinit var routerFragment: ActionRouterFragment
    private val reacter = StateReacter() {
        object {
            @GlobalState
            var logon = LogonStateData()

            @ActionState
            var login = LoginResult()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        prepareUI()

        reacter.reacting { newLoginState, oldLoginState ->
            email_field.error = newLoginState.login.emailError
            password_field.error = newLoginState.login.passwordError
            if(newLoginState.logon.loggedIn) {
                this.startActivity(Intent(this, HomeActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK))
            }
        }

        var fragment = fragmentManager.findFragmentByTag("fragmentRouter") as? ActionRouterFragment
        if(fragment == null) {
            fragment = ActionRouterFragment()
            val transaction = this.fragmentManager.beginTransaction()
            transaction.add(fragment, "fragmentRouter")
            transaction.commit()
        }
        routerFragment = fragment
        routerFragment.initializingState(object:StateTransitionIndicator {
            override fun start() {
                showProgress(true)
            }

            override fun end() {
                showProgress(false)
            }
        })
        routerFragment.prepare(MyBackendService::class.java, reacter)
    }

    private fun prepareUI() {
        this.password_field.setOnEditorActionListener(TextView.OnEditorActionListener { textView, id, keyEvent ->
            if (id == R.id.password_field || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@OnEditorActionListener true
            }
            false
        })
        val mEmailSignInButton = findViewById(R.id.email_sign_in_button) as Button?
        mEmailSignInButton?.setOnClickListener { attemptLogin() }
    }

    private fun attemptLogin() {
        reacter.fire(ActionPath(handler = "logon", action = "login")) {
            input { LoginCredentials(email = email_field.text.toString(), password = password_field.text.toString()) }
            state { state -> state.login }
        }
    }

    private fun showProgress(show: Boolean) {
            val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime)

            login_form.visibility = if (show) View.GONE else View.VISIBLE
            login_form.animate().setDuration(shortAnimTime.toLong()).alpha(
                    (if (show) 0 else 1).toFloat()).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    login_form.visibility = if (show) View.GONE else View.VISIBLE
                }
            })

            login_progress.visibility = if (show) View.VISIBLE else View.GONE
            login_progress.animate().setDuration(shortAnimTime.toLong()).alpha(
                    (if (show) 1 else 0).toFloat()).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    login_progress.visibility = if (show) View.VISIBLE else View.GONE
                }
            })
    }
}

