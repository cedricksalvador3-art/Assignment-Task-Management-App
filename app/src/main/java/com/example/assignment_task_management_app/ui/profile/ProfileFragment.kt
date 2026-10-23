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
import com.example.assignment_task_management_app.R
import com.example.assignment_task_management_app.databinding.FragmentProfileBinding
import com.example.assignment_task_management_app.ui.auth.LoginActivity
import com.example.assignment_task_management_app.utils.SessionManager
import java.io.File

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager

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

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        val fullName = sessionManager.getLoggedInFullName() ?: "DueWise User"
        val email = sessionManager.getLoggedInEmail() ?: "jane.doe@email.com"

        binding.tvProfileName.text = fullName
        binding.tvProfileEmail.text = email

        loadProfilePicture()
    }

    private fun loadProfilePicture() {
        val picPath = sessionManager.getProfilePicPath()
        if (picPath != null) {
            val file = File(picPath)
            if (file.exists()) {
                binding.ivProfileAvatar.setImageURI(Uri.fromFile(file))
                binding.ivProfileAvatar.setPadding(0, 0, 0, 0)
                binding.ivProfileAvatar.imageTintList = null
            } else {
                setDefaultAvatar()
            }
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

        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }
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
