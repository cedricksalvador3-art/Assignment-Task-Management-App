package com.example.assignment_task_management_app.ui.scanner

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.assignment_task_management_app.databinding.ActivityScanResultBinding
import com.example.assignment_task_management_app.db.DatabaseHelper
import com.example.assignment_task_management_app.model.Task
import com.example.assignment_task_management_app.model.TaskPriority
import com.example.assignment_task_management_app.model.TaskStatus
import com.example.assignment_task_management_app.repository.TaskRepository
import com.example.assignment_task_management_app.ui.task.TaskDetailActivity
import com.example.assignment_task_management_app.utils.DateUtils
import com.example.assignment_task_management_app.utils.DocumentExporter
import com.example.assignment_task_management_app.utils.SessionManager
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

class ScanResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanResultBinding
    private lateinit var taskRepository: TaskRepository
    private lateinit var sessionManager: SessionManager

    private var imageUriStr: String? = null
    private var defaultFileName: String = "DueWise_Scan_001.pdf"

    private val calendar: Calendar = DateUtils.getPhilippineCalendar()
    private var selectedDueDateMs: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val dbHelper = DatabaseHelper.getInstance(this)
        taskRepository = TaskRepository(dbHelper)
        sessionManager = SessionManager(this)

        imageUriStr = intent.getStringExtra("EXTRA_IMAGE_URI")
        defaultFileName = intent.getStringExtra("EXTRA_DEFAULT_FILE_NAME") ?: "DueWise_Scan_001.pdf"

        calendar.timeInMillis = System.currentTimeMillis()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        selectedDueDateMs = calendar.timeInMillis

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        binding.etScanFileName.setText(defaultFileName)
        updateDateDisplay()

        imageUriStr?.let { uriStr ->
            try {
                val uri = Uri.parse(uriStr)
                binding.ivScanResultPreview.setImageURI(uri)
            } catch (_: Exception) {
                // Keep default preview
            }
        }
    }

    private fun updateDateDisplay() {
        binding.btnScanPickDate.text = DateUtils.formatDate(selectedDueDateMs)
        binding.btnScanPickTime.text = DateUtils.formatTime(selectedDueDateMs)
    }

    private fun setupListeners() {
        binding.btnResultBack.setOnClickListener { finish() }
        binding.btnCancelScanResult.setOnClickListener { finish() }

        binding.rgFormatType.setOnCheckedChangeListener { _, checkedId ->
            updateFileNameExtension(checkedId)
        }

        binding.btnScanPickDate.setOnClickListener {
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    selectedDueDateMs = calendar.timeInMillis
                    updateDateDisplay()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            val minDateVal = minOf(calendar.timeInMillis, System.currentTimeMillis() - 1000L)
            datePicker.datePicker.minDate = minDateVal
            datePicker.show()
        }

        binding.btnScanPickTime.setOnClickListener {
            val timePicker = TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    calendar.set(Calendar.MINUTE, minute)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    selectedDueDateMs = calendar.timeInMillis
                    updateDateDisplay()
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
            )
            timePicker.show()
        }

        binding.btnSaveDownload.setOnClickListener {
            saveScanToTaskList()
        }
    }

    private fun updateFileNameExtension(checkedId: Int) {
        val currentFileName = binding.etScanFileName.text.toString().trim()
        val baseName = currentFileName
            .replace(".pdf", "", ignoreCase = true)
            .replace(".docx", "", ignoreCase = true)
            .replace(".png", "", ignoreCase = true)
            .replace(".jpg", "", ignoreCase = true)
            .replace(".jpeg", "", ignoreCase = true)
            .ifBlank { "DueWise_Scan_001" }

        val newExtension = when (checkedId) {
            binding.rbFormatDocx.id -> ".docx"
            binding.rbFormatPng.id -> ".png"
            binding.rbFormatJpg.id -> ".jpg"
            else -> ".pdf"
        }

        binding.etScanFileName.setText("$baseName$newExtension")
    }

    private fun openGeneratedFile(filePath: String?) {
        if (filePath.isNullOrBlank()) return

        try {
            val file = File(filePath)
            if (!file.exists()) return

            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                file
            )

            val mimeType = when {
                filePath.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                filePath.endsWith(".docx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                filePath.endsWith(".png", ignoreCase = true) -> "image/png"
                filePath.endsWith(".jpg", ignoreCase = true) || filePath.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
                filePath.endsWith(".txt", ignoreCase = true) -> "text/plain"
                else -> "*/*"
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(intent, "Open Generated Document"))
        } catch (_: Exception) {
            Toast.makeText(this, "Could not open generated file automatically.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveScanToTaskList() {
        val fileNameInput = binding.etScanFileName.text.toString().trim().ifBlank { "DueWise_Scan_001" }
        val userId = sessionManager.getLoggedInUserId()

        if (userId == -1L) {
            Toast.makeText(this, "User session expired. Please log in.", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedDueDateMs < System.currentTimeMillis()) {
            AlertDialog.Builder(this)
                .setTitle("Invalid Due Date")
                .setMessage("The selected due date has already passed. Please choose a valid future date.")
                .setPositiveButton("Choose Date") { dialog, _ ->
                    dialog.dismiss()
                    binding.btnScanPickDate.performClick()
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }

        val format = when (binding.rgFormatType.checkedRadioButtonId) {
            binding.rbFormatDocx.id -> "DOCX"
            binding.rbFormatPng.id -> "PNG"
            binding.rbFormatJpg.id -> "JPG"
            else -> "PDF"
        }

        val cleanTitle = fileNameInput
            .replace(".pdf", "", ignoreCase = true)
            .replace(".docx", "", ignoreCase = true)
            .replace(".png", "", ignoreCase = true)
            .replace(".jpg", "", ignoreCase = true)
            .replace(".jpeg", "", ignoreCase = true)

        val sourceUri = imageUriStr?.let { Uri.parse(it) }
        val scannedBitmap: Bitmap? = sourceUri?.let { uri ->
            try {
                contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } catch (_: Exception) {
                null
            }
        }

        val exportedPath = DocumentExporter.saveOriginalDocument(
            context = this,
            sourceUri = sourceUri,
            bitmap = scannedBitmap,
            title = cleanTitle,
            targetFormat = format
        )

        val newTask = Task(
            userId = userId,
            title = cleanTitle,
            description = "Attached original document ($format format).",
            subject = "Scanned Assignment",
            dueDate = selectedDueDateMs,
            priority = TaskPriority.MEDIUM,
            status = TaskStatus.PENDING,
            pdfPath = exportedPath,
            mathSolution = null
        )

        lifecycleScope.launch {
            binding.btnSaveDownload.isEnabled = false
            val result = taskRepository.insertTask(newTask)
            result.onSuccess { savedTaskId ->
                Toast.makeText(this@ScanResultActivity, "File saved & opening document...", Toast.LENGTH_SHORT).show()
                openGeneratedFile(exportedPath)

                val intent = Intent(this@ScanResultActivity, TaskDetailActivity::class.java).apply {
                    putExtra("EXTRA_TASK_ID", savedTaskId)
                }
                startActivity(intent)
                finish()
            }.onFailure { ex ->
                binding.btnSaveDownload.isEnabled = true
                Toast.makeText(this@ScanResultActivity, ex.message ?: "Save failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
