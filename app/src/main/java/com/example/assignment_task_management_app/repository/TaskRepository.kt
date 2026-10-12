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
    val overdueTasks: Int = 0,
    val highPriorityCount: Int = 0,
    val mediumPriorityCount: Int = 0,
    val lowPriorityCount: Int = 0,
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
        val defaultSubjects = listOf("History", "Mathematics", "Science", "English", "Programming")
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
        val completed = allTasks.count { it.status == TaskStatus.COMPLETED }
        val overdue = allTasks.count { it.status != TaskStatus.COMPLETED && DateUtils.isOverdue(it.dueDate) }
        val pending = allTasks.count { it.status == TaskStatus.PENDING && !DateUtils.isOverdue(it.dueDate) }
        val inProgress = allTasks.count { it.status == TaskStatus.IN_PROGRESS && !DateUtils.isOverdue(it.dueDate) }

        val highCount = allTasks.count { it.priority == TaskPriority.HIGH }
        val mediumCount = allTasks.count { it.priority == TaskPriority.MEDIUM }
        val lowCount = allTasks.count { it.priority == TaskPriority.LOW }

        val percentage = if (total > 0) ((completed.toDouble() / total.toDouble()) * 100).toInt() else 0

        return DashboardStats(
            totalTasks = total,
            pendingTasks = pending + inProgress,
            inProgressTasks = inProgress,
            completedTasks = completed,
            overdueTasks = overdue,
            highPriorityCount = highCount,
            mediumPriorityCount = mediumCount,
            lowPriorityCount = lowCount,
            completionPercentage = percentage
        )
    }

    suspend fun getDueSoonTasks(userId: Long): List<Task> {
        val allTasks = taskDao.getTasksForUser(userId)
        return allTasks.filter {
            it.status != TaskStatus.COMPLETED
        }.sortedBy { it.dueDate }
    }
}
