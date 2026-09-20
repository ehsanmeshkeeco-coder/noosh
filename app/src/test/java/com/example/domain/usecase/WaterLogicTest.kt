package com.example.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WaterLogicTest {

    @Test
    fun `test goal achievement detection`() {
        val dailyGoal = 2000
        val previousTotal = 1750
        val addedAmount = 250
        val newTotal = previousTotal + addedAmount

        val wasAchievedBefore = previousTotal >= dailyGoal
        val isAchievedNow = newTotal >= dailyGoal
        val isJustAchieved = !wasAchievedBefore && isAchievedNow

        assertFalse(wasAchievedBefore)
        assertTrue(isAchievedNow)
        assertTrue(isJustAchieved)
    }

    @Test
    fun `test streak calculation with grace day`() {
        // If user missed 1 day and graceDayEnabled is true, streak is preserved
        val currentStreak = 5
        val missedDays = 1
        val graceDayAvailable = true

        val finalStreak = if (missedDays == 1 && graceDayAvailable) {
            currentStreak // preserved
        } else if (missedDays > 0) {
            1
        } else {
            currentStreak + 1
        }

        assertEquals(5, finalStreak)
    }
}
