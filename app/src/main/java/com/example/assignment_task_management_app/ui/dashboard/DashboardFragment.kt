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
import com.example.assignment_task_management_app.repository.DashboardStats
import com.example.assignment_task_management_app.repository.TaskRepository
import com.example.assignment_task_management_app.ui.main.MainActivity
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
    private var userId: Long = -1L

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

        val context = requireContext()
        sessionManager = SessionManager(context)
        userId = sessionManager.getLoggedInUserId()

        val dbHelper = DatabaseHelper.getInstance(context)
        taskRepository = TaskRepository(dbHelper)

        setupUI()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData()
    }

    private fun setupUI() {
        val name = sessionManager.getLoggedInFullName() ?: "User"
        binding.tvGreeting.text = getString(R.string.good_morning, name)

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
            startActivity(Intent(requireContext(), AddEditTaskActivity::class.java))
        }

        binding.tvSeeAll.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(1) // Tab 1 = My Tasks
        }

        binding.btnHeaderNotifications.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(3) // Tab 3 = Notifications
        }

        binding.btnHeaderSettings.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(4) // Tab 4 = Profile
        }
    }

    private fun loadDashboardData() {
        lifecycleScope.launch {
            val stats = taskRepository.getDashboardStats(userId)
            binding.tvDashTotal.text = stats.totalTasks.toString()
            binding.tvDashPending.text = stats.pendingTasks.toString()
            binding.tvDashCompleted.text = stats.completedTasks.toString()
            binding.tvDashOverdue.text = stats.overdueTasks.toString()

            binding.tvDashPercentage.text = "${stats.completionPercentage}%"
            binding.progressCircular.progress = stats.completionPercentage

            if (stats.dueSoonTasksCount() > 0) {
                binding.tvDashProgressSub.text = "${stats.dueSoonTasksCount()} upcoming task(s) due soon"
            } else {
                binding.tvDashProgressSub.text = "No upcoming deadlines"
            }

            val dueSoonTasks = taskRepository.getDueSoonTasks(userId)
            if (dueSoonTasks.isNotEmpty()) {
                binding.rvUpcomingTasks.visibility = View.VISIBLE
                binding.tvNoUpcomingTasks.visibility = View.GONE
                taskAdapter.submitList(dueSoonTasks.take(5))
            } else {
                binding.rvUpcomingTasks.visibility = View.GONE
                binding.tvNoUpcomingTasks.visibility = View.VISIBLE
            }
        }
    }

    private fun DashboardStats.dueSoonTasksCount(): Int {
        return pendingTasks
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
