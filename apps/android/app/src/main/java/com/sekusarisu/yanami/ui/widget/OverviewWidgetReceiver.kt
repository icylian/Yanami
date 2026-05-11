package com.sekusarisu.yanami.ui.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class OverviewWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = OverviewWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetUpdateWorker.enqueue(context, immediate = true)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val manager = AppWidgetManager.getInstance(context)
        val allIds = manager.getAppWidgetIds(
            ComponentName(context, OverviewWidgetReceiver::class.java)
        )
        val remaining = allIds.filter { it !in appWidgetIds }
        if (remaining.isEmpty()) {
            WidgetUpdateWorker.cancel(context)
        }
    }
}
