package com.example

import com.example.core.database.entity.ReminderEntity
import com.example.core.permissions.LocationSearchHelper
import com.example.core.permissions.ManualLocationInput
import com.example.core.reminders.ReminderIntents
import com.example.core.reminders.ReminderRingtoneService
import com.example.core.reminders.ReminderScheduler
import com.example.core.theme.AccentTheme
import com.example.core.ui.components.nextFutureTriggerMillis
import com.example.feature.doctorsearch.DoctorSearchDictionary
import com.example.feature.wallet.CalculatorEvaluator
import com.example.feature.wellness.sleepDurationMinutes
import com.example.feature.healthinfo.healthInformationSearchUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class MyCareLogicUnitTest {

    @Test
    fun wellness_sleepDurationIsCalculatedInWholeMinutes() {
        assertEquals(7 * 60L + 30L, sleepDurationMinutes(0L, (7 * 60L + 30L) * 60_000L))
        assertEquals(0L, sleepDurationMinutes(10_000L, 5_000L))
    }

    @Test
    fun healthInformationSearchUsesTrustedSitesAndEncodesQuery() {
        val url = healthInformationSearchUrl("blood pressure & sleep")
        assertTrue(url.startsWith("https://www.google.com/search?q="))
        assertTrue(url.contains("medlineplus.gov"))
        assertTrue(url.contains("nhs.uk"))
        assertTrue(url.contains("blood+pressure+%26+sleep"))
    }

    @Test
    fun calculator_evaluatesPrecedenceAndParentheses() {
        assertEquals("14", CalculatorEvaluator.evaluate("2 + 3 * 4").getOrThrow())
        assertEquals("20", CalculatorEvaluator.evaluate("(2 + 3) × 4").getOrThrow())
        assertEquals("12.5", CalculatorEvaluator.evaluate("25 ÷ 2").getOrThrow())
        assertEquals("0.5", CalculatorEvaluator.evaluate("50%").getOrThrow())
        assertEquals("-3", CalculatorEvaluator.evaluate("−8 + 5").getOrThrow())
    }

    @Test
    fun calculator_handlesErrorsGracefully() {
        assertTrue(CalculatorEvaluator.evaluate("10 / 0").isFailure)
        assertTrue(CalculatorEvaluator.evaluate("(2 + 3").isFailure)
        assertEquals("0", CalculatorEvaluator.evaluate("   ").getOrThrow())
    }

    @Test
    fun doctorSearch_matchesEnglishBanglaAndBanglish() {
        val eye = DoctorSearchDictionary.searchSpecialties("chokher somossa")
        assertTrue(eye.any { it.specialtyEnglish == "Ophthalmologist" })

        val heartBn = DoctorSearchDictionary.searchSpecialties("বুকে ব্যথা")
        assertTrue(heartBn.any { it.specialtyEnglish == "Cardiologist" })

        val toothFuzzy = DoctorSearchDictionary.searchSpecialties("toothache")
        assertTrue(toothFuzzy.any { it.specialtyEnglish == "Dentist" })

        val dengue = DoctorSearchDictionary.searchSpecialties("dengu jor")
        assertTrue(dengue.any { it.specialtyEnglish.contains("General Physician") })

        val empty = DoctorSearchDictionary.searchSpecialties("   ")
        assertTrue(empty.isEmpty())

        val unknown = DoctorSearchDictionary.searchSpecialties("xyzunknownsymptom123")
        assertEquals(1, unknown.size)
        assertTrue(unknown.first().isGeneralFallback)
        assertFalse(eye.first().isGeneralFallback)
    }

    @Test
    fun reminderScheduler_computesNextTriggerTimes() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 25, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val pastOnce = ReminderEntity(
            title = "Past",
            triggerTimeMillis = now - 60_000L,
            repeatType = "ONCE"
        )
        assertNull(ReminderScheduler.computeNextTriggerMillis(pastOnce, now))

        val futureOnce = pastOnce.copy(triggerTimeMillis = now + 60_000L)
        assertEquals(now + 60_000L, ReminderScheduler.computeNextTriggerMillis(futureOnce, now))

        // Daily reminder at 09:00 AM when it's already 10:00 AM -> next day
        val morningCal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
        }
        val daily = ReminderEntity(
            title = "Daily",
            triggerTimeMillis = morningCal.timeInMillis,
            repeatType = "DAILY"
        )
        val nextDaily = ReminderScheduler.computeNextTriggerMillis(daily, now)
        assertNotNull(nextDaily)
        assertTrue(nextDaily!! > now)
        val nextCal = Calendar.getInstance().apply { timeInMillis = nextDaily }
        assertEquals(9, nextCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, nextCal.get(Calendar.MINUTE))

        // Custom weekday reminder
        val customWeekdays = ReminderEntity(
            title = "MWF",
            triggerTimeMillis = morningCal.timeInMillis,
            repeatType = "CUSTOM",
            weekdaysCsv = "${Calendar.FRIDAY}"
        )
        val nextFriday = ReminderScheduler.computeNextTriggerMillis(customWeekdays, now)
        assertNotNull(nextFriday)
        val fridayCal = Calendar.getInstance().apply { timeInMillis = nextFriday!! }
        assertEquals(Calendar.FRIDAY, fridayCal.get(Calendar.DAY_OF_WEEK))
    }

    @Test
    fun reminderScheduler_ignoresInvalidWeekdaysAndPreservesWallClockAcrossDst() {
        val prevTz = TimeZone.getDefault()
        try {
            // America/New_York springs forward on Sunday March 8, 2026 at 02:00 AM
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
            val beforeDst = Calendar.getInstance().apply {
                set(2026, Calendar.MARCH, 7, 9, 30, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val afterSpringForwardCheck = Calendar.getInstance().apply {
                set(2026, Calendar.MARCH, 7, 10, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val intervalCustom = ReminderEntity(
                title = "DST Safe",
                triggerTimeMillis = beforeDst,
                repeatType = "CUSTOM",
                repeatIntervalDays = 1,
                weekdaysCsv = "0,99,invalid"
            )
            val next = ReminderScheduler.computeNextTriggerMillis(intervalCustom, afterSpringForwardCheck)
            assertNotNull(next)
            val nextCal = Calendar.getInstance().apply { timeInMillis = next!! }
            assertEquals(8, nextCal.get(Calendar.DAY_OF_MONTH))
            assertEquals(9, nextCal.get(Calendar.HOUR_OF_DAY))
            assertEquals(30, nextCal.get(Calendar.MINUTE))
        } finally {
            TimeZone.setDefault(prevTz)
        }
    }

    @Test
    fun reminderIntents_generateUniqueRequestCodesAndNotificationIds() {
        val codes = (1L..50L).flatMap { id ->
            listOf(0, 1, 2, 3, 10, 11).map { offset -> ReminderIntents.requestCode(id, offset) }
        }
        assertEquals(300, codes.toSet().size)
        assertNotEquals(
            ReminderRingtoneService.notificationIdFor(1L),
            ReminderRingtoneService.notificationIdFor(2L)
        )
    }

    @Test
    fun nextFutureTriggerMillis_rollsSameDayPastTimeToTomorrow() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 25, 21, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val scheduled = nextFutureTriggerMillis(
            dateMillis = now,
            hour = 8,
            minute = 30,
            repeatType = "ONCE",
            nowMillis = now
        )
        assertTrue(scheduled > now)
        val cal = Calendar.getInstance().apply { timeInMillis = scheduled }
        assertEquals(26, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(8, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, cal.get(Calendar.MINUTE))
    }

    @Test
    fun accentThemes_supportExactFourThemesAndLegacyFallback() {
        assertEquals(4, AccentTheme.entries.size)
        assertEquals(AccentTheme.SOFT_ASH, AccentTheme.fromKey("soft_ash"))
        assertEquals(AccentTheme.SOFT_PINK, AccentTheme.fromKey("soft_pink"))
        assertEquals(AccentTheme.SOFT_BLUE, AccentTheme.fromKey("soft_blue"))
        assertEquals(AccentTheme.SOFT_WHITE_GREEN, AccentTheme.fromKey("soft_white_green"))
        assertEquals(AccentTheme.SOFT_WHITE_GREEN, AccentTheme.fromKey("teal"))
    }

    @Test
    fun locationSearchHelper_formatsManualAndCurrentLocationQueries() {
        val manual = ManualLocationInput(
            district = "Narsingdi",
            thana = "Narsingdi Sadar",
            city = "Madhabdi",
            area = "Birampur"
        )
        assertEquals("Birampur, Madhabdi, Narsingdi Sadar, Narsingdi", manual.toFormattedLocation())

        val groceryQuery = LocationSearchHelper.buildMapsQueryText(
            baseQuery = "grocery shop supermarket",
            useCurrentLocation = false,
            manualLocation = manual
        )
        assertEquals(
            "grocery shop supermarket Birampur, Madhabdi, Narsingdi Sadar, Narsingdi",
            groceryQuery
        )

        val gpsDoctorQuery = LocationSearchHelper.buildMapsQueryText(
            baseQuery = "Cardiologist",
            useCurrentLocation = true,
            manualLocation = ManualLocationInput()
        )
        assertEquals("Cardiologist near me", gpsDoctorQuery)
    }
}
