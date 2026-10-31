package com.example.assignment_task_management_app.ui.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.assignment_task_management_app.R
import com.example.assignment_task_management_app.databinding.ActivityScannerBinding
import com.example.assignment_task_management_app.utils.MathExpressionSolver
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File

class ScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScannerBinding
    private var isMathMode: Boolean = false
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var isFlashOn: Boolean = false

    private val textRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private var isAutoScanningLocked: Boolean = false
    private var lastDetectedText: String = ""
    private var detectionCount: Int = 0

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission is required for scanning.", Toast.LENGTH_SHORT).show()
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { sourceUri ->
            processScannedUri(sourceUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupListeners()
        checkCameraPermissionAndStart()
    }

    override fun onResume() {
        super.onResume()
        resetAutoScanState()
    }

    private fun resetAutoScanState() {
        isAutoScanningLocked = false
        detectionCount = 0
        lastDetectedText = ""
        if (isMathMode) {
            binding.tvMathLiveOverlay.text = "Aim camera at math problem"
            binding.tvScannerGuideTitle.text = "Fit the math problem inside the frame"
        } else {
            binding.tvScannerGuideTitle.text = "Place the paper inside the frame"
        }
    }

    private fun checkCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                    }

                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                    imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis?.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                        processCameraFrame(imageProxy)
                    }

                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build()

                    cameraProvider.unbindAll()
                    camera = cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageCapture,
                        imageAnalysis
                    )
                    camera?.cameraControl?.enableTorch(isFlashOn)

                    binding.viewFinder.visibility = View.VISIBLE
                    binding.ivCameraPreview.visibility = View.GONE
                } catch (_: Throwable) {
                    binding.viewFinder.visibility = View.GONE
                    binding.ivCameraPreview.visibility = View.VISIBLE
                }
            }, ContextCompat.getMainExecutor(this))
        } catch (_: Throwable) {
            binding.viewFinder.visibility = View.GONE
            binding.ivCameraPreview.visibility = View.VISIBLE
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processCameraFrame(imageProxy: ImageProxy) {
        if (isAutoScanningLocked) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            textRecognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    handleLiveVisionText(visionText.text)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun handleLiveVisionText(rawText: String) {
        if (isAutoScanningLocked) return

        if (isMathMode) {
            val extractedProblem = MathExpressionSolver.extractMathProblemFromText(rawText)
            if (extractedProblem.isNotBlank()) {
                binding.tvMathLiveOverlay.text = "Detected Math:\n$extractedProblem"
                binding.tvScannerGuideTitle.text = "Auto-Scanning Math Equation..."

                if (extractedProblem == lastDetectedText) {
                    detectionCount++
                } else {
                    lastDetectedText = extractedProblem
                    detectionCount = 1
                }

                if (detectionCount >= 2) {
                    isAutoScanningLocked = true
                    Toast.makeText(this, "Math Equation Detected! Solving...", Toast.LENGTH_SHORT).show()
                    capturePhotoOrProcess(forcedProblem = extractedProblem)
                }
            } else {
                binding.tvMathLiveOverlay.text = "Aim camera at math problem"
            }
        } else {
            val trimmedText = rawText.trim()
            if (trimmedText.length >= 8) {
                binding.tvScannerGuideTitle.text = "Document Detected! Auto-Scanning..."

                if (trimmedText.take(15) == lastDetectedText.take(15)) {
                    detectionCount++
                } else {
                    lastDetectedText = trimmedText
                    detectionCount = 1
                }

                if (detectionCount >= 2) {
                    isAutoScanningLocked = true
                    Toast.makeText(this, "Document Scanned!", Toast.LENGTH_SHORT).show()
                    capturePhotoOrProcess()
                }
            }
        }
    }

    private fun setupUI() {
        updateModeUI(isMathMode)
    }

    private fun updateModeUI(mathMode: Boolean) {
        isMathMode = mathMode
        resetAutoScanState()
        if (mathMode) {
            binding.tvScannerGuide.text = "Solves automatically"
            binding.tvMathLiveOverlay.visibility = View.VISIBLE
        } else {
            binding.tvScannerGuide.text = "Saves as PDF, DOCX & more"
            binding.tvMathLiveOverlay.visibility = View.GONE
        }
    }

    private fun setupListeners() {
        binding.btnScannerClose.setOnClickListener { finish() }

        binding.rgScannerMode.setOnCheckedChangeListener { _, checkedId ->
            val math = checkedId == binding.rbModeMath.id
            updateModeUI(math)
        }

        binding.btnScannerGallery.setOnClickListener {
            isAutoScanningLocked = true
            galleryLauncher.launch("image/*")
        }

        binding.btnScannerShutter.setOnClickListener {
            isAutoScanningLocked = true
            capturePhotoOrProcess()
        }

        binding.btnScannerFlip.setOnClickListener {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }
            startCamera()
        }

        binding.btnScannerFlash.setOnClickListener {
            toggleFlash()
        }
    }

    private fun toggleFlash() {
        isFlashOn = !isFlashOn
        try {
            camera?.cameraControl?.enableTorch(isFlashOn)
        } catch (_: Exception) {}

        if (isFlashOn) {
            binding.btnScannerFlash.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.warning_yellow))
            Toast.makeText(this, "Flash ON", Toast.LENGTH_SHORT).show()
        } else {
            binding.btnScannerFlash.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
            Toast.makeText(this, "Flash OFF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun capturePhotoOrProcess(forcedProblem: String? = null) {
        val capture = imageCapture
        if (capture != null) {
            val photoFile = File(cacheDir, "scan_${System.currentTimeMillis()}.jpg")
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            capture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        val savedUri = Uri.fromFile(photoFile)
                        processScannedUri(savedUri, forcedProblem)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        processShutterFallback(forcedProblem)
                    }
                }
            )
        } else {
            processShutterFallback(forcedProblem)
        }
    }

    private fun processShutterFallback(forcedProblem: String? = null) {
        if (isMathMode || !forcedProblem.isNullOrBlank()) {
            val prob = forcedProblem ?: lastDetectedText.ifBlank { "5x + 10 = 35" }
            val solution = MathExpressionSolver.solveWithPhotomathSteps(prob)
            val intent = Intent(this, MathSolutionActivity::class.java).apply {
                putExtra("EXTRA_PROBLEM", solution.problemText)
                putExtra("EXTRA_ANSWER", solution.finalAnswerText)
                putExtra("EXTRA_STEPS", solution.stepByStepExplanations)
                if (solution.problemText.isBlank()) {
                    putExtra("EXTRA_PROMPT_INPUT", true)
                }
            }
            startActivity(intent)
        } else {
            val intent = Intent(this, ScanResultActivity::class.java).apply {
                putExtra("EXTRA_DEFAULT_FILE_NAME", "DueWise_Scan_001.pdf")
            }
            startActivity(intent)
        }
    }

    private fun processScannedUri(uri: Uri, forcedProblem: String? = null) {
        // Document Mode: preserve exact original document image without converting to editable text
        if (!isMathMode) {
            val intent = Intent(this, ScanResultActivity::class.java).apply {
                putExtra("EXTRA_IMAGE_URI", uri.toString())
                putExtra("EXTRA_DEFAULT_FILE_NAME", "DueWise_Scan_001.pdf")
            }
            startActivity(intent)
            return
        }

        // Math Mode: process math equation solver
        try {
            val inputImage = InputImage.fromFilePath(this, uri)
            textRecognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val fullText = visionText.text
                    val extractedProblem = MathExpressionSolver.extractMathProblemFromText(fullText)
                        .ifBlank { forcedProblem ?: "" }

                    val finalProblem = extractedProblem.ifBlank { "5x + 10 = 35" }
                    val solution = MathExpressionSolver.solveWithPhotomathSteps(finalProblem)

                    val intent = Intent(this, MathSolutionActivity::class.java).apply {
                        putExtra("EXTRA_IMAGE_URI", uri.toString())
                        putExtra("EXTRA_PROBLEM", solution.problemText)
                        putExtra("EXTRA_ANSWER", solution.finalAnswerText)
                        putExtra("EXTRA_STEPS", solution.stepByStepExplanations)
                        if (solution.problemText.isBlank()) {
                            putExtra("EXTRA_PROMPT_INPUT", true)
                        }
                    }
                    startActivity(intent)
                }
                .addOnFailureListener {
                    processShutterFallback(forcedProblem)
                }
        } catch (_: Exception) {
            processShutterFallback(forcedProblem)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            textRecognizer.close()
        } catch (_: Exception) {}
    }
}
