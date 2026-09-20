package com.example.core.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeUtils {
    val zoneId: ZoneId
        get() = ZoneId.systemDefault()

    fun getStartOfDayMillis(date: LocalDate = LocalDate.now(zoneId)): Long {
        return date.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun getEndOfOfDayMillis(date: LocalDate = LocalDate.now(zoneId)): Long {
        return date.atTime(LocalTime.MAX).atZone(zoneId).toInstant().toEpochMilli()
    }

    fun getTodayDateString(): String {
        return LocalDate.now(zoneId).format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    fun formatDate(epochMillis: Long): String {
        val zdt = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        return zdt.format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))
    }

    fun getPersianDayName(date: LocalDate): String {
        return when (date.dayOfWeek) {
            DayOfWeek.SATURDAY -> "شنبه"
            DayOfWeek.SUNDAY -> "۱شنبه"
            DayOfWeek.MONDAY -> "۲شنبه"
            DayOfWeek.TUESDAY -> "۳شنبه"
            DayOfWeek.WEDNESDAY -> "۴شنبه"
            DayOfWeek.THURSDAY -> "۵شنبه"
            DayOfWeek.FRIDAY -> "جمعه"
            else -> "شنبه"
        }
    }

    fun parseTime(timeStr: String): LocalTime {
        return try {
            LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e: Exception) {
            LocalTime.of(8, 0)
        }
    }

    fun toPersianDigits(input: String): String {
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val sb = StringBuilder()
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(persianDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun parseDateToStartOfDayMillis(dateStr: String): Long {
        return try {
            val date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
            getStartOfDayMillis(date)
        } catch (e: Exception) {
            getStartOfDayMillis()
        }
    }

    fun parseDateToEndOfDayMillis(dateStr: String): Long {
        return try {
            val date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
            getEndOfOfDayMillis(date)
        } catch (e: Exception) {
            getEndOfOfDayMillis()
        }
    }

    fun formatDateIso(epochMillis: Long): String {
        val zdt = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        return zdt.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }
}
