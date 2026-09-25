package com.example.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.NooshApplication
import com.example.R
import com.example.core.util.DateTimeUtils
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

                val chartBitmap = createCircularChartBitmap(pct)

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_water_progress)
                    views.setImageViewBitmap(R.id.widget_circular_chart, chartBitmap)
                    views.setTextViewText(
                        R.id.widget_intake_text,
                        "${DateTimeUtils.toPersianDigits(totalMl)} از ${DateTimeUtils.toPersianDigits(goalMl)} میلی‌لیتر"
                    )
                    views.setTextViewText(
                        R.id.widget_glasses_text,
                        "${DateTimeUtils.toPersianDigits(glasses)} از ${DateTimeUtils.toPersianDigits(goalGlasses)} لیوان استاندارد"
                    )

                    // Open app on root click
                    val clickIntent = Intent(context, MainActivity::class.java)
                    val clickPendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        clickIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_progress_root, clickPendingIntent)

                    // Quick add default 1 glass (250ml)
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

        fun createCircularChartBitmap(percentage: Int): Bitmap {
            val size = 200
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val strokeWidth = 20f
            val padding = strokeWidth / 2f + 6f
            val oval = RectF(padding, padding, size - padding, size - padding)

            // Background track circle
            val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                this.strokeWidth = strokeWidth
                color = Color.parseColor("#E0F2FE")
            }
            canvas.drawArc(oval, 0f, 360f, false, trackPaint)

            // Progress arc
            val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                this.strokeWidth = strokeWidth
                strokeCap = Paint.Cap.ROUND
                color = if (percentage >= 100) Color.parseColor("#10B981") else Color.parseColor("#0284C7")
            }
            val sweepAngle = (percentage.coerceIn(0, 100) / 100f) * 360f
            if (sweepAngle > 0f) {
                canvas.drawArc(oval, -90f, sweepAngle, false, progressPaint)
            }

            // Percentage Text
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                textSize = 42f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = if (percentage >= 100) Color.parseColor("#047857") else Color.parseColor("#0369A1")
            }
            val percentText = "${DateTimeUtils.toPersianDigits(percentage)}٪"
            canvas.drawText(percentText, size / 2f, size / 2f + 14f, textPaint)

            // Subtitle text (آب امروز)
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                textSize = 18f
                color = Color.parseColor("#64748B")
            }
            canvas.drawText("آب امروز", size / 2f, size / 2f + 42f, subPaint)

            return bitmap
        }
    }
}
