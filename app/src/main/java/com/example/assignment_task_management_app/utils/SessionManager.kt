package com.example.assignment_task_management_app.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "task_app_user_session"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_USER_ID = "userId"
        private const val KEY_USERNAME = "username"
        private const val KEY_FULL_NAME = "fullName"
        private const val KEY_EMAIL = "email"
    }

    fun saveUserSession(userId: Long, username: String, fullName: String, email: String) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putLong(KEY_USER_ID, userId)
            putString(KEY_USERNAME, username)
            putString(KEY_FULL_NAME, fullName)
            putString(KEY_EMAIL, email)
            apply()
        }
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun getLoggedInUserId(): Long = prefs.getLong(KEY_USER_ID, -1L)

    fun getLoggedInUsername(): String? = prefs.getString(KEY_USERNAME, null)

    fun getLoggedInFullName(): String? = prefs.getString(KEY_FULL_NAME, "User")

    fun getLoggedInEmail(): String? = prefs.getString(KEY_EMAIL, null)

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
