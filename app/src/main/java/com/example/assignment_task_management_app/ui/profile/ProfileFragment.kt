package com.example.assignment_task_management_app.ui.profile

import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.assignment_task_management_app.R
import com.example.assignment_task_management_app.databinding.DialogAboutAppBinding
import com.example.assignment_task_management_app.databinding.DialogChangePasswordBinding
import com.example.assignment_task_management_app.databinding.DialogEditProfileBinding
import com.example.assignment_task_management_app.databinding.FragmentProfileBinding
import com.example.assignment_task_management_app.db.DatabaseHelper
import com.example.assignment_task_management_app.repository.UserRepository
import com.example.assignment_task_management_app.ui.auth.LoginActivity
import com.example.assignment_task_management_app.utils.SessionManager
import kotlinx.coroutines.launch
import java.io.File

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var userRepository: UserRepository

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { sourceUri ->
            try {
                val userId = sessionManager.getLoggedInUserId()
                val profileFile = File(requireContext().filesDir, "profile_avatar_$userId.jpg")

                requireContext().contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    profileFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                sessionManager.saveProfilePicPath(profileFile.absolutePath)
                loadProfilePicture()
                Toast.makeText(requireContext(), "Profile picture updated successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to update profile picture: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        val dbHelper = DatabaseHelper.getInstance(requireContext())
        userRepository = UserRepository(dbHelper)

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        val fullName = sessionManager.getLoggedInFullName() ?: "DueWise User"
        val email = sessionManager.getLoggedInEmail() ?: "user@email.com"
        val username = sessionManager.getLoggedInUsername() ?: "user"

        binding.tvProfileName.text = fullName
        binding.tvProfileEmail.text = email
        binding.tvProfileUsername.text = "@$username"

        loadProfilePicture()
        setupSavedPreferences()
    }

    private fun setupSavedPreferences() {
        // Notification Switches
        binding.switchNotifications.isChecked = sessionManager.isNotificationsEnabled()
        binding.switchReminders.isChecked = sessionManager.isRemindersEnabled()
        binding.switchAlerts.isChecked = sessionManager.isAlertsEnabled()
    }

    private fun loadProfilePicture() {
        val userId = sessionManager.getLoggedInUserId()
        val userAvatarFile = if (userId != -1L) File(requireContext().filesDir, "profile_avatar_$userId.jpg") else null
        val picPath = sessionManager.getProfilePicPath()

        val avatarFile = when {
            picPath != null && File(picPath).exists() -> File(picPath)
            userAvatarFile != null && userAvatarFile.exists() -> userAvatarFile
            else -> null
        }

        if (avatarFile != null && avatarFile.exists()) {
            binding.ivProfileAvatar.setImageURI(null)
            binding.ivProfileAvatar.setImageURI(Uri.fromFile(avatarFile))
            binding.ivProfileAvatar.setPadding(0, 0, 0, 0)
            binding.ivProfileAvatar.imageTintList = null
        } else {
            setDefaultAvatar()
        }
    }

    private fun setDefaultAvatar() {
        binding.ivProfileAvatar.setImageResource(R.drawable.ic_profile)
        binding.ivProfileAvatar.setPadding(12, 12, 12, 12)
        binding.ivProfileAvatar.imageTintList = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), R.color.navy_primary)
        )
    }

    private fun setupListeners() {
        binding.ivProfileAvatar.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnEditProfile.setOnClickListener {
            showEditProfileDialog()
        }

        // Notification Toggles
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            sessionManager.setNotificationsEnabled(isChecked)
        }
        binding.switchReminders.setOnCheckedChangeListener { _, isChecked ->
            sessionManager.setRemindersEnabled(isChecked)
        }
        binding.switchAlerts.setOnCheckedChangeListener { _, isChecked ->
            sessionManager.setAlertsEnabled(isChecked)
        }

        // Row Click Listeners
        binding.rowChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.rowAboutApp.setOnClickListener {
            showAboutAppDialog()
        }

        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun showEditProfileDialog() {
        val dialogBinding = DialogEditProfileBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        val currentFullName = sessionManager.getLoggedInFullName() ?: ""
        val nameParts = currentFullName.split(" ").filter { it.isNotBlank() }
        val firstName = if (nameParts.isNotEmpty()) nameParts[0] else ""
        val middleName = if (nameParts.size > 2) nameParts.subList(1, nameParts.size - 1).joinToString(" ") else ""
        val lastName = if (nameParts.size > 1) nameParts.last() else ""

        dialogBinding.etEditFirstName.setText(firstName)
        dialogBinding.etEditMiddleName.setText(middleName)
        dialogBinding.etEditLastName.setText(lastName)
        dialogBinding.etEditEmail.setText(sessionManager.getLoggedInEmail())

        dialogBinding.btnCancelEditProfile.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSaveEditProfile.setOnClickListener {
            val newFirstName = dialogBinding.etEditFirstName.text.toString().trim()
            val newMiddleName = dialogBinding.etEditMiddleName.text.toString().trim()
            val newLastName = dialogBinding.etEditLastName.text.toString().trim()
            val newEmail = dialogBinding.etEditEmail.text.toString().trim()

            dialogBinding.tilEditFirstName.error = null
            dialogBinding.tilEditMiddleName.error = null
            dialogBinding.tilEditLastName.error = null
            dialogBinding.tilEditEmail.error = null

            if (newFirstName.isEmpty()) {
                dialogBinding.tilEditFirstName.error = "First name is required"
                return@setOnClickListener
            }

            if (newLastName.isEmpty()) {
                dialogBinding.tilEditLastName.error = "Last name is required"
                return@setOnClickListener
            }

            if (newEmail.isEmpty()) {
                dialogBinding.tilEditEmail.error = "Email cannot be empty"
                return@setOnClickListener
            }

            val newFullName = if (newMiddleName.isNotEmpty()) {
                "$newFirstName $newMiddleName $newLastName"
            } else {
                "$newFirstName $newLastName"
            }

            val userId = sessionManager.getLoggedInUserId()
            lifecycleScope.launch {
                val result = userRepository.updateUserProfile(userId, newFullName, newEmail)
                result.onSuccess {
                    sessionManager.updateUserInfo(newFullName, newEmail)
                    setupUI()
                    dialog.dismiss()
                    Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                }.onFailure { ex ->
                    Toast.makeText(requireContext(), ex.message ?: "Failed to update profile", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    private fun showChangePasswordDialog() {
        val dialogBinding = DialogChangePasswordBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancelChangePassword.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSaveChangePassword.setOnClickListener {
            val currentPass = dialogBinding.etCurrentPassword.text.toString().trim()
            val newPass = dialogBinding.etNewPassword.text.toString().trim()
            val confirmPass = dialogBinding.etConfirmNewPassword.text.toString().trim()

            dialogBinding.tilCurrentPassword.error = null
            dialogBinding.tilNewPassword.error = null
            dialogBinding.tilConfirmNewPassword.error = null

            if (currentPass.isEmpty()) {
                dialogBinding.tilCurrentPassword.error = "Current password is required"
                return@setOnClickListener
            }

            if (newPass.isEmpty()) {
                dialogBinding.tilNewPassword.error = "New password is required"
                return@setOnClickListener
            }

            if (newPass != confirmPass) {
                dialogBinding.tilConfirmNewPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            val userId = sessionManager.getLoggedInUserId()
            lifecycleScope.launch {
                val result = userRepository.changePassword(userId, currentPass, newPass)
                result.onSuccess {
                    dialog.dismiss()
                    Toast.makeText(requireContext(), "Password changed successfully!", Toast.LENGTH_SHORT).show()
                }.onFailure { ex ->
                    dialogBinding.tilCurrentPassword.error = ex.message
                }
            }
        }

        dialog.show()
    }

    private fun showAboutAppDialog() {
        val dialogBinding = DialogAboutAppBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnAboutOk.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showLogoutDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_logout, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.btn_logout_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btn_logout_confirm).setOnClickListener {
            dialog.dismiss()
            sessionManager.clearSession()
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()

            val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            activity?.finish()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
