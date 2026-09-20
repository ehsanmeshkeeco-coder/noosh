package com.example.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.R

class QuickAddWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_quick_add)

            fun createAddPendingIntent(amountMl: Int, requestCode: Int): PendingIntent {
                val intent = Intent(context, WidgetActionReceiver::class.java).apply {
                    action = WidgetActionReceiver.ACTION_WIDGET_ADD_WATER
                    putExtra(WidgetActionReceiver.EXTRA_AMOUNT_ML, amountMl)
                }
                return PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

            views.setOnClickPendingIntent(R.id.widget_btn_150, createAddPendingIntent(150, 101))
            views.setOnClickPendingIntent(R.id.widget_btn_250, createAddPendingIntent(250, 102))
            views.setOnClickPendingIntent(R.id.widget_btn_500, createAddPendingIntent(500, 103))

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
