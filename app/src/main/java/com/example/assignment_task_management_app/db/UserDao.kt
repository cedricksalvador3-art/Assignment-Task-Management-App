package com.example.assignment_task_management_app.db

import com.example.assignment_task_management_app.model.User

interface UserDao {
    suspend fun getUserByUsername(username: String): User?
    suspend fun getUserByEmail(email: String): User?
    suspend fun getUserById(id: Long): User?
    suspend fun insertUser(user: User): Long
    suspend fun updateUser(user: User): Boolean
}
