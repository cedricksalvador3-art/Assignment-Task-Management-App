package com.example.assignment_task_management_app

import com.example.assignment_task_management_app.db.UserDao
import com.example.assignment_task_management_app.model.TaskPriority
import com.example.assignment_task_management_app.model.TaskStatus
import com.example.assignment_task_management_app.model.User
import com.example.assignment_task_management_app.repository.LoginResult
import com.example.assignment_task_management_app.repository.UserRepository
import com.example.assignment_task_management_app.utils.DateUtils
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskAppLogicTest {

    private class FakeUserDao : UserDao {
        private val users = mutableMapOf<Long, User>()
        private var nextId = 1L

        override suspend fun getUserByUsername(username: String): User? {
            return users.values.find { it.username.equals(username, ignoreCase = true) }
        }

        override suspend fun getUserByEmail(email: String): User? {
            return users.values.find { it.email.equals(email, ignoreCase = true) }
        }

        override suspend fun getUserById(id: Long): User? = users[id]

        override suspend fun insertUser(user: User): Long {
            val id = nextId++
            val newUser = user.copy(id = id)
            users[id] = newUser
            return id
        }

        override suspend fun updateUser(user: User): Boolean {
            if (users.containsKey(user.id)) {
                users[user.id] = user
                return true
            }
            return false
        }
    }

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
    fun testLockoutOnlyTriggeredForValidExistingAccount() = runBlocking {
        val userDao = FakeUserDao()
        val repository = UserRepository(userDao)

        // Seed a valid user
        userDao.insertUser(
            User(
                username = "janedoe",
                fullName = "Jane Doe",
                email = "jane.doe@email.com",
                passwordHash = "Password123"
            )
        )

        // 1. Invalid identifier should return Error and NOT trigger lockout or increment attempts
        val invalidResult1 = repository.loginUser("nonexistent@user.com", "wrongpass")
        assertTrue(invalidResult1 is LoginResult.Error)

        val invalidResult2 = repository.loginUser("nonexistent@user.com", "wrongpass")
        assertTrue(invalidResult2 is LoginResult.Error)

        val invalidResult3 = repository.loginUser("nonexistent@user.com", "wrongpass")
        assertTrue(invalidResult3 is LoginResult.Error)

        // 2. Valid user should still have 0 failed attempts
        val validUserBefore = userDao.getUserByUsername("janedoe")!!
        assertEquals(0, validUserBefore.failedAttempts)
        assertFalse(validUserBefore.isLocked)
    }

    @Test
    fun testThreeFailedAttemptsLocksAccountAndSuccessResetsCounter() = runBlocking {
        val userDao = FakeUserDao()
        val repository = UserRepository(userDao)

        // Seed user
        userDao.insertUser(
            User(
                username = "janedoe",
                fullName = "Jane Doe",
                email = "jane.doe@email.com",
                passwordHash = "Password123"
            )
        )

        // Attempt 1: Failed
        val result1 = repository.loginUser("janedoe", "wrong1")
        assertTrue(result1 is LoginResult.Failed)
        assertEquals(2, (result1 as LoginResult.Failed).remainingAttempts)

        // Successful login before 3 attempts should reset failed counter to 0
        val successResult = repository.loginUser("janedoe", "Password123")
        assertTrue(successResult is LoginResult.Success)

        val userAfterSuccess = userDao.getUserByUsername("janedoe")!!
        assertEquals(0, userAfterSuccess.failedAttempts)
        assertFalse(userAfterSuccess.isLocked)

        // Now fail 3 consecutive attempts
        repository.loginUser("janedoe", "wrong1")
        repository.loginUser("janedoe", "wrong2")
        val lockedResult = repository.loginUser("janedoe", "wrong3")

        assertTrue(lockedResult is LoginResult.Locked)

        val lockedUser = userDao.getUserByUsername("janedoe")!!
        assertTrue(lockedUser.isLocked)
        assertEquals(3, lockedUser.failedAttempts)
        assertTrue(lockedUser.lockedUntil > System.currentTimeMillis())
    }

    @Test
    fun testExpiredLockoutPeriodAutoUnlocksAccount() = runBlocking {
        val userDao = FakeUserDao()
        val repository = UserRepository(userDao)

        // Seed a locked user whose lockout period expired in the past
        val expiredLockoutTime = System.currentTimeMillis() - 1000L // Expired 1 second ago
        val userId = userDao.insertUser(
            User(
                username = "lockeduser",
                fullName = "Locked User",
                email = "locked@example.com",
                passwordHash = "CorrectPass",
                failedAttempts = 3,
                isLocked = true,
                lockedUntil = expiredLockoutTime
            )
        )

        // Login attempt after lockout period expired should auto-unlock and succeed
        val loginResult = repository.loginUser("lockeduser", "CorrectPass")
        assertTrue(loginResult is LoginResult.Success)

        val unlockedUser = userDao.getUserById(userId)!!
        assertFalse(unlockedUser.isLocked)
        assertEquals(0, unlockedUser.failedAttempts)
        assertEquals(0L, unlockedUser.lockedUntil)
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
        assertFalse(DateUtils.isDueSoon(now + (48 * 60 * 60 * 1000), 24))

        assertTrue(DateUtils.isOverdue(pastDueMs))
        assertFalse(DateUtils.isOverdue(futureDueMs))
    }
}
