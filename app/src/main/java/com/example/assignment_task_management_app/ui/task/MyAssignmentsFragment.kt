package com.example.assignment_task_management_app.ui.task

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.assignment_task_management_app.R
import com.example.assignment_task_management_app.adapter.TaskAdapter
import com.example.assignment_task_management_app.databinding.FragmentMyAssignmentsBinding
import com.example.assignment_task_management_app.db.DatabaseHelper
import com.example.assignment_task_management_app.model.TaskPriority
import com.example.assignment_task_management_app.model.TaskStatus
import com.example.assignment_task_management_app.repository.TaskRepository
import com.example.assignment_task_management_app.utils.SessionManager
import kotlinx.coroutines.launch

class MyAssignmentsFragment : Fragment() {

    private var _binding: FragmentMyAssignmentsBinding? = null
    private val binding get() = _binding!!

    private lateinit var taskRepository: TaskRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var taskAdapter: TaskAdapter
    private var userId: Long = -1L

    private var currentQuery: String = ""
    private var currentStatus: TaskStatus? = null
    private var currentSortBy: String = "DUE_DATE_ASC"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyAssignmentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        sessionManager = SessionManager(context)
        userId = sessionManager.getLoggedInUserId()

        val dbHelper = DatabaseHelper.getInstance(context)
        taskRepository = TaskRepository(dbHelper)

        setupRecyclerView()
        setupSearchAndFilters()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadAssignments()
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            onItemClick = { task ->
                val intent = Intent(requireContext(), TaskDetailActivity::class.java).apply {
                    putExtra("EXTRA_TASK_ID", task.id)
                }
                startActivity(intent)
            }
        )

        binding.rvAssignments.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAssignments.adapter = taskAdapter
    }

    private fun setupSearchAndFilters() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentQuery = s.toString()
                loadAssignments()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.chipGroupStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            currentStatus = if (checkedIds.isEmpty()) {
                null
            } else {
                when (checkedIds.first()) {
                    R.id.chip_pending -> TaskStatus.PENDING
                    R.id.chip_in_progress -> TaskStatus.IN_PROGRESS
                    R.id.chip_completed -> TaskStatus.COMPLETED
                    else -> null
                }
            }
            loadAssignments()
        }

        val sortOptions = listOf("Due Date ↑", "Due Date ↓", "Priority ↓", "Title A-Z")
        val sortAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sortOptions)
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSort.adapter = sortAdapter

        binding.spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentSortBy = when (position) {
                    1 -> "DUE_DATE_DESC"
                    2 -> "PRIORITY_DESC"
                    3 -> "TITLE_ASC"
                    else -> "DUE_DATE_ASC"
                }
                loadAssignments()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupListeners() {
        binding.fabAddAssignment.setOnClickListener {
            startActivity(Intent(requireContext(), AddEditTaskActivity::class.java))
        }
    }

    private fun loadAssignments() {
        lifecycleScope.launch {
            val tasks = taskRepository.searchAndFilterTasks(
                userId = userId,
                query = currentQuery,
                status = currentStatus,
                priority = null,
                subject = null,
                sortBy = currentSortBy
            )

            if (tasks.isEmpty()) {
                binding.rvAssignments.visibility = View.GONE
                binding.tvEmptyAssignments.visibility = View.VISIBLE
            } else {
                binding.rvAssignments.visibility = View.VISIBLE
                binding.tvEmptyAssignments.visibility = View.GONE
                taskAdapter.submitList(tasks)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
