package com.example.assignment_task_management_app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.assignment_task_management_app.databinding.ActivityLoginBinding
import com.example.assignment_task_management_app.db.DatabaseHelper
import com.example.assignment_task_management_app.repository.LoginResult
import com.example.assignment_task_management_app.repository.UserRepository
import com.example.assignment_task_management_app.ui.dashboard.DashboardActivity
import com.example.assignment_task_management_app.utils.SessionManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var userRepository: UserRepository
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val dbHelper = DatabaseHelper.getInstance(this)
        userRepository = UserRepository(dbHelper)
        sessionManager = SessionManager(this)

        // Check if user is already logged in
        if (sessionManager.isLoggedIn()) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
            return
        }

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            attemptLogin()
        }

        binding.btnUnlockAccount.setOnClickListener {
            unlockAccount()
        }

        binding.tvRegisterLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun attemptLogin() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        binding.tilUsername.error = null
        binding.tilPassword.error = null
        binding.tvAttemptWarning.visibility = View.GONE

        if (username.isEmpty()) {
            binding.tilUsername.error = "Username or email is required"
            return
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            return
        }

        lifecycleScope.launch {
            binding.btnLogin.isEnabled = false
            when (val result = userRepository.loginUser(username, password)) {
                is LoginResult.Success -> {
                    val user = result.user
                    sessionManager.saveUserSession(
                        userId = user.id,
                        username = user.username,
                        fullName = user.fullName,
                        email = user.email
                    )
                    Toast.makeText(this@LoginActivity, "Welcome, ${user.fullName}!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                    finish()
                }
                is LoginResult.Failed -> {
                    binding.btnLogin.isEnabled = true
                    binding.tvAttemptWarning.text = result.message
                    binding.tvAttemptWarning.visibility = View.VISIBLE
                    binding.tilPassword.error = "Incorrect password"
                }
                is LoginResult.Locked -> {
                    binding.btnLogin.isEnabled = false
                    binding.cardLockWarning.visibility = View.VISIBLE
                    binding.tvLockMessage.text = result.message
                    binding.tvAttemptWarning.visibility = View.GONE
                    Toast.makeText(this@LoginActivity, result.message, Toast.LENGTH_LONG).show()
                }
                is LoginResult.Error -> {
                    binding.btnLogin.isEnabled = true
                    Toast.makeText(this@LoginActivity, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun unlockAccount() {
        val username = binding.etUsername.text.toString().trim()
        if (username.isEmpty()) {
            Toast.makeText(this, "Enter your username or email above to unlock", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val success = userRepository.unlockUserAccount(username)
            if (success) {
                binding.cardLockWarning.visibility = View.GONE
                binding.btnLogin.isEnabled = true
                binding.tvAttemptWarning.visibility = View.GONE
                Toast.makeText(this@LoginActivity, "Account unlocked successfully! You may now log in.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this@LoginActivity, "Account not found for '$username'", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
