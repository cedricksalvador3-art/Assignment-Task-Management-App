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
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.assignment_task_management_app.R
import com.example.assignment_task_management_app.databinding.ActivityScannerBinding
import com.example.assignment_task_management_app.utils.MathExpressionSolver
import java.io.File

class ScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScannerBinding
    private var isMathMode: Boolean = false
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var isFlashOn: Boolean = false

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

                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build()

                    cameraProvider.unbindAll()
                    camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
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

    private fun setupUI() {
        updateModeUI(isMathMode)
    }

    private fun updateModeUI(mathMode: Boolean) {
        isMathMode = mathMode
        if (mathMode) {
            binding.tvScannerGuide.text = "Fit the math problem inside the frame\nSolves automatically"
            binding.tvMathLiveOverlay.visibility = View.VISIBLE
        } else {
            binding.tvScannerGuide.text = "Place the paper inside the frame\nSaves as PDF, DOCX & more"
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
            galleryLauncher.launch("image/*")
        }

        binding.btnScannerShutter.setOnClickListener {
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

    private fun capturePhotoOrProcess() {
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
                        processScannedUri(savedUri)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        processShutterFallback()
                    }
                }
            )
        } else {
            processShutterFallback()
        }
    }

    private fun processShutterFallback() {
        if (isMathMode) {
            val intent = Intent(this, MathSolutionActivity::class.java).apply {
                putExtra("EXTRA_PROMPT_INPUT", true)
            }
            startActivity(intent)
        } else {
            val intent = Intent(this, ScanResultActivity::class.java).apply {
                putExtra("EXTRA_DEFAULT_FILE_NAME", "DueWise_Scan_001.pdf")
            }
            startActivity(intent)
        }
    }

    private fun processScannedUri(uri: Uri) {
        if (isMathMode) {
            val intent = Intent(this, MathSolutionActivity::class.java).apply {
                putExtra("EXTRA_IMAGE_URI", uri.toString())
                putExtra("EXTRA_PROMPT_INPUT", true)
            }
            startActivity(intent)
        } else {
            val intent = Intent(this, ScanResultActivity::class.java).apply {
                putExtra("EXTRA_IMAGE_URI", uri.toString())
                putExtra("EXTRA_DEFAULT_FILE_NAME", "DueWise_Scan_001.pdf")
            }
            startActivity(intent)
        }
    }
}
