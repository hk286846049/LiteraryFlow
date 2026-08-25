package com.monster.literaryflow.scheduler

import com.monster.literaryflow.core.model.ScheduleType
import com.monster.literaryflow.core.model.TaskModel
import java.time.LocalDateTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleMatcherTest {
    @Test
    fun matchesDailyAndWeeklyWindows() {
        val daily = task(ScheduleType.DAILY, start = "08:00", end = "09:00")
        val weekly = task(ScheduleType.WEEKLY, start = "08:00", end = "09:00", days = "2")
        val monday = LocalDateTime.of(2026, 8, 24, 8, 30)
        assertTrue(ScheduleMatcher.isDue(daily, monday))
        assertTrue(ScheduleMatcher.isDue(weekly, monday))
        assertFalse(ScheduleMatcher.isDue(weekly, monday.plusDays(1)))
    }

    private fun task(
        schedule: ScheduleType,
        start: String?,
        end: String?,
        days: String? = null
    ) = TaskModel(
        id = 1,
        title = "t",
        enabled = true,
        targetPackageName = "",
        targetAppName = "",
        allowAutoOpenApp = false,
        scheduleType = schedule,
        startTime = start,
        endTime = end,
        weeklyDays = days,
        runTimes = 1,
        intervalMs = 0,
        createdAt = 0,
        updatedAt = 0
    )
}
