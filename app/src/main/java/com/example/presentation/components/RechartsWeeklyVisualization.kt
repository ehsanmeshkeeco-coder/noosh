package com.example.presentation.components

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.domain.model.DayIntake
import org.json.JSONArray
import org.json.JSONObject

/**
 * Modern Data Visualization Screen Component using Recharts engine
 * to display daily water intake trends over the past week.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RechartsWeeklyVisualization(
    days: List<DayIntake>,
    goalMl: Int = 2000,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    val isPageLoaded = remember { mutableStateOf(false) }

    val jsonData = remember(days, goalMl) {
        val root = JSONObject()
        root.put("goalMl", goalMl)
        val arr = JSONArray()
        days.forEach { day ->
            val obj = JSONObject()
            val shortLetter = when {
                day.dayName.contains("یک") -> "ی"
                day.dayName.contains("دو") -> "د"
                day.dayName.contains("سه") -> "س"
                day.dayName.contains("چهار") -> "چ"
                day.dayName.contains("پنج") -> "پ"
                day.dayName.contains("جمعه") -> "ج"
                day.dayName.contains("شنبه") -> "ش"
                else -> day.dayName.take(1)
            }
            obj.put("dayName", shortLetter)
            obj.put("fullDate", day.dayName)
            obj.put("amountMl", day.amountMl)
            obj.put("goalMl", day.goalMl)
            obj.put("isToday", day.isToday)
            arr.put(obj)
        }
        root.put("days", arr)
        root.toString()
    }

    LaunchedEffect(jsonData, isPageLoaded.value) {
        if (isPageLoaded.value) {
            webViewRef.value?.let { webView ->
                val escapedJson = JSONObject.quote(jsonData)
                webView.evaluateJavascript(
                    "if (window.updateData) { window.updateData(JSON.parse($escapedJson)); }",
                    null
                )
            }
        }
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("recharts_weekly_visualization")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .clip(RoundedCornerShape(22.dp))
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        setBackgroundColor(AndroidColor.TRANSPARENT)
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                            displayZoomControls = false
                            builtInZoomControls = false
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isPageLoaded.value = true
                                val escaped = JSONObject.quote(jsonData)
                                evaluateJavascript(
                                    "if (window.updateData) { window.updateData(JSON.parse($escaped)); }",
                                    null
                                )
                            }
                        }
                        loadUrl("file:///android_asset/recharts_weekly.html")
                        webViewRef.value = this
                    }
                },
                update = { webView ->
                    webViewRef.value = webView
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef.value?.destroy()
            webViewRef.value = null
        }
    }
}
