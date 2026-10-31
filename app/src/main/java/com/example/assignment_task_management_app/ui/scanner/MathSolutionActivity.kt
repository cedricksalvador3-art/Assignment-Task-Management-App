package com.example.assignment_task_management_app.ui.scanner

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.assignment_task_management_app.databinding.ActivityMathSolutionBinding
import com.example.assignment_task_management_app.db.DatabaseHelper
import com.example.assignment_task_management_app.model.Task
import com.example.assignment_task_management_app.model.TaskPriority
import com.example.assignment_task_management_app.model.TaskStatus
import com.example.assignment_task_management_app.repository.TaskRepository
import com.example.assignment_task_management_app.ui.main.MainActivity
import com.example.assignment_task_management_app.utils.AssignmentPdfGenerator
import com.example.assignment_task_management_app.utils.MathExpressionSolver
import com.example.assignment_task_management_app.utils.SessionManager
import kotlinx.coroutines.launch

class MathSolutionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMathSolutionBinding
    private lateinit var taskRepository: TaskRepository
    private lateinit var sessionManager: SessionManager

    private var problemText: String = ""
    private var answerText: String = ""
    private var stepsText: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMathSolutionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val dbHelper = DatabaseHelper.getInstance(this)
        taskRepository = TaskRepository(dbHelper)
        sessionManager = SessionManager(this)

        val rawProblem = intent.getStringExtra("EXTRA_PROBLEM") ?: ""
        val rawAnswer = intent.getStringExtra("EXTRA_ANSWER")
        val rawSteps = intent.getStringExtra("EXTRA_STEPS")
        val shouldPrompt = intent.getBooleanExtra("EXTRA_PROMPT_INPUT", false) || rawProblem.isBlank()

        if (rawProblem.isNotBlank()) {
            if (!rawAnswer.isNullOrBlank() && !rawSteps.isNullOrBlank()) {
                problemText = rawProblem
                answerText = rawAnswer
                stepsText = rawSteps
                binding.tvMathProblemText.text = problemText
                binding.tvMathFinalAnswer.text = answerText
                binding.tvMathStepsText.text = stepsText
            } else {
                recalculateSolution(rawProblem)
            }
        } else {
            recalculateSolution("5x + 10 = 35")
        }

        setupListeners()

        if (shouldPrompt) {
            showEditProblemDialog()
        }
    }

    private fun recalculateSolution(inputProblem: String) {
        val solution = MathExpressionSolver.solveWithPhotomathSteps(inputProblem)
        problemText = solution.problemText
        answerText = solution.finalAnswerText
        stepsText = solution.stepByStepExplanations

        binding.tvMathProblemText.text = problemText
        binding.tvMathFinalAnswer.text = answerText
        binding.tvMathStepsText.text = stepsText
    }

    private fun setupListeners() {
        binding.btnMathBack.setOnClickListener { finish() }

        binding.tvMathProblemText.setOnClickListener {
            showEditProblemDialog()
        }

        binding.btnScanAnother.setOnClickListener {
            val intent = Intent(this, ScannerActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.btnMathDashboard.setOnClickListener {
            saveMathSolutionToTask()
        }
    }

    private fun showEditProblemDialog() {
        val input = EditText(this).apply {
            if (problemText.isNotBlank()) {
                setText(problemText)
            }
            hint = "e.g., 5x + 10 = 35 or 4x - 8 = 24"
        }

        AlertDialog.Builder(this)
            .setTitle("Edit / Solve Math Problem")
            .setMessage("Enter or edit the mathematical equation to solve:")
            .setView(input)
            .setPositiveButton("Solve") { dialog, _ ->
                val newProblem = input.text.toString().trim()
                if (newProblem.isNotEmpty()) {
                    recalculateSolution(newProblem)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveMathSolutionToTask() {
        val userId = sessionManager.getLoggedInUserId()
        if (userId == -1L) {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
            return
        }

        val fullSolutionSummary = "Problem: $problemText\nAnswer: $answerText\n\nSolution Steps:\n$stepsText"
        val sampleBitmap = Bitmap.createBitmap(400, 600, Bitmap.Config.ARGB_8888)

        val pdfPath = AssignmentPdfGenerator.generatePdfFromScan(
            context = this,
            scannedBitmap = sampleBitmap,
            title = "Math Solution ($problemText)",
            subject = "Mathematics",
            mathSolutions = fullSolutionSummary
        )

        val newTask = Task(
            userId = userId,
            title = "Math: $problemText",
            description = "Automatically scanned and solved math problem.\nAnswer: $answerText",
            subject = "Mathematics",
            dueDate = System.currentTimeMillis() + (24 * 60 * 60 * 1000L),
            priority = TaskPriority.HIGH,
            status = TaskStatus.PENDING,
            pdfPath = pdfPath,
            mathSolution = fullSolutionSummary
        )

        lifecycleScope.launch {
            binding.btnMathDashboard.isEnabled = false
            taskRepository.insertTask(newTask)
            Toast.makeText(this@MathSolutionActivity, "Math solution saved to your Task List!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this@MathSolutionActivity, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }
    }
}
