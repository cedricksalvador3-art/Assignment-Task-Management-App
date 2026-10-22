package com.example.assignment_task_management_app.ui.progress

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.assignment_task_management_app.databinding.FragmentProgressMonitoringBinding
import com.example.assignment_task_management_app.db.DatabaseHelper
import com.example.assignment_task_management_app.repository.TaskRepository
import com.example.assignment_task_management_app.utils.SessionManager
import kotlinx.coroutines.launch

class ProgressMonitoringFragment : Fragment() {

    private var _binding: FragmentProgressMonitoringBinding? = null
    private val binding get() = _binding!!

    private lateinit var taskRepository: TaskRepository
    private lateinit var sessionManager: SessionManager
    private var userId: Long = -1L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProgressMonitoringBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        sessionManager = SessionManager(context)
        userId = sessionManager.getLoggedInUserId()

        val dbHelper = DatabaseHelper.getInstance(context)
        taskRepository = TaskRepository(dbHelper)
    }

    override fun onResume() {
        super.onResume()
        loadProgressData()
    }

    private fun loadProgressData() {
        lifecycleScope.launch {
            val stats = taskRepository.getDashboardStats(userId)

            binding.tvLargePercentage.text = "${stats.completionPercentage}%"
            binding.progressLargeCircular.progress = stats.completionPercentage

            // Max scale for progress bars
            val maxCount = maxOf(stats.totalTasks, 1)

            // Tasks by Status
            binding.tvCountCompleted.text = stats.completedTasks.toString()
            binding.pbCompleted.max = maxCount
            binding.pbCompleted.progress = stats.completedTasks

            binding.tvCountPending.text = stats.pendingTasks.toString()
            binding.pbPending.max = maxCount
            binding.pbPending.progress = stats.pendingTasks

            binding.tvCountOverdue.text = stats.overdueTasks.toString()
            binding.pbOverdue.max = maxCount
            binding.pbOverdue.progress = stats.overdueTasks

            // Tasks by Priority
            binding.tvCountHigh.text = stats.highPriorityCount.toString()
            binding.pbHigh.max = maxCount
            binding.pbHigh.progress = stats.highPriorityCount

            binding.tvCountMedium.text = stats.mediumPriorityCount.toString()
            binding.pbMedium.max = maxCount
            binding.pbMedium.progress = stats.mediumPriorityCount

            binding.tvCountLow.text = stats.lowPriorityCount.toString()
            binding.pbLow.max = maxCount
            binding.pbLow.progress = stats.lowPriorityCount
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
