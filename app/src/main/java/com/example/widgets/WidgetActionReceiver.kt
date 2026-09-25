package com.example.widgets

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.NooshApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_WIDGET_ADD_WATER) {
            val amountMl = intent.getIntExtra(EXTRA_AMOUNT_ML, 250)
            val pendingResult = goAsync()

            val app = context.applicationContext as? NooshApplication
            if (app != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val result = app.addWaterIntakeUseCase(
                            amountMl = amountMl,
                            source = "widget"
                        )
                        WaterProgressWidgetProvider.updateAllWidgets(context)

                        val msg = if (result.xpEarned > 0) {
                            "${com.example.core.util.DateTimeUtils.toPersianDigits(amountMl)} میلی‌لیتر آب ثبت شد (+${com.example.core.util.DateTimeUtils.toPersianDigits(result.xpEarned)} امتیاز ✨) 💧"
                        } else {
                            "${com.example.core.util.DateTimeUtils.toPersianDigits(amountMl)} میلی‌لیتر آب ثبت شد 💧"
                        }

                        launch(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                msg,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            } else {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_WIDGET_ADD_WATER = "com.example.action.WIDGET_ADD_WATER"
        const val EXTRA_AMOUNT_ML = "extra_amount_ml"
    }
}
