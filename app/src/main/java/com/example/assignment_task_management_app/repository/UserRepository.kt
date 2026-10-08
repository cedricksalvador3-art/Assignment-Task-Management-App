package com.example.assignment_task_management_app.repository

import com.example.assignment_task_management_app.db.UserDao
import com.example.assignment_task_management_app.model.User

sealed class LoginResult {
    data class Success(val user: User) : LoginResult()
    data class Failed(val message: String, val remainingAttempts: Int) : LoginResult()
    data class Locked(val message: String) : LoginResult()
    data class Error(val message: String) : LoginResult()
}

class UserRepository(private val userDao: UserDao) {

    suspend fun registerUser(user: User): Result<Long> {
        return try {
            val existingUser = userDao.getUserByUsername(user.username)
            if (existingUser != null) {
                return Result.failure(Exception("Username '${user.username}' is already taken."))
            }

            val existingEmail = userDao.getUserByEmail(user.email)
            if (existingEmail != null) {
                return Result.failure(Exception("Email '${user.email}' is already registered."))
            }

            val id = userDao.insertUser(user)
            if (id > 0) {
                Result.success(id)
            } else {
                Result.failure(Exception("Failed to register user."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(usernameOrEmail: String, passwordInput: String): LoginResult {
        return try {
            val user = userDao.getUserByUsername(usernameOrEmail)
                ?: userDao.getUserByEmail(usernameOrEmail)
                ?: return LoginResult.Error("Account not found. Please register.")

            if (user.isLocked) {
                return LoginResult.Locked("Account locked due to 3 failed attempts. Please unlock or contact admin.")
            }

            if (user.passwordHash == passwordInput) {
                // Successful login -> Reset failed attempts
                if (user.failedAttempts > 0) {
                    val updatedUser = user.copy(failedAttempts = 0, isLocked = false)
                    userDao.updateUser(updatedUser)
                    LoginResult.Success(updatedUser)
                } else {
                    LoginResult.Success(user)
                }
            } else {
                // Password incorrect -> Increment failed attempts
                val newFailedCount = user.failedAttempts + 1
                if (newFailedCount >= 3) {
                    val lockedUser = user.copy(failedAttempts = newFailedCount, isLocked = true)
                    userDao.updateUser(lockedUser)
                    LoginResult.Locked("Account has been locked after 3 failed login attempts!")
                } else {
                    val updatedUser = user.copy(failedAttempts = newFailedCount)
                    userDao.updateUser(updatedUser)
                    val remaining = 3 - newFailedCount
                    LoginResult.Failed("Incorrect password. Attempt $newFailedCount of 3.", remaining)
                }
            }
        } catch (e: Exception) {
            LoginResult.Error(e.message ?: "An unexpected error occurred during login.")
        }
    }

    suspend fun unlockUserAccount(usernameOrEmail: String): Boolean {
        val user = userDao.getUserByUsername(usernameOrEmail)
            ?: userDao.getUserByEmail(usernameOrEmail)
            ?: return false

        val unlocked = user.copy(failedAttempts = 0, isLocked = false)
        return userDao.updateUser(unlocked)
    }

    suspend fun getUserById(userId: Long): User? = userDao.getUserById(userId)
}
