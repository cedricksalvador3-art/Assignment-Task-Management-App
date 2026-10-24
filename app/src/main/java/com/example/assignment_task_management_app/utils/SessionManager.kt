package com.example.assignment_task_management_app.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "task_app_user_session"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_USER_ID = "userId"
        private const val KEY_USERNAME = "username"
        private const val KEY_FULL_NAME = "fullName"
        private const val KEY_EMAIL = "email"
        private const val KEY_PROFILE_PIC_PATH = "profilePicPath"

        private const val KEY_NOTIF_PREF = "notifPref"
        private const val KEY_REMINDER_PREF = "reminderPref"
        private const val KEY_ALERT_PREF = "alertPref"
        private const val KEY_THEME_MODE = "themeMode"
        private const val KEY_DEFAULT_PRIORITY = "defaultPriority"
        private const val KEY_DEFAULT_SORT = "defaultSort"
    }

    fun saveProfilePicPath(path: String) {
        val userId = getLoggedInUserId()
        val key = if (userId != -1L) "${KEY_PROFILE_PIC_PATH}_$userId" else KEY_PROFILE_PIC_PATH
        prefs.edit().putString(key, path).putString(KEY_PROFILE_PIC_PATH, path).apply()
    }

    fun getProfilePicPath(): String? {
        val userId = getLoggedInUserId()
        val userKey = if (userId != -1L) "${KEY_PROFILE_PIC_PATH}_$userId" else KEY_PROFILE_PIC_PATH
        return prefs.getString(userKey, null) ?: prefs.getString(KEY_PROFILE_PIC_PATH, null)
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

    fun updateUserInfo(fullName: String, email: String) {
        prefs.edit().apply {
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

    // Notification Toggles
    fun isNotificationsEnabled(): Boolean = prefs.getBoolean(KEY_NOTIF_PREF, true)
    fun setNotificationsEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_NOTIF_PREF, enabled).apply()

    fun isRemindersEnabled(): Boolean = prefs.getBoolean(KEY_REMINDER_PREF, true)
    fun setRemindersEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_REMINDER_PREF, enabled).apply()

    fun isAlertsEnabled(): Boolean = prefs.getBoolean(KEY_ALERT_PREF, true)
    fun setAlertsEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_ALERT_PREF, enabled).apply()

    // Theme Settings
    fun getThemeMode(): Int = prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    fun setThemeMode(mode: Int) = prefs.edit().putInt(KEY_THEME_MODE, mode).apply()

    // Task Preferences
    fun getDefaultPriority(): String = prefs.getString(KEY_DEFAULT_PRIORITY, "Medium") ?: "Medium"
    fun setDefaultPriority(priority: String) = prefs.edit().putString(KEY_DEFAULT_PRIORITY, priority).apply()

    fun getDefaultSort(): String = prefs.getString(KEY_DEFAULT_SORT, "Due Date") ?: "Due Date"
    fun setDefaultSort(sort: String) = prefs.edit().putString(KEY_DEFAULT_SORT, sort).apply()

    fun clearSession() {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_USER_ID)
            remove(KEY_USERNAME)
            remove(KEY_FULL_NAME)
            remove(KEY_EMAIL)
            apply()
        }
    }
}
