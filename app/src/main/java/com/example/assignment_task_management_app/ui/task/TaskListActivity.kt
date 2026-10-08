package com.example.assignment_task_management_app.ui.task

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.assignment_task_management_app.adapter.TaskAdapter
import com.example.assignment_task_management_app.databinding.ActivityTaskListBinding
import com.example.assignment_task_management_app.db.DatabaseHelper
import com.example.assignment_task_management_app.model.Task
import com.example.assignment_task_management_app.model.TaskPriority
import com.example.assignment_task_management_app.model.TaskStatus
import com.example.assignment_task_management_app.repository.TaskRepository
import com.example.assignment_task_management_app.utils.AlarmScheduler
import com.example.assignment_task_management_app.utils.SessionManager
import kotlinx.coroutines.launch

class TaskListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskListBinding
    private lateinit var taskRepository: TaskRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var taskAdapter: TaskAdapter
    private var userId: Long = -1L

    private var currentSearchQuery: String = ""
    private var currentStatusFilter: TaskStatus? = null
    private var currentPriorityFilter: TaskPriority? = null
    private var currentSubjectFilter: String = "All"
    private var currentSortBy: String = "DUE_DATE_ASC"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        userId = sessionManager.getLoggedInUserId()

        val dbHelper = DatabaseHelper.getInstance(this)
        taskRepository = TaskRepository(dbHelper)

        setupToolbar()
        setupRecyclerView()
        setupSearchAndFilters()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadSubjectsAndTasks()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarTaskList)
        binding.toolbarTaskList.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            onItemClick = { task ->
                val intent = Intent(this, TaskDetailActivity::class.java).apply {
                    putExtra("EXTRA_TASK_ID", task.id)
                }
                startActivity(intent)
            },
            onMarkCompleteClick = { task ->
                markTaskCompleted(task)
            },
            onEditClick = { task ->
                val intent = Intent(this, AddEditTaskActivity::class.java).apply {
                    putExtra("EXTRA_TASK_ID", task.id)
                }
                startActivity(intent)
            },
            onDeleteClick = { task ->
                confirmDeleteTask(task)
            }
        )

        binding.rvTasks.layoutManager = LinearLayoutManager(this)
        binding.rvTasks.adapter = taskAdapter
    }

    private fun setupSearchAndFilters() {
        // Search text listener
        binding.etSearchTasks.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s.toString()
                applyFiltersAndLoad()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Status Spinner Adapter
        val statusOptions = listOf("All Statuses", "Pending", "In Progress", "Completed")
        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statusOptions)
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStatusFilter.adapter = statusAdapter

        binding.spinnerStatusFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentStatusFilter = when (position) {
                    1 -> TaskStatus.PENDING
                    2 -> TaskStatus.IN_PROGRESS
                    3 -> TaskStatus.COMPLETED
                    else -> null
                }
                applyFiltersAndLoad()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Priority Spinner Adapter
        val priorityOptions = listOf("All Priorities", "High", "Medium", "Low")
        val priorityAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, priorityOptions)
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPriorityFilter.adapter = priorityAdapter

        binding.spinnerPriorityFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentPriorityFilter = when (position) {
                    1 -> TaskPriority.HIGH
                    2 -> TaskPriority.MEDIUM
                    3 -> TaskPriority.LOW
                    else -> null
                }
                applyFiltersAndLoad()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Sort Spinner Adapter
        val sortOptions = listOf("Due Date ↑", "Due Date ↓", "Priority ↓", "Title A-Z")
        val sortAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sortOptions)
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSortBy.adapter = sortAdapter

        binding.spinnerSortBy.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentSortBy = when (position) {
                    1 -> "DUE_DATE_DESC"
                    2 -> "PRIORITY_DESC"
                    3 -> "TITLE_ASC"
                    else -> "DUE_DATE_ASC"
                }
                applyFiltersAndLoad()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadSubjectsAndTasks() {
        lifecycleScope.launch {
            val subjects = taskRepository.getSubjectsForUser(userId).toMutableList()
            subjects.add(0, "All Subjects")

            val subjectAdapter = ArrayAdapter(this@TaskListActivity, android.R.layout.simple_spinner_item, subjects)
            subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerSubjectFilter.adapter = subjectAdapter

            binding.spinnerSubjectFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    currentSubjectFilter = if (position == 0) "All" else subjects[position]
                    applyFiltersAndLoad()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            applyFiltersAndLoad()
        }
    }

    private fun applyFiltersAndLoad() {
        lifecycleScope.launch {
            val tasks = taskRepository.searchAndFilterTasks(
                userId = userId,
                query = currentSearchQuery,
                status = currentStatusFilter,
                priority = currentPriorityFilter,
                subject = currentSubjectFilter,
                sortBy = currentSortBy
            )

            if (tasks.isEmpty()) {
                binding.rvTasks.visibility = View.GONE
                binding.tvEmptyTaskList.visibility = View.VISIBLE
            } else {
                binding.rvTasks.visibility = View.VISIBLE
                binding.tvEmptyTaskList.visibility = View.GONE
                taskAdapter.submitList(tasks)
            }
        }
    }

    private fun setupListeners() {
        binding.fabAddTask.setOnClickListener {
            startActivity(Intent(this, AddEditTaskActivity::class.java))
        }
    }

    private fun markTaskCompleted(task: Task) {
        lifecycleScope.launch {
            val success = taskRepository.updateTaskStatus(task.id, TaskStatus.COMPLETED)
            if (success) {
                AlarmScheduler.cancelTaskReminder(this@TaskListActivity, task.id)
                Toast.makeText(this@TaskListActivity, "Task '${task.title}' completed!", Toast.LENGTH_SHORT).show()
                applyFiltersAndLoad()
            }
        }
    }

    private fun confirmDeleteTask(task: Task) {
        AlertDialog.Builder(this)
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete '${task.title}'? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    val deleted = taskRepository.deleteTask(task.id)
                    if (deleted) {
                        AlarmScheduler.cancelTaskReminder(this@TaskListActivity, task.id)
                        Toast.makeText(this@TaskListActivity, "Task deleted", Toast.LENGTH_SHORT).show()
                        applyFiltersAndLoad()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
