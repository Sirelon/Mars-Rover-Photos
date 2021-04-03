package com.sirelon.marsroverphotos.utils

import android.app.Activity
import android.graphics.Insets
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowInsets


/**
 * Created on 03.04.2021 21:44 for Mars-Rover-Photos.
 */
val Activity.screenWidth: Int
    get() {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val insets: Insets = windowMetrics.windowInsets
                .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
            windowMetrics.bounds.width() - insets.left - insets.right
        } else {
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            displayMetrics.widthPixels
        }
    }