package com.example.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.core.util.DateTimeUtils
import com.example.domain.model.BadgeTier
import com.example.domain.model.StreakBadge
import com.example.domain.model.StreakBadgeManager
import com.example.domain.model.StreakInfo
import com.example.presentation.theme.FlameOrange
import com.example.presentation.theme.NooshPrimary
import com.example.presentation.theme.SuccessGreen

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HydrationStreakCard(
    streakInfo: StreakInfo,
    graceDayEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedBadge by remember { mutableStateOf<StreakBadge?>(null) }
    var showRulesDialog by remember { mutableStateOf(false) }

    val badges = remember(streakInfo) {
        StreakBadgeManager.getBadges(streakInfo.currentStreak, streakInfo.longestStreak)
    }

    val nextMilestone = remember(streakInfo) {
        StreakBadgeManager.getNextMilestone(streakInfo.currentStreak, streakInfo.longestStreak)
    }

    val unlockedCount = remember(badges) {
        badges.count { it.isUnlocked }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "flame_anim")
    val flameScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flameScale"
    )

    val isDarkTheme = MaterialTheme.colorScheme.surface.let { (it.red * 0.299f + it.green * 0.587f + it.blue * 0.114f) < 0.5f }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("hydration_streak_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: Title and Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(FlameOrange.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = FlameOrange,
                            modifier = Modifier
                                .size(22.dp)
                                .scale(flameScale)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "زنجیره هیدراتاسیون و مدال‌ها",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = StreakBadgeManager.getStreakTierTitle(streakInfo.currentStreak),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FlameOrange
                        )
                    }
                }

                IconButton(
                    onClick = { showRulesDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "قوانین زنجیره",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Streak Hero Banner
            val bannerGradient = if (isDarkTheme) {
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF3B1A0A),
                        Color(0xFF2E1308),
                        Color(0xFF1E1006)
                    )
                )
            } else {
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFFFFF7ED),
                        Color(0xFFFFEDD5),
                        Color(0xFFFEF3C7)
                    )
                )
            }
            val bannerBorder = if (isDarkTheme) Color(0xFF9A3412).copy(alpha = 0.5f) else Color(0xFFFED7AA)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(bannerGradient)
                    .border(1.dp, bannerBorder, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "زنجیره فعال جاری",
                                fontSize = 11.sp,
                                color = if (isDarkTheme) Color(0xFFFDBA74) else Color(0xFF9A3412),
                                fontWeight = FontWeight.Medium
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = DateTimeUtils.toPersianDigits(streakInfo.currentStreak.toString()),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDarkTheme) Color(0xFFFB923C) else Color(0xFFC2410C)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "روز مداوم",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) Color(0xFFFED7AA) else Color(0xFFEA580C),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                        }

                        // Stat Badges (Longest record & Grace Day)
                        Column(horizontalAlignment = Alignment.End) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant else Color.White.copy(alpha = 0.9f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "رکورد: ${DateTimeUtils.toPersianDigits(streakInfo.longestStreak.toString())} روز",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkTheme) Color(0xFFFDE68A) else Color(0xFF78350F)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Box(
                                modifier = Modifier
                                    .background(
                                        if (graceDayEnabled) {
                                            if (isDarkTheme) Color(0xFF064E3B) else Color(0xFFDCFCE7)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = if (graceDayEnabled) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (graceDayEnabled) "روز بخشش فعال" else "روز بخشش خاموش",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (graceDayEnabled) {
                                            if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF166534)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Next Milestone teaser & progress
                    if (nextMilestone != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val daysRemaining = nextMilestone.requiredDays - maxOf(streakInfo.currentStreak, streakInfo.longestStreak)
                        Text(
                            text = "تنها ${DateTimeUtils.toPersianDigits(daysRemaining.toString())} روز تا کسب مدال «${nextMilestone.title}» (${nextMilestone.iconEmoji})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDarkTheme) Color(0xFFFDBA74) else Color(0xFF9A3412)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { nextMilestone.progressPercent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = FlameOrange,
                            trackColor = if (isDarkTheme) Color(0xFF431407) else Color(0xFFFED7AA)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Badges Section Title & Counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "نشان‌ها و دستاوردهای هیدراتاسیون",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "${DateTimeUtils.toPersianDigits(unlockedCount.toString())} از ${DateTimeUtils.toPersianDigits(badges.size.toString())} مدال",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Badges Flow Grid
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 4
            ) {
                badges.forEach { badge ->
                    BadgeItemView(
                        badge = badge,
                        onClick = { selectedBadge = badge }
                    )
                }
            }
        }
    }

    // Badge Details Dialog
    selectedBadge?.let { badge ->
        BadgeDetailDialog(
            badge = badge,
            onDismiss = { selectedBadge = null }
        )
    }

    // Streak Rules Explanatory Dialog
    if (showRulesDialog) {
        StreakRulesDialog(onDismiss = { showRulesDialog = false })
    }
}

@Composable
private fun BadgeItemView(
    badge: StreakBadge,
    onClick: () -> Unit
) {
    val tierColor = when (badge.tier) {
        BadgeTier.BRONZE -> Color(0xFFCD7F32)
        BadgeTier.SILVER -> Color(0xFF94A3B8)
        BadgeTier.GOLD -> Color(0xFFF59E0B)
        BadgeTier.DIAMOND -> Color(0xFF06B6D4)
    }

    val isDarkTheme = MaterialTheme.colorScheme.surface.let { (it.red * 0.299f + it.green * 0.587f + it.blue * 0.114f) < 0.5f }

    val cardBg = if (badge.isUnlocked) {
        if (isDarkTheme) {
            when (badge.tier) {
                BadgeTier.BRONZE -> Color(0xFF2E1A0F)
                BadgeTier.SILVER -> Color(0xFF1E293B)
                BadgeTier.GOLD -> Color(0xFF2E230B)
                BadgeTier.DIAMOND -> Color(0xFF0C2A3A)
            }
        } else {
            when (badge.tier) {
                BadgeTier.BRONZE -> Color(0xFFFFFBEB)
                BadgeTier.SILVER -> Color(0xFFF8FAFC)
                BadgeTier.GOLD -> Color(0xFFFEF3C7)
                BadgeTier.DIAMOND -> Color(0xFFECFEFF)
            }
        }
    } else {
        if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else Color(0xFFF8FAFC)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(
            width = if (badge.isUnlocked) 1.5.dp else 1.dp,
            color = if (badge.isUnlocked) tierColor.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .width(76.dp)
            .clickable(onClick = onClick)
            .testTag("badge_item_${badge.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (badge.isUnlocked) tierColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badge.iconEmoji,
                    fontSize = 22.sp,
                    modifier = Modifier.scale(if (badge.isUnlocked) 1f else 0.85f)
                )

                if (badge.isUnlocked) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(9.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = badge.title,
                fontSize = 10.sp,
                fontWeight = if (badge.isUnlocked) FontWeight.Bold else FontWeight.Medium,
                color = if (badge.isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${DateTimeUtils.toPersianDigits(badge.requiredDays.toString())} روز",
                fontSize = 9.sp,
                color = if (badge.isUnlocked) tierColor else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BadgeDetailDialog(
    badge: StreakBadge,
    onDismiss: () -> Unit
) {
    val tierColor = when (badge.tier) {
        BadgeTier.BRONZE -> Color(0xFFCD7F32)
        BadgeTier.SILVER -> Color(0xFF64748B)
        BadgeTier.GOLD -> Color(0xFFD97706)
        BadgeTier.DIAMOND -> Color(0xFF0891B2)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Medal Emoji Hero
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(tierColor.copy(alpha = 0.15f))
                        .border(2.dp, tierColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = badge.iconEmoji, fontSize = 42.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = badge.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = badge.rewardTitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = tierColor
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = badge.subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Status Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (badge.isUnlocked) Color(0xFFDCFCE7).copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (badge.isUnlocked) "وضعیت: کسب شده ✓" else "وضعیت: در مسیر پیشرفت",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (badge.isUnlocked) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "${DateTimeUtils.toPersianDigits(badge.currentProgress.toString())} / ${DateTimeUtils.toPersianDigits(badge.requiredDays.toString())} روز",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = if (badge.isUnlocked) SuccessGreen else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Health Benefit Card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "💡 فواید تندرستی این سطح:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = badge.healthBenefit,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("بسیار عالی", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun StreakRulesDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "راهنمای زنجیره و روز بخشش",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "• هر روزی که به هدف کامل آب روزانه‌تان برسید، یک روز به زنجیره اضافه می‌شود.\n" +
                            "• با تداوم روزها، مدال‌های افتخاری از برنز تا الماس آزاد می‌شوند.\n" +
                            "• سپر روز بخشش (Grace Day): اگر در یک روز نتوانید به هدف کامل برسید، سپر بخشش فعال شده و زنجیره شما شکسته نمی‌شود تا با آرامش ادامه دهید.\n" +
                            "• با نوشیدن منظم و پایبندی، بدن شما بهترین عادت سلامتی را می‌سازد.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("متوجه شدم", fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}
