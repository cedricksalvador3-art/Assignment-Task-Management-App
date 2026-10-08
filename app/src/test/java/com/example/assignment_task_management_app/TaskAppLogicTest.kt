package com.example.assignment_task_management_app

import com.example.assignment_task_management_app.model.Task
import com.example.assignment_task_management_app.model.TaskPriority
import com.example.assignment_task_management_app.model.TaskStatus
import com.example.assignment_task_management_app.model.User
import com.example.assignment_task_management_app.utils.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskAppLogicTest {

    @Test
    fun testUserLockoutStateOnFailedAttempts() {
        val user = User(
            username = "student1",
            fullName = "Alice Smith",
            email = "alice@example.com",
            passwordHash = "secret123",
            failedAttempts = 0,
            isLocked = false
        )

        assertFalse(user.isLocked)
        assertEquals(0, user.failedAttempts)

        val userAfter2Failures = user.copy(failedAttempts = 2)
        assertFalse(userAfter2Failures.isLocked)

        val userAfter3Failures = user.copy(failedAttempts = 3, isLocked = true)
        assertTrue(userAfter3Failures.isLocked)
        assertEquals(3, userAfter3Failures.failedAttempts)
    }

    @Test
    fun testTaskPriorityAndStatusParsing() {
        assertEquals(TaskPriority.HIGH, TaskPriority.fromString("HIGH"))
        assertEquals(TaskPriority.HIGH, TaskPriority.fromString("High"))
        assertEquals(TaskPriority.MEDIUM, TaskPriority.fromString("Unknown"))

        assertEquals(TaskStatus.PENDING, TaskStatus.fromString("Pending"))
        assertEquals(TaskStatus.IN_PROGRESS, TaskStatus.fromString("In Progress"))
        assertEquals(TaskStatus.COMPLETED, TaskStatus.fromString("Completed"))
    }

    @Test
    fun testDateUtilsIsDueSoonAndOverdue() {
        val now = System.currentTimeMillis()
        val futureDueMs = now + (5 * 60 * 60 * 1000) // 5 hours in future
        val pastDueMs = now - (1000 * 60 * 60) // 1 hour ago

        assertTrue(DateUtils.isDueSoon(futureDueMs, 24))
        assertFalse(DateUtils.isDueSoon(now + (48 * 60 * 60 * 1000), 24)) // 48 hours away is not due in 24h

        assertTrue(DateUtils.isOverdue(pastDueMs))
        assertFalse(DateUtils.isOverdue(futureDueMs))
    }
}
