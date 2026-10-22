package com.example.assignment_task_management_app.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.assignment_task_management_app.R
import com.example.assignment_task_management_app.databinding.ActivityMainBinding
import com.example.assignment_task_management_app.ui.auth.LoginActivity
import com.example.assignment_task_management_app.ui.dashboard.DashboardFragment
import com.example.assignment_task_management_app.ui.notifications.NotificationsFragment
import com.example.assignment_task_management_app.ui.profile.ProfileFragment
import com.example.assignment_task_management_app.ui.progress.ProgressMonitoringFragment
import com.example.assignment_task_management_app.ui.task.MyAssignmentsFragment
import com.example.assignment_task_management_app.utils.SessionManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        if (!sessionManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        checkNotificationPermission()
        setupBottomNavigation()

        if (savedInstanceState == null) {
            loadFragment(DashboardFragment())
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    loadFragment(DashboardFragment())
                    true
                }
                R.id.navigation_tasks -> {
                    loadFragment(MyAssignmentsFragment())
                    true
                }
                R.id.navigation_progress -> {
                    loadFragment(ProgressMonitoringFragment())
                    true
                }
                R.id.navigation_notifications -> {
                    loadFragment(NotificationsFragment())
                    true
                }
                R.id.navigation_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    fun navigateToTab(tabIndex: Int) {
        val itemId = when (tabIndex) {
            0 -> R.id.navigation_home
            1 -> R.id.navigation_tasks
            2 -> R.id.navigation_progress
            3 -> R.id.navigation_notifications
            4 -> R.id.navigation_profile
            else -> R.id.navigation_home
        }
        binding.bottomNavigation.selectedItemId = itemId
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
