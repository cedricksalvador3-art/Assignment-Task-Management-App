package com.example.assignment_task_management_app.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DocumentExporter {

    fun saveOriginalDocument(
        context: Context,
        sourceUri: Uri?,
        bitmap: Bitmap?,
        title: String,
        targetFormat: String
    ): String? {
        val dir = File(context.filesDir, "scanned_assignments")
        if (!dir.exists()) {
            dir.mkdirs()
        }

        val timestamp = System.currentTimeMillis()
        val formatClean = targetFormat.lowercase().trim()
        val extension = when {
            formatClean.contains("png") -> "png"
            formatClean.contains("jpg") || formatClean.contains("jpeg") -> "jpg"
            formatClean.contains("docx") -> "docx"
            formatClean.contains("txt") -> "txt"
            else -> "pdf"
        }

        val cleanTitle = title.replace(" ", "_").replace(".pdf", "").replace(".docx", "").replace(".png", "").replace(".jpg", "").ifBlank { "DueWise_Scan_$timestamp" }
        val targetFile = File(dir, "${cleanTitle}_$timestamp.$extension")

        try {
            if (sourceUri != null) {
                val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
                if (inputStream != null) {
                    FileOutputStream(targetFile).use { output ->
                        inputStream.copyTo(output)
                    }
                    return targetFile.absolutePath
                }
            }

            if (bitmap != null) {
                FileOutputStream(targetFile).use { output ->
                    when (extension) {
                        "png" -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                        "jpg" -> bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)
                        "docx" -> {
                            val textContent = "DueWise Soft Copy Assignment (.DOCX)\nTitle: $cleanTitle\nDate: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}"
                            output.write(textContent.toByteArray())
                        }
                        "txt" -> {
                            val textContent = "DueWise Soft Copy Assignment (.TXT)\nTitle: $cleanTitle\nDate: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}"
                            output.write(textContent.toByteArray())
                        }
                        else -> {
                            return AssignmentPdfGenerator.generatePdfFromScan(
                                context = context,
                                scannedBitmap = bitmap,
                                title = cleanTitle,
                                subject = "Scanned Assignment",
                                mathSolutions = ""
                            )
                        }
                    }
                }
                return targetFile.absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }
}
