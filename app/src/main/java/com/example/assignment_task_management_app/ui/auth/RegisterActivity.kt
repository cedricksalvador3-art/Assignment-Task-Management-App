package com.example.assignment_task_management_app.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.assignment_task_management_app.databinding.ActivityRegisterBinding
import com.example.assignment_task_management_app.db.DatabaseHelper
import com.example.assignment_task_management_app.model.User
import com.example.assignment_task_management_app.repository.UserRepository
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val dbHelper = DatabaseHelper.getInstance(this)
        userRepository = UserRepository(dbHelper)

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnRegister.setOnClickListener {
            attemptRegister()
        }

        binding.tvLoginLink.setOnClickListener {
            finish()
        }
    }

    private fun attemptRegister() {
        val firstName = binding.etFirstName.text.toString().trim()
        val middleName = binding.etMiddleName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val username = binding.etRegUsername.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etRegPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        binding.tilFirstName.error = null
        binding.tilMiddleName.error = null
        binding.tilLastName.error = null
        binding.tilRegUsername.error = null
        binding.tilEmail.error = null
        binding.tilRegPassword.error = null
        binding.tilConfirmPassword.error = null
        binding.tvMismatchWarning.visibility = View.GONE

        if (firstName.isEmpty()) {
            binding.tilFirstName.error = "First Name is required"
            return
        }

        if (lastName.isEmpty()) {
            binding.tilLastName.error = "Last Name is required"
            return
        }

        if (username.isEmpty()) {
            binding.tilRegUsername.error = "Username is required"
            return
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Valid email address is required"
            return
        }

        if (password.length < 6) {
            binding.tilRegPassword.error = "Password must be at least 6 characters"
            return
        }

        if (password != confirmPassword) {
            binding.tvMismatchWarning.visibility = View.VISIBLE
            binding.tilConfirmPassword.error = "Passwords do not match"
            return
        }

        val fullName = if (middleName.isNotEmpty()) {
            "$firstName $middleName $lastName"
        } else {
            "$firstName $lastName"
        }

        lifecycleScope.launch {
            binding.btnRegister.isEnabled = false
            val newUser = User(
                username = username,
                fullName = fullName,
                email = email,
                passwordHash = password
            )

            val result = userRepository.registerUser(newUser)
            result.onSuccess {
                Toast.makeText(this@RegisterActivity, "Account registered successfully! Please log in.", Toast.LENGTH_LONG).show()
                finish()
            }.onFailure { ex ->
                binding.btnRegister.isEnabled = true
                Toast.makeText(this@RegisterActivity, ex.message ?: "Registration failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
