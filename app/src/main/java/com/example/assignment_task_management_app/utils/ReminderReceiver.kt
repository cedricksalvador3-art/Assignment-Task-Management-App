package com.example.assignment_task_management_app.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("EXTRA_TASK_ID", -1L)
        val title = intent.getStringExtra("EXTRA_TASK_TITLE") ?: "Upcoming Task"
        val subject = intent.getStringExtra("EXTRA_TASK_SUBJECT") ?: "Assignment"
        val remainingText = intent.getStringExtra("EXTRA_TASK_REMAINING") ?: "Due Soon"

        if (taskId != -1L) {
            NotificationHelper.showTaskReminderNotification(context, taskId, title, subject, remainingText)
        }
    }
}
