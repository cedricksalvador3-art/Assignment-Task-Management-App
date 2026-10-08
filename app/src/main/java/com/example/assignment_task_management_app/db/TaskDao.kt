package com.example.assignment_task_management_app.db

import com.example.assignment_task_management_app.model.Task
import com.example.assignment_task_management_app.model.TaskPriority
import com.example.assignment_task_management_app.model.TaskStatus

interface TaskDao {
    suspend fun getTasksForUser(userId: Long): List<Task>
    suspend fun getTaskById(taskId: Long): Task?
    suspend fun insertTask(task: Task): Long
    suspend fun updateTask(task: Task): Boolean
    suspend fun deleteTask(task: Task): Boolean
    suspend fun deleteTaskById(taskId: Long): Boolean
    suspend fun updateTaskStatus(taskId: Long, status: TaskStatus): Boolean
    suspend fun searchAndFilterTasks(
        userId: Long,
        query: String = "",
        status: TaskStatus? = null,
        priority: TaskPriority? = null,
        subject: String? = null,
        sortBy: String = "DUE_DATE_ASC"
    ): List<Task>
    suspend fun getSubjectsForUser(userId: Long): List<String>
    suspend fun getTasksDueBetween(userId: Long, startMs: Long, endMs: Long): List<Task>
}
