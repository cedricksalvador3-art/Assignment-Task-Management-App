package com.example.assignment_task_management_app.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    private val dateTimeFormat = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

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
        val hours = diff / (1000 * 60 * 60)
        return diff > 0 && hours <= hoursThreshold
    }

    fun isOverdue(dueDate: Long): Boolean {
        return dueDate < System.currentTimeMillis()
    }

    fun getRemainingTimeString(dueDate: Long): String {
        val diff = dueDate - System.currentTimeMillis()
        if (diff <= 0) return "Overdue"
        val hours = diff / (1000 * 60 * 60)
        val days = hours / 24
        return when {
            days > 0 -> "$days day${if (days > 1) "s" else ""} left"
            hours > 0 -> "$hours hour${if (hours > 1) "s" else ""} left"
            else -> "Due in less than an hour"
        }
    }

    fun combineDateAndTime(year: Int, month: Int, dayOfMonth: Int, hourOfDay: Int, minute: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, dayOfMonth, hourOfDay, minute, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
