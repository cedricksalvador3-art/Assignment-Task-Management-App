package com.example.assignment_task_management_app.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.assignment_task_management_app.model.Task

object AlarmScheduler {

    fun scheduleTaskReminder(context: Context, task: Task) {
        if (!task.isReminderSet) return

        val now = System.currentTimeMillis()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        // 1. Exact Due Date Alarm (Right when the deadline arrives)
        if (task.dueDate > now) {
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("EXTRA_TASK_ID", task.id)
                putExtra("EXTRA_TASK_TITLE", task.title)
                putExtra("EXTRA_TASK_SUBJECT", task.subject)
                putExtra("EXTRA_TASK_REMAINING", "Due Now!")
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                task.id.toInt(),
                intent,
                pendingIntentFlags
            )

            setAlarm(alarmManager, task.dueDate, pendingIntent)
        }

        // 2. Advance Heads-up Reminder Alarm (15 minutes before due date)
        val advanceTime = task.dueDate - (15 * 60 * 1000L)
        if (advanceTime > now) {
            val advanceIntent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("EXTRA_TASK_ID", task.id)
                putExtra("EXTRA_TASK_TITLE", task.title)
                putExtra("EXTRA_TASK_SUBJECT", task.subject)
                putExtra("EXTRA_TASK_REMAINING", "Due in 15 minutes!")
            }

            val advancePendingIntent = PendingIntent.getBroadcast(
                context,
                (task.id + 100000L).toInt(),
                advanceIntent,
                pendingIntentFlags
            )

            setAlarm(alarmManager, advanceTime, advancePendingIntent)
        }
    }

    private fun setAlarm(alarmManager: AlarmManager, triggerTime: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelTaskReminder(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        // Cancel exact due date alarm
        val intent1 = Intent(context, ReminderReceiver::class.java)
        val pendingIntent1 = PendingIntent.getBroadcast(context, taskId.toInt(), intent1, pendingIntentFlags)
        alarmManager.cancel(pendingIntent1)

        // Cancel 15-minute advance alarm
        val intent2 = Intent(context, ReminderReceiver::class.java)
        val pendingIntent2 = PendingIntent.getBroadcast(context, (taskId + 100000L).toInt(), intent2, pendingIntentFlags)
        alarmManager.cancel(pendingIntent2)
    }
}
