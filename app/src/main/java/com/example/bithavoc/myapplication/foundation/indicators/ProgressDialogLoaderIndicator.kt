package com.example.bithavoc.myapplication.foundation.indicators

import android.app.ProgressDialog
import android.content.Context
import com.example.bithavoc.myapplication.foundation.StateTransitionIndicator

/**
 * Created by bithavoc on 5/4/16.
 */
class ProgressDialogLoaderIndicator(val context:Context) : StateTransitionIndicator {
    var dialog: ProgressDialog? = null
    override fun start() {
        dialog = ProgressDialog.show(context, "Progress", "haciendo algo")
    }

    override fun end() {
        dialog?.dismiss()
        dialog = null
    }
}