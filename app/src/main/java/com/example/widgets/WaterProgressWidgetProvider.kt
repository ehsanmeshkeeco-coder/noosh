package com.example.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.NooshApplication
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WaterProgressWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, WaterProgressWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                updateWidgets(context, appWidgetManager, ids)
            }
        }

        private fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            val app = context.applicationContext as? NooshApplication
            CoroutineScope(Dispatchers.IO).launch {
                val totalMl = app?.waterRepository?.getTodayTotalMl() ?: 500
                val profile = app?.userRepository?.getUserProfile()
                val goalMl = profile?.dailyWaterGoalMl ?: 2000
                val pct = if (goalMl > 0) ((totalMl.toFloat() / goalMl) * 100).toInt().coerceAtMost(100) else 0
                val glasses = totalMl / 250
                val goalGlasses = (goalMl / 250).coerceAtLeast(1)

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_water_progress)
                    views.setTextViewText(R.id.widget_progress_percent, "$pct٪")
                    views.setTextViewText(
                        R.id.widget_intake_text,
                        "$totalMl از $goalMl میلی‌لیتر ($glasses از $goalGlasses لیوان)"
                    )
                    views.setProgressBar(R.id.widget_progress_bar, 100, pct, false)

                    // Open app on root click
                    val clickIntent = Intent(context, MainActivity::class.java)
                    val clickPendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        clickIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_progress_root, clickPendingIntent)

                    // Quick add +1 glass
                    val addIntent = Intent(context, WidgetActionReceiver::class.java).apply {
                        action = WidgetActionReceiver.ACTION_WIDGET_ADD_WATER
                        putExtra(WidgetActionReceiver.EXTRA_AMOUNT_ML, 250)
                    }
                    val addPendingIntent = PendingIntent.getBroadcast(
                        context,
                        10,
                        addIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_btn_quick_add, addPendingIntent)

                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            }
        }
    }
}
