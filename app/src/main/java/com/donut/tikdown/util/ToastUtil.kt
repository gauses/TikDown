package com.donut.tikdown.util

import android.content.Context
import android.widget.Toast
import com.donut.tikdown.app
import com.donut.tikdown.appScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


var toast: Toast? = null

fun showToast(msg: String, context: Context = app) {
    appScope.launch(Dispatchers.Main) {
        toast?.cancel()
        toast = Toast.makeText(context, msg, Toast.LENGTH_LONG).apply {
            show()
        }
    }
}
