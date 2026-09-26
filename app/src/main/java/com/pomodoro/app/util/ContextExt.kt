package com.pomodoro.app.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** Unwraps Compose's themed/decor Context wrappers to find the hosting Activity. */
fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
