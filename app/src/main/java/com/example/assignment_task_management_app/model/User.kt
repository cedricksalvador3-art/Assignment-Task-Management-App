package com.example.assignment_task_management_app.model

data class User(
    val id: Long = 0,
    val username: String,
    val fullName: String,
    val email: String,
    val passwordHash: String,
    val failedAttempts: Int = 0,
    val isLocked: Boolean = false
)
