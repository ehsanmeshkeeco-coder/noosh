package com.example.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.theme.NooshPrimary

data class TourStep(
    val title: String,
    val description: String,
    val targetArea: TourTargetArea,
    val targetRect: Rect? = null
)

enum class TourTargetArea {
    WATER_RING,
    QUICK_ADD,
    BOTTOM_NAV,
    COMPANION_TAB
}

/**
 * Interactive spotlight tour overlay that dims and blurs the entire screen,
 * keeping only the focused explaining area completely clear and bright.
 */
@Composable
fun InteractiveTourOverlay(
    activeStepIndex: Int,
    onNextStep: () -> Unit,
    onSkipTour: () -> Unit,
    ringBounds: Rect? = null,
    quickAddBounds: Rect? = null,
    bottomNavBounds: Rect? = null
) {
    val steps = remember(ringBounds, quickAddBounds, bottomNavBounds) {
        listOf(
            TourStep(
                title = "حلقه هوشمند پیشرفت آب",
                description = "در این قسمت درصد مصرف آب روزانه، تعداد لیوان‌های خورده شده و یادآور بعدی را می‌بینید.",
                targetArea = TourTargetArea.WATER_RING,
                targetRect = ringBounds
            ),
            TourStep(
                title = "ثبت سریع و هوشمند آب",
                description = "با لمس دکمه «+۱ لیوان» یا «ثبت مقدار»، آب مصرفی‌تان بلافاصله ذخیره می‌شود.",
                targetArea = TourTargetArea.QUICK_ADD,
                targetRect = quickAddBounds
            ),
            TourStep(
                title = "همراه سلامت و نوار ناوبری",
                description = "از این بخش می‌توانید به نمودارهای هفتگی، اتاق همراه سلامت و تنظیمات شخصی دسترسی داشته باشید.",
                targetArea = TourTargetArea.BOTTOM_NAV,
                targetRect = bottomNavBounds
            )
        )
    }

    if (activeStepIndex >= steps.size) return

    val currentStep = steps[activeStepIndex]
    val isLast = activeStepIndex == steps.size - 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .testTag("interactive_tour_overlay")
    ) {
        // Spotlight Canvas: Darkened overlay with a transparent hole for the focused area
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val targetRect = currentStep.targetRect ?: when (currentStep.targetArea) {
                TourTargetArea.WATER_RING -> Rect(
                    left = canvasWidth * 0.15f,
                    top = canvasHeight * 0.18f,
                    right = canvasWidth * 0.85f,
                    bottom = canvasHeight * 0.52f
                )
                TourTargetArea.QUICK_ADD -> Rect(
                    left = canvasWidth * 0.08f,
                    top = canvasHeight * 0.55f,
                    right = canvasWidth * 0.92f,
                    bottom = canvasHeight * 0.68f
                )
                TourTargetArea.BOTTOM_NAV, TourTargetArea.COMPANION_TAB -> Rect(
                    left = 0f,
                    top = canvasHeight * 0.88f,
                    right = canvasWidth,
                    bottom = canvasHeight
                )
            }

            // Draw dimmed backdrop with cut-out hole
            val path = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = targetRect,
                        cornerRadius = CornerRadius(28.dp.toPx(), 28.dp.toPx())
                    )
                )
            }

            clipPath(path, clipOp = ClipOp.Difference) {
                // Dimmed translucent scrim
                drawRect(
                    color = Color(0xDD0B132B), // Deep slate with high opacity (87%)
                    size = Size(canvasWidth, canvasHeight)
                )
            }
        }

        // Tooltip Card positioned near or in center
        val isTargetBottom = currentStep.targetArea == TourTargetArea.BOTTOM_NAV
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            contentAlignment = if (isTargetBottom) Alignment.Center else Alignment.BottomCenter
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tour_tooltip_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentStep.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "مرحله ${activeStepIndex + 1} از ${steps.size}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        TextButton(onClick = onSkipTour) {
                            Text("رد شدن", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = currentStep.description,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onNextStep,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                text = if (isLast) "شروع استفاده از نوش 💧" else "متوجه شدم، بعدی",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
