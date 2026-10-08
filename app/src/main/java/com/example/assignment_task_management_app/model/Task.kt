package com.example.assignment_task_management_app.model

data class Task(
    val id: Long = 0,
    val userId: Long,
    val title: String,
    val description: String,
    val subject: String,
    val dueDate: Long, // Timestamp in milliseconds
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val status: TaskStatus = TaskStatus.PENDING,
    val isReminderSet: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
