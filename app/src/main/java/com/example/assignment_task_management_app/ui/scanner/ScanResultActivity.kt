package com.example.assignment_task_management_app.ui.scanner

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.assignment_task_management_app.databinding.ActivityScanResultBinding
import com.example.assignment_task_management_app.db.DatabaseHelper
import com.example.assignment_task_management_app.model.Task
import com.example.assignment_task_management_app.model.TaskPriority
import com.example.assignment_task_management_app.model.TaskStatus
import com.example.assignment_task_management_app.repository.TaskRepository
import com.example.assignment_task_management_app.ui.main.MainActivity
import com.example.assignment_task_management_app.utils.DateUtils
import com.example.assignment_task_management_app.utils.DocumentExporter
import com.example.assignment_task_management_app.utils.SessionManager
import kotlinx.coroutines.launch
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
                // Keep default image
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

        val cleanTitle = fileNameInput.replace(".pdf", "").replace(".docx", "").replace(".png", "").replace(".jpg", "").replace(".jpeg", "")
        val sampleBitmap = Bitmap.createBitmap(400, 600, Bitmap.Config.ARGB_8888)
        val sourceUri = imageUriStr?.let { Uri.parse(it) }

        val exportedPath = DocumentExporter.saveOriginalDocument(
            context = this,
            sourceUri = sourceUri,
            bitmap = sampleBitmap,
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
            result.onSuccess {
                Toast.makeText(this@ScanResultActivity, "File saved in original format ($format)!", Toast.LENGTH_LONG).show()
                val intent = Intent(this@ScanResultActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
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
