package com.example.assignment_task_management_app.model

enum class TaskPriority(val displayName: String) {
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low");

    companion object {
        fun fromString(value: String): TaskPriority {
            return values().find { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) } ?: MEDIUM
        }
    }
}
