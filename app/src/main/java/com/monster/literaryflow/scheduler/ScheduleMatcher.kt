package com.monster.literaryflow.scheduler

import com.monster.literaryflow.core.model.ScheduleType
import com.monster.literaryflow.core.model.TaskModel
import java.time.DayOfWeek
import java.time.LocalDateTime

object ScheduleMatcher {
    fun isDue(task: TaskModel, now: LocalDateTime = LocalDateTime.now()): Boolean {
        return when (task.scheduleType) {
            ScheduleType.MANUAL -> false
            ScheduleType.LOOP -> true
            ScheduleType.RUN_COUNT -> true
            ScheduleType.DAILY -> inTimeWindow(now, task.startTime, task.endTime)
            ScheduleType.WEEKLY -> {
                val days = task.weeklyDays.orEmpty().split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
                val day = now.dayOfWeek.toLegacyDay()
                day in days && inTimeWindow(now, task.startTime, task.endTime)
            }
        }
    }

    private fun inTimeWindow(now: LocalDateTime, start: String?, end: String?): Boolean {
        val startMinutes = start.toMinutes() ?: return true
        val endMinutes = end.toMinutes() ?: return true
        val current = now.hour * 60 + now.minute
        return if (startMinutes <= endMinutes) {
            current in startMinutes..endMinutes
        } else {
            current >= startMinutes || current <= endMinutes
        }
    }

    private fun String?.toMinutes(): Int? {
        if (this == null) return null
        val parts = split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        return if (hour in 0..23 && minute in 0..59) hour * 60 + minute else null
    }

    private fun DayOfWeek.toLegacyDay(): Int = when (this) {
        DayOfWeek.SUNDAY -> 1
        DayOfWeek.MONDAY -> 2
        DayOfWeek.TUESDAY -> 3
        DayOfWeek.WEDNESDAY -> 4
        DayOfWeek.THURSDAY -> 5
        DayOfWeek.FRIDAY -> 6
        DayOfWeek.SATURDAY -> 7
    }
}
