package com.example.assignment_task_management_app.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.assignment_task_management_app.model.Task
import com.example.assignment_task_management_app.model.TaskPriority
import com.example.assignment_task_management_app.model.TaskStatus
import com.example.assignment_task_management_app.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION), UserDao, TaskDao {

    companion object {
        private const val DATABASE_NAME = "assignment_task_app.db"
        private const val DATABASE_VERSION = 1

        // Table Users
        private const val TABLE_USERS = "users"
        private const val KEY_USER_ID = "id"
        private const val KEY_USERNAME = "username"
        private const val KEY_FULL_NAME = "fullName"
        private const val KEY_EMAIL = "email"
        private const val KEY_PASSWORD = "passwordHash"
        private const val KEY_FAILED_ATTEMPTS = "failedAttempts"
        private const val KEY_IS_LOCKED = "isLocked"

        // Table Tasks
        private const val TABLE_TASKS = "tasks"
        private const val KEY_TASK_ID = "id"
        private const val KEY_TASK_USER_ID = "userId"
        private const val KEY_TITLE = "title"
        private const val KEY_DESCRIPTION = "description"
        private const val KEY_SUBJECT = "subject"
        private const val KEY_DUE_DATE = "dueDate"
        private const val KEY_PRIORITY = "priority"
        private const val KEY_STATUS = "status"
        private const val KEY_IS_REMINDER = "isReminderSet"
        private const val KEY_CREATED_AT = "createdAt"

        @Volatile
        private var instance: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createUsersTable = """
            CREATE TABLE $TABLE_USERS (
                $KEY_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_USERNAME TEXT UNIQUE NOT NULL,
                $KEY_FULL_NAME TEXT NOT NULL,
                $KEY_EMAIL TEXT NOT NULL,
                $KEY_PASSWORD TEXT NOT NULL,
                $KEY_FAILED_ATTEMPTS INTEGER DEFAULT 0,
                $KEY_IS_LOCKED INTEGER DEFAULT 0
            )
        """.trimIndent()

        val createTasksTable = """
            CREATE TABLE $TABLE_TASKS (
                $KEY_TASK_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_TASK_USER_ID INTEGER NOT NULL,
                $KEY_TITLE TEXT NOT NULL,
                $KEY_DESCRIPTION TEXT,
                $KEY_SUBJECT TEXT NOT NULL,
                $KEY_DUE_DATE INTEGER NOT NULL,
                $KEY_PRIORITY TEXT NOT NULL,
                $KEY_STATUS TEXT NOT NULL,
                $KEY_IS_REMINDER INTEGER DEFAULT 1,
                $KEY_CREATED_AT INTEGER NOT NULL,
                FOREIGN KEY($KEY_TASK_USER_ID) REFERENCES $TABLE_USERS($KEY_USER_ID) ON DELETE CASCADE
            )
        """.trimIndent()

        db.execSQL(createUsersTable)
        db.execSQL(createTasksTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TASKS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    // ==================== USER DAO IMPLEMENTATION ====================

    override suspend fun getUserByUsername(username: String): User? = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS, null,
            "$KEY_USERNAME = ?", arrayOf(username.trim()),
            null, null, null, "1"
        )
        cursor.use { if (it.moveToFirst()) cursorToUser(it) else null }
    }

    override suspend fun getUserByEmail(email: String): User? = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS, null,
            "$KEY_EMAIL = ?", arrayOf(email.trim()),
            null, null, null, "1"
        )
        cursor.use { if (it.moveToFirst()) cursorToUser(it) else null }
    }

    override suspend fun getUserById(id: Long): User? = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS, null,
            "$KEY_USER_ID = ?", arrayOf(id.toString()),
            null, null, null, "1"
        )
        cursor.use { if (it.moveToFirst()) cursorToUser(it) else null }
    }

    override suspend fun insertUser(user: User): Long = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_USERNAME, user.username.trim())
            put(KEY_FULL_NAME, user.fullName.trim())
            put(KEY_EMAIL, user.email.trim())
            put(KEY_PASSWORD, user.passwordHash)
            put(KEY_FAILED_ATTEMPTS, user.failedAttempts)
            put(KEY_IS_LOCKED, if (user.isLocked) 1 else 0)
        }
        db.insert(TABLE_USERS, null, values)
    }

    override suspend fun updateUser(user: User): Boolean = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_USERNAME, user.username.trim())
            put(KEY_FULL_NAME, user.fullName.trim())
            put(KEY_EMAIL, user.email.trim())
            put(KEY_PASSWORD, user.passwordHash)
            put(KEY_FAILED_ATTEMPTS, user.failedAttempts)
            put(KEY_IS_LOCKED, if (user.isLocked) 1 else 0)
        }
        val rows = db.update(TABLE_USERS, values, "$KEY_USER_ID = ?", arrayOf(user.id.toString()))
        rows > 0
    }

    private fun cursorToUser(cursor: Cursor): User {
        return User(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_USER_ID)),
            username = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USERNAME)),
            fullName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FULL_NAME)),
            email = cursor.getString(cursor.getColumnIndexOrThrow(KEY_EMAIL)),
            passwordHash = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PASSWORD)),
            failedAttempts = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_FAILED_ATTEMPTS)),
            isLocked = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_LOCKED)) == 1
        )
    }

    // ==================== TASK DAO IMPLEMENTATION ====================

    override suspend fun getTasksForUser(userId: Long): List<Task> = withContext(Dispatchers.IO) {
        val tasks = mutableListOf<Task>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TASKS, null,
            "$KEY_TASK_USER_ID = ?", arrayOf(userId.toString()),
            null, null, "$KEY_DUE_DATE ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                tasks.add(cursorToTask(it))
            }
        }
        tasks
    }

    override suspend fun getTaskById(taskId: Long): Task? = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TASKS, null,
            "$KEY_TASK_ID = ?", arrayOf(taskId.toString()),
            null, null, null, "1"
        )
        cursor.use { if (it.moveToFirst()) cursorToTask(it) else null }
    }

    override suspend fun insertTask(task: Task): Long = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_TASK_USER_ID, task.userId)
            put(KEY_TITLE, task.title.trim())
            put(KEY_DESCRIPTION, task.description.trim())
            put(KEY_SUBJECT, task.subject.trim())
            put(KEY_DUE_DATE, task.dueDate)
            put(KEY_PRIORITY, task.priority.name)
            put(KEY_STATUS, task.status.name)
            put(KEY_IS_REMINDER, if (task.isReminderSet) 1 else 0)
            put(KEY_CREATED_AT, task.createdAt)
        }
        db.insert(TABLE_TASKS, null, values)
    }

    override suspend fun updateTask(task: Task): Boolean = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_TITLE, task.title.trim())
            put(KEY_DESCRIPTION, task.description.trim())
            put(KEY_SUBJECT, task.subject.trim())
            put(KEY_DUE_DATE, task.dueDate)
            put(KEY_PRIORITY, task.priority.name)
            put(KEY_STATUS, task.status.name)
            put(KEY_IS_REMINDER, if (task.isReminderSet) 1 else 0)
        }
        val rows = db.update(TABLE_TASKS, values, "$KEY_TASK_ID = ?", arrayOf(task.id.toString()))
        rows > 0
    }

    override suspend fun deleteTask(task: Task): Boolean = deleteTaskById(task.id)

    override suspend fun deleteTaskById(taskId: Long): Boolean = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val rows = db.delete(TABLE_TASKS, "$KEY_TASK_ID = ?", arrayOf(taskId.toString()))
        rows > 0
    }

    override suspend fun updateTaskStatus(taskId: Long, status: TaskStatus): Boolean = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_STATUS, status.name)
        }
        val rows = db.update(TABLE_TASKS, values, "$KEY_TASK_ID = ?", arrayOf(taskId.toString()))
        rows > 0
    }

    override suspend fun searchAndFilterTasks(
        userId: Long,
        query: String,
        status: TaskStatus?,
        priority: TaskPriority?,
        subject: String?,
        sortBy: String
    ): List<Task> = withContext(Dispatchers.IO) {
        val tasks = mutableListOf<Task>()
        val db = readableDatabase

        val selectionList = mutableListOf("$KEY_TASK_USER_ID = ?")
        val selectionArgs = mutableListOf(userId.toString())

        if (query.isNotBlank()) {
            selectionList.add("($KEY_TITLE LIKE ? OR $KEY_DESCRIPTION LIKE ? OR $KEY_SUBJECT LIKE ?)")
            val q = "%${query.trim()}%"
            selectionArgs.add(q)
            selectionArgs.add(q)
            selectionArgs.add(q)
        }

        if (status != null) {
            selectionList.add("$KEY_STATUS = ?")
            selectionArgs.add(status.name)
        }

        if (priority != null) {
            selectionList.add("$KEY_PRIORITY = ?")
            selectionArgs.add(priority.name)
        }

        if (!subject.isNullOrBlank() && !subject.equals("All", ignoreCase = true)) {
            selectionList.add("$KEY_SUBJECT = ?")
            selectionArgs.add(subject.trim())
        }

        val orderBy = when (sortBy) {
            "DUE_DATE_DESC" -> "$KEY_DUE_DATE DESC"
            "PRIORITY_DESC" -> "CASE $KEY_PRIORITY WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 WHEN 'LOW' THEN 3 END ASC"
            "TITLE_ASC" -> "$KEY_TITLE ASC"
            else -> "$KEY_DUE_DATE ASC"
        }

        val selection = selectionList.joinToString(" AND ")
        val cursor = db.query(
            TABLE_TASKS, null,
            selection, selectionArgs.toTypedArray(),
            null, null, orderBy
        )

        cursor.use {
            while (it.moveToNext()) {
                tasks.add(cursorToTask(it))
            }
        }
        tasks
    }

    override suspend fun getSubjectsForUser(userId: Long): List<String> = withContext(Dispatchers.IO) {
        val subjects = mutableListOf<String>()
        val db = readableDatabase
        val cursor = db.query(
            true,
            TABLE_TASKS,
            arrayOf(KEY_SUBJECT),
            "$KEY_TASK_USER_ID = ?",
            arrayOf(userId.toString()),
            null, null, "$KEY_SUBJECT ASC", null
        )
        cursor.use {
            while (it.moveToNext()) {
                val sub = it.getString(it.getColumnIndexOrThrow(KEY_SUBJECT))
                if (sub.isNotBlank()) subjects.add(sub)
            }
        }
        subjects
    }

    override suspend fun getTasksDueBetween(userId: Long, startMs: Long, endMs: Long): List<Task> = withContext(Dispatchers.IO) {
        val tasks = mutableListOf<Task>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TASKS, null,
            "$KEY_TASK_USER_ID = ? AND $KEY_DUE_DATE BETWEEN ? AND ? AND $KEY_STATUS != ?",
            arrayOf(userId.toString(), startMs.toString(), endMs.toString(), TaskStatus.COMPLETED.name),
            null, null, "$KEY_DUE_DATE ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                tasks.add(cursorToTask(it))
            }
        }
        tasks
    }

    private fun cursorToTask(cursor: Cursor): Task {
        return Task(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TASK_ID)),
            userId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TASK_USER_ID)),
            title = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TITLE)),
            description = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DESCRIPTION)),
            subject = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SUBJECT)),
            dueDate = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_DUE_DATE)),
            priority = TaskPriority.fromString(cursor.getString(cursor.getColumnIndexOrThrow(KEY_PRIORITY))),
            status = TaskStatus.fromString(cursor.getString(cursor.getColumnIndexOrThrow(KEY_STATUS))),
            isReminderSet = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_REMINDER)) == 1,
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_CREATED_AT))
        )
    }
}
