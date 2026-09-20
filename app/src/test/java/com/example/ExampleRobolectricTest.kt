package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.util.DateTimeUtils
import com.example.domain.model.WaterIntake
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertTrue(appName == "نوش" || appName == "Noosh")
    }

    @Test
    fun `test persian digits conversion`() {
        val englishDigits = "123450"
        val persian = DateTimeUtils.toPersianDigits(englishDigits)
        assertEquals("۱۲۳۴۵۰", persian)
    }

    @Test
    fun `test water calculation`() {
        val goalMl = 2000
        val consumedMl = 1500
        val percentage = ((consumedMl.toFloat() / goalMl) * 100).toInt()
        val glasses = consumedMl / 250
        assertEquals(75, percentage)
        assertEquals(6, glasses)
    }
}
