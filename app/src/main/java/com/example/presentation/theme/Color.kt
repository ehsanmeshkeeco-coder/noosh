package com.example.presentation.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Primary Noosh Brand Palette
val NooshPrimary = Color(0xFF2D9CFF)
val NooshPrimaryDark = Color(0xFF0284C7)
val NooshPrimaryLight = Color(0xFF56B7FF)
val NooshAccent = Color(0xFF8ED3FF)
val NooshSubtleBlue = Color(0xFFE0F2FE)
val NooshSoftBackground = Color(0xFFF0F9FF)
val NooshBackground = NooshSoftBackground
val NooshSurface = Color(0xFFFFFFFF)

val NooshWaveStart = Color(0xFF56B7FF)
val NooshWaveEnd = Color(0xFF2D9CFF)

// Text and neutral colors
val TextPrimary = Color(0xFF0F172A)
val TextSecondary = Color(0xFF475569)
val TextTertiary = Color(0xFF94A3B8)
val BorderSubtle = Color(0xFFE2E8F0)

// Feedback colors
val SuccessGreen = Color(0xFF10B981)
val WarningAmber = Color(0xFFF59E0B)
val FlameOrange = Color(0xFFFF6B4A)

// Gradients
val WaterGradient = Brush.verticalGradient(
    colors = listOf(NooshWaveStart, NooshWaveEnd)
)

val CardGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFFFFFFFF), Color(0xFFF0F9FF))
)

val DropletGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF56B7FF), Color(0xFF1E88E5))
)
