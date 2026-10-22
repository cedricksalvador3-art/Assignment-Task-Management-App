package com.example.assignment_task_management_app.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtils {

    val PHILIPPINE_TIME_ZONE: TimeZone = TimeZone.getTimeZone("Asia/Manila")

    private val dateTimeFormat = SimpleDateFormat("MMM dd, yyyy - h:mm a", Locale.US).apply {
        timeZone = PHILIPPINE_TIME_ZONE
    }
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US).apply {
        timeZone = PHILIPPINE_TIME_ZONE
    }
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.US).apply {
        timeZone = PHILIPPINE_TIME_ZONE
    }

    fun getPhilippineCalendar(): Calendar {
        return Calendar.getInstance(PHILIPPINE_TIME_ZONE, Locale.US)
    }

    fun formatDateTime(timestamp: Long): String {
        return dateTimeFormat.format(Date(timestamp))
    }

    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }

    fun isDueSoon(dueDate: Long, hoursThreshold: Int = 24): Boolean {
        val now = System.currentTimeMillis()
        val diff = dueDate - now
        return diff > 0 && diff <= (hoursThreshold * 60 * 60 * 1000L)
    }

    fun isOverdue(dueDate: Long): Boolean {
        return dueDate < System.currentTimeMillis()
    }

    fun getRemainingTimeString(dueDate: Long): String {
        val diff = dueDate - System.currentTimeMillis()
        if (diff <= 0) return "Overdue"

        // Round up to nearest minute
        val totalMinutes = (diff + 59999L) / (1000L * 60L)
        val totalHours = totalMinutes / 60L
        val days = totalHours / 24L
        val remainingHours = totalHours % 24L
        val remainingMinutes = totalMinutes % 60L

        return when {
            days > 0 -> {
                if (remainingHours > 0) {
                    "$days day${if (days > 1) "s" else ""} $remainingHours hr${if (remainingHours > 1) "s" else ""} left"
                } else {
                    "$days day${if (days > 1) "s" else ""} left"
                }
            }
            totalHours > 0 -> {
                if (remainingMinutes > 0) {
                    "$totalHours hr${if (totalHours > 1) "s" else ""} $remainingMinutes min${if (remainingMinutes > 1) "s" else ""} left"
                } else {
                    "$totalHours hr${if (totalHours > 1) "s" else ""} left"
                }
            }
            totalMinutes > 0 -> {
                "$totalMinutes min${if (totalMinutes > 1) "s" else ""} left"
            }
            else -> "Due in less than a minute"
        }
    }

    fun combineDateAndTime(year: Int, month: Int, dayOfMonth: Int, hourOfDay: Int, minute: Int): Long {
        val calendar = getPhilippineCalendar()
        calendar.set(year, month, dayOfMonth, hourOfDay, minute, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
