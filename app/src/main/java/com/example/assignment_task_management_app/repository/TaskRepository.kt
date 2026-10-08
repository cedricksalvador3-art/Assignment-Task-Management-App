package com.example.assignment_task_management_app.repository

import com.example.assignment_task_management_app.db.TaskDao
import com.example.assignment_task_management_app.model.Task
import com.example.assignment_task_management_app.model.TaskPriority
import com.example.assignment_task_management_app.model.TaskStatus
import com.example.assignment_task_management_app.utils.DateUtils

data class DashboardStats(
    val totalTasks: Int = 0,
    val pendingTasks: Int = 0,
    val inProgressTasks: Int = 0,
    val completedTasks: Int = 0,
    val dueSoonTasks: Int = 0,
    val completionPercentage: Int = 0
)

class TaskRepository(private val taskDao: TaskDao) {

    suspend fun getTasksForUser(userId: Long): List<Task> = taskDao.getTasksForUser(userId)

    suspend fun getTaskById(taskId: Long): Task? = taskDao.getTaskById(taskId)

    suspend fun insertTask(task: Task): Result<Long> {
        return try {
            if (task.title.isBlank()) {
                return Result.failure(Exception("Task title is required."))
            }
            if (task.subject.isBlank()) {
                return Result.failure(Exception("Subject is required."))
            }
            val id = taskDao.insertTask(task)
            if (id > 0) {
                Result.success(id)
            } else {
                Result.failure(Exception("Failed to save task to database."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTask(task: Task): Result<Boolean> {
        return try {
            if (task.title.isBlank()) {
                return Result.failure(Exception("Task title is required."))
            }
            if (task.subject.isBlank()) {
                return Result.failure(Exception("Subject is required."))
            }
            val success = taskDao.updateTask(task)
            if (success) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to update task."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTask(taskId: Long): Boolean = taskDao.deleteTaskById(taskId)

    suspend fun updateTaskStatus(taskId: Long, status: TaskStatus): Boolean {
        return taskDao.updateTaskStatus(taskId, status)
    }

    suspend fun searchAndFilterTasks(
        userId: Long,
        query: String = "",
        status: TaskStatus? = null,
        priority: TaskPriority? = null,
        subject: String? = null,
        sortBy: String = "DUE_DATE_ASC"
    ): List<Task> {
        return taskDao.searchAndFilterTasks(userId, query, status, priority, subject, sortBy)
    }

    suspend fun getSubjectsForUser(userId: Long): List<String> {
        val subjects = taskDao.getSubjectsForUser(userId).toMutableList()
        val defaultSubjects = listOf("Mobile App Dev", "Database Systems", "Software Engineering", "Mathematics", "Physics", "General")
        for (def in defaultSubjects) {
            if (!subjects.contains(def)) {
                subjects.add(def)
            }
        }
        return subjects
    }

    suspend fun getDashboardStats(userId: Long): DashboardStats {
        val allTasks = taskDao.getTasksForUser(userId)
        val total = allTasks.size
        val pending = allTasks.count { it.status == TaskStatus.PENDING }
        val inProgress = allTasks.count { it.status == TaskStatus.IN_PROGRESS }
        val completed = allTasks.count { it.status == TaskStatus.COMPLETED }
        val dueSoon = allTasks.count {
            it.status != TaskStatus.COMPLETED && DateUtils.isDueSoon(it.dueDate)
        }
        val percentage = if (total > 0) ((completed.toDouble() / total.toDouble()) * 100).toInt() else 0

        return DashboardStats(
            totalTasks = total,
            pendingTasks = pending,
            inProgressTasks = inProgress,
            completedTasks = completed,
            dueSoonTasks = dueSoon,
            completionPercentage = percentage
        )
    }

    suspend fun getDueSoonTasks(userId: Long): List<Task> {
        val allTasks = taskDao.getTasksForUser(userId)
        return allTasks.filter {
            it.status != TaskStatus.COMPLETED && DateUtils.isDueSoon(it.dueDate, 48)
        }.sortedBy { it.dueDate }
    }
}
