package com.example.assignment_task_management_app.repository

import com.example.assignment_task_management_app.db.UserDao
import com.example.assignment_task_management_app.model.User

sealed class LoginResult {
    data class Success(val user: User) : LoginResult()
    data class Failed(val message: String, val remainingAttempts: Int) : LoginResult()
    data class Locked(val message: String, val lockedUntil: Long = 0L) : LoginResult()
    data class Error(val message: String) : LoginResult()
}

class UserRepository(private val userDao: UserDao) {

    companion object {
        const val LOCKOUT_DURATION_MS = 5 * 60 * 1000L // 5 minutes temporary lockout
        const val MAX_FAILED_ATTEMPTS = 3
    }

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
            // 1. Validate if identifier matches an existing account in database
            val existingUser = userDao.getUserByUsername(usernameOrEmail)
                ?: userDao.getUserByEmail(usernameOrEmail)
                ?: return LoginResult.Error("Account not found. Please register.")

            val currentTime = System.currentTimeMillis()
            var user = existingUser

            // 2. Check if account is currently locked or lockout period is active
            if (user.isLocked || user.lockedUntil > currentTime) {
                if (user.lockedUntil > currentTime) {
                    // Lockout period is active and has NOT expired yet
                    return LoginResult.Locked(
                        "Account temporarily locked in DueWise. Wait 5 minutes before trying again.",
                        user.lockedUntil
                    )
                } else {
                    // Lockout period HAS expired! Automatically reset counter & unlock account
                    user = user.copy(failedAttempts = 0, isLocked = false, lockedUntil = 0L)
                    userDao.updateUser(user)
                }
            }

            // 3. Verify password credentials for the registered account
            if (user.passwordHash == passwordInput) {
                // Successful login -> Reset failed attempts counter to 0
                if (user.failedAttempts > 0 || user.isLocked || user.lockedUntil > 0L) {
                    user = user.copy(failedAttempts = 0, isLocked = false, lockedUntil = 0L)
                    userDao.updateUser(user)
                }
                LoginResult.Success(user)
            } else {
                // Incorrect password for valid registered account -> Increment failed attempts
                val newFailedCount = user.failedAttempts + 1
                if (newFailedCount >= MAX_FAILED_ATTEMPTS) {
                    val lockedUntil = System.currentTimeMillis() + LOCKOUT_DURATION_MS
                    val lockedUser = user.copy(
                        failedAttempts = newFailedCount,
                        isLocked = true,
                        lockedUntil = lockedUntil
                    )
                    userDao.updateUser(lockedUser)
                    LoginResult.Locked(
                        "Account temporarily locked in DueWise. Wait 5 minutes before trying again.",
                        lockedUntil
                    )
                } else {
                    val updatedUser = user.copy(failedAttempts = newFailedCount)
                    userDao.updateUser(updatedUser)
                    val remaining = MAX_FAILED_ATTEMPTS - newFailedCount
                    LoginResult.Failed("Incorrect password. Attempt $newFailedCount of $MAX_FAILED_ATTEMPTS.", remaining)
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

        val unlocked = user.copy(failedAttempts = 0, isLocked = false, lockedUntil = 0L)
        return userDao.updateUser(unlocked)
    }

    suspend fun getUserById(userId: Long): User? = userDao.getUserById(userId)

    suspend fun updateUserProfile(userId: Long, fullName: String, email: String): Result<Boolean> {
        return try {
            val user = userDao.getUserById(userId)
                ?: return Result.failure(Exception("User not found."))

            if (email != user.email) {
                val existingEmailUser = userDao.getUserByEmail(email)
                if (existingEmailUser != null && existingEmailUser.id != userId) {
                    return Result.failure(Exception("Email '$email' is already in use."))
                }
            }

            val updated = user.copy(fullName = fullName, email = email)
            val success = userDao.updateUser(updated)
            if (success) Result.success(true) else Result.failure(Exception("Failed to update profile."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun changePassword(userId: Long, currentPass: String, newPass: String): Result<Boolean> {
        return try {
            val user = userDao.getUserById(userId)
                ?: return Result.failure(Exception("User not found."))

            if (user.passwordHash != currentPass) {
                return Result.failure(Exception("Current password is incorrect."))
            }

            val updated = user.copy(passwordHash = newPass)
            val success = userDao.updateUser(updated)
            if (success) Result.success(true) else Result.failure(Exception("Failed to update password."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPasswordByEmailOrUsername(identifier: String, newPass: String): Result<Boolean> {
        return try {
            val user = userDao.getUserByUsername(identifier)
                ?: userDao.getUserByEmail(identifier)
                ?: return Result.failure(Exception("No registered account found with that email or username."))

            val updated = user.copy(passwordHash = newPass, failedAttempts = 0, isLocked = false, lockedUntil = 0L)
            val success = userDao.updateUser(updated)
            if (success) Result.success(true) else Result.failure(Exception("Failed to reset password."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
