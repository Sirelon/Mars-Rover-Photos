@file:SuppressLint("RestrictedApi")

package com.sirelon.marsroverphotos

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.window.layout.WindowMetrics
import androidx.window.layout.WindowMetricsCalculator
import androidx.window.layout.WindowMetricsCalculatorDecorator

/**
 * Keeps window-size reads working on devices that report API 34 but lack
 * `android.view.WindowMetrics.getDensity()` (an API 34 method).
 *
 * androidx.window calls `getDensity()` on every API 34+ device. Compose reads the window size
 * through androidx.window for `currentWindowAdaptiveInfo()`, so on such a device the first
 * composition crashes with `NoSuchMethodError` (seen on a "Pixel 8 Pro, Android 14" in 5.3.0).
 *
 * [install] probes for the method once. On a normal device it does nothing. On a device without
 * it, the decorator answers window metrics from the window bounds and the resources' density.
 */
object WindowMetricsFallback {

    fun install() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE || hasDensityMethod()) return
        WindowMetricsCalculator.overrideDecorator(
            object : WindowMetricsCalculatorDecorator {
                override fun decorate(calculator: WindowMetricsCalculator): WindowMetricsCalculator = Calculator
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun hasDensityMethod(): Boolean = runCatching {
        android.view.WindowMetrics::class.java.getMethod("getDensity")
    }.isSuccess

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private object Calculator : WindowMetricsCalculator {

        override fun computeCurrentWindowMetrics(activity: Activity) =
            metrics(activity) { it.currentWindowMetrics }

        override fun computeCurrentWindowMetrics(context: Context) =
            metrics(context) { it.currentWindowMetrics }

        override fun computeMaximumWindowMetrics(activity: Activity) =
            metrics(activity) { it.maximumWindowMetrics }

        override fun computeMaximumWindowMetrics(context: Context) =
            metrics(context) { it.maximumWindowMetrics }

        private fun metrics(
            context: Context,
            read: (WindowManager) -> android.view.WindowMetrics,
        ): WindowMetrics {
            val windowManager = context.getSystemService(WindowManager::class.java)
                ?: context.applicationContext.getSystemService(WindowManager::class.java)
            return WindowMetrics(read(windowManager).bounds, context.resources.displayMetrics.density)
        }
    }
}
