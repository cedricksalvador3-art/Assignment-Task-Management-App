package com.example.assignment_task_management_app.ui.notifications

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.assignment_task_management_app.adapter.NotificationAdapter
import com.example.assignment_task_management_app.adapter.NotificationItem
import com.example.assignment_task_management_app.databinding.FragmentNotificationsBinding
import com.example.assignment_task_management_app.db.DatabaseHelper
import com.example.assignment_task_management_app.model.TaskStatus
import com.example.assignment_task_management_app.repository.TaskRepository
import com.example.assignment_task_management_app.ui.task.TaskDetailActivity
import com.example.assignment_task_management_app.utils.DateUtils
import com.example.assignment_task_management_app.utils.SessionManager
import kotlinx.coroutines.launch

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var taskRepository: TaskRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var notificationAdapter: NotificationAdapter
    private var userId: Long = -1L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
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
    }

    override fun onResume() {
        super.onResume()
        loadNotifications()
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter(
            onItemClick = { item ->
                val intent = Intent(requireContext(), TaskDetailActivity::class.java).apply {
                    putExtra("EXTRA_TASK_ID", item.taskId)
                }
                startActivity(intent)
            }
        )

        binding.rvNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotifications.adapter = notificationAdapter
    }

    private fun loadNotifications() {
        lifecycleScope.launch {
            val allTasks = taskRepository.getTasksForUser(userId)
            val notificationList = mutableListOf<NotificationItem>()

            var notifId = 1L
            for (task in allTasks) {
                if (task.status == TaskStatus.COMPLETED) continue

                val isOverdue = DateUtils.isOverdue(task.dueDate)
                val msg = if (isOverdue) {
                    "Task Overdue: ${task.title}"
                } else {
                    "Upcoming: ${task.title} - Due ${DateUtils.formatDateTime(task.dueDate)}"
                }

                notificationList.add(
                    NotificationItem(
                        id = notifId++,
                        taskId = task.id,
                        title = task.title,
                        message = msg,
                        timeAgo = DateUtils.getRemainingTimeString(task.dueDate),
                        isOverdue = isOverdue
                    )
                )
            }

            if (notificationList.isEmpty()) {
                binding.rvNotifications.visibility = View.GONE
                binding.tvEmptyNotifications.visibility = View.VISIBLE
            } else {
                binding.rvNotifications.visibility = View.VISIBLE
                binding.tvEmptyNotifications.visibility = View.GONE
                notificationAdapter.submitList(notificationList)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
