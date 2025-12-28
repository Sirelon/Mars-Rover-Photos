package com.sirelon.marsroverphotos.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetReceiver

public class MarsPhotoWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget = MarsPhotoWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        MarsPhotoWidgetWorker.enqueuePeriodic(context)
        MarsPhotoWidgetWorker.enqueueOnce(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        MarsPhotoWidgetWorker.enqueuePeriodic(context)
        MarsPhotoWidgetWorker.enqueueOnce(context)
    }
}
