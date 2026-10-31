package com.example.assignment_task_management_app.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.assignment_task_management_app.R
import com.example.assignment_task_management_app.adapter.TaskAdapter
import com.example.assignment_task_management_app.databinding.FragmentDashboardBinding
import com.example.assignment_task_management_app.db.DatabaseHelper
import com.example.assignment_task_management_app.model.TaskStatus
import com.example.assignment_task_management_app.repository.TaskRepository
import com.example.assignment_task_management_app.ui.auth.SplashActivity
import com.example.assignment_task_management_app.ui.main.MainActivity
import com.example.assignment_task_management_app.ui.scanner.ScannerActivity
import com.example.assignment_task_management_app.ui.task.AddEditTaskActivity
import com.example.assignment_task_management_app.ui.task.TaskDetailActivity
import com.example.assignment_task_management_app.utils.DateUtils
import com.example.assignment_task_management_app.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var taskRepository: TaskRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var taskAdapter: TaskAdapter

    private var isFabExpanded: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dbHelper = DatabaseHelper.getInstance(requireContext())
        taskRepository = TaskRepository(dbHelper)
        sessionManager = SessionManager(requireContext())

        setupUI()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        collapseFabMenu()
        loadDashboardData()
    }

    private fun setupUI() {
        val dateFormat = SimpleDateFormat("EEEE, MMM dd", Locale.US).apply {
            timeZone = DateUtils.PHILIPPINE_TIME_ZONE
        }
        binding.tvDate.text = dateFormat.format(Date())

        taskAdapter = TaskAdapter(
            onItemClick = { task ->
                val intent = Intent(requireContext(), TaskDetailActivity::class.java).apply {
                    putExtra("EXTRA_TASK_ID", task.id)
                }
                startActivity(intent)
            }
        )

        binding.rvUpcomingTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUpcomingTasks.adapter = taskAdapter
    }

    private fun setupListeners() {
        binding.btnHeaderAdd.setOnClickListener {
            startActivity(Intent(requireContext(), AddEditTaskActivity::class.java))
        }

        binding.fabDashAddTask.setOnClickListener {
            toggleFabMenu()
        }

        binding.btnFabCreateTask.setOnClickListener {
            collapseFabMenu()
            startActivity(Intent(requireContext(), AddEditTaskActivity::class.java))
        }

        binding.btnFabScanner.setOnClickListener {
            collapseFabMenu()
            startActivity(Intent(requireContext(), ScannerActivity::class.java))
        }

        binding.tvSeeAll.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(1)
        }

        binding.btnHeaderNotifications.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(3)
        }

        binding.btnHeaderSettings.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(4)
        }
    }

    private fun toggleFabMenu() {
        isFabExpanded = !isFabExpanded
        if (isFabExpanded) {
            binding.layoutFabMenu.visibility = View.VISIBLE
            binding.fabDashAddTask.setImageResource(R.drawable.ic_close)
        } else {
            collapseFabMenu()
        }
    }

    private fun collapseFabMenu() {
        isFabExpanded = false
        if (_binding != null) {
            binding.layoutFabMenu.visibility = View.GONE
            binding.fabDashAddTask.setImageResource(R.drawable.ic_add)
        }
    }

    private fun loadDashboardData() {
        val userId = sessionManager.getLoggedInUserId()
        if (userId == -1L) {
            startActivity(Intent(requireContext(), SplashActivity::class.java))
            activity?.finish()
            return
        }

        lifecycleScope.launch {
            val stats = taskRepository.getDashboardStats(userId)

            binding.tvDashTotal.text = stats.totalTasks.toString()
            binding.tvDashPending.text = stats.pendingTasks.toString()
            binding.tvDashCompleted.text = stats.completedTasks.toString()
            binding.tvDashOverdue.text = stats.overdueTasks.toString()

            val progressPercent = stats.completionPercentage
            binding.progressCircular.progress = progressPercent
            binding.tvDashPercentage.text = "$progressPercent%"

            val allUserTasks = taskRepository.getTasksForUser(userId)
            val upcoming = allUserTasks.filter { it.status != TaskStatus.COMPLETED }.sortedBy { it.dueDate }

            if (upcoming.isNotEmpty()) {
                taskAdapter.submitList(upcoming.take(3))
                binding.rvUpcomingTasks.visibility = View.VISIBLE
                binding.tvNoUpcomingTasks.visibility = View.GONE

                val firstUpcoming = upcoming.first()
                val remainingStr = DateUtils.getRemainingTimeString(firstUpcoming.dueDate)
                binding.tvDashProgressSub.text = "Next: ${firstUpcoming.title} ($remainingStr)"
            } else {
                taskAdapter.submitList(emptyList())
                binding.rvUpcomingTasks.visibility = View.GONE
                binding.tvNoUpcomingTasks.visibility = View.VISIBLE
                binding.tvDashProgressSub.text = getString(R.string.no_upcoming_deadlines)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
