package com.example.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.DateTimeUtils
import com.example.presentation.theme.NooshPrimary
import com.example.presentation.theme.NooshSubtleBlue

@Composable
fun GlassRowIndicator(
    consumedGlasses: Int,
    totalGoalGlasses: Int,
    onGlassClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${DateTimeUtils.toPersianDigits(consumedGlasses.toString())} از ${DateTimeUtils.toPersianDigits(totalGoalGlasses.toString())} لیوان امروز",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "${DateTimeUtils.toPersianDigits((consumedGlasses * 250).toString())} میلی‌لیتر",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Row of glasses
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("glass_row_indicator"),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val count = totalGoalGlasses.coerceIn(4, 10)
            for (i in 1..count) {
                val isFilled = i <= consumedGlasses
                GlassItem(
                    index = i,
                    isFilled = isFilled,
                    onClick = onGlassClick
                )
            }
        }
    }
}

@Composable
private fun GlassItem(
    index: Int,
    isFilled: Boolean,
    onClick: () -> Unit
) {
    val glassShape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp, topStart = 3.dp, topEnd = 3.dp)

    Box(
        modifier = Modifier
            .size(width = 32.dp, height = 44.dp)
            .clip(glassShape)
            .background(if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.5.dp,
                color = if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = glassShape
            )
            .clickable { onClick() }
            .testTag("glass_item_$index"),
        contentAlignment = Alignment.Center
    ) {
        if (isFilled) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "خورده شده",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Text(
                text = DateTimeUtils.toPersianDigits(index.toString()),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
