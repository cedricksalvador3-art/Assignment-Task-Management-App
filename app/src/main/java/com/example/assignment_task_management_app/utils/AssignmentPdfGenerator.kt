package com.example.assignment_task_management_app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AssignmentPdfGenerator {

    fun generatePdfFromScan(
        context: Context,
        scannedBitmap: Bitmap,
        title: String,
        subject: String,
        mathSolutions: String
    ): String? {
        return try {
            val pdfDocument = PdfDocument()

            // Standard A4 page dimensions (595 x 842 points)
            val pageWidth = 595
            val pageHeight = 842
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            // Page Background
            canvas.drawColor(Color.WHITE)

            // Paints
            val headerBgPaint = Paint().apply {
                color = Color.parseColor("#1E2250") // Navy Primary
            }

            val titlePaint = Paint().apply {
                color = Color.WHITE
                textSize = 16f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val subtitlePaint = Paint().apply {
                color = Color.parseColor("#94A3B8")
                textSize = 9.5f
                isAntiAlias = true
            }

            val sectionTitlePaint = Paint().apply {
                color = Color.parseColor("#1E2250")
                textSize = 13f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val labelPaint = Paint().apply {
                color = Color.parseColor("#2563EB")
                textSize = 10.5f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val textPaint = Paint().apply {
                color = Color.parseColor("#1E293B")
                textSize = 10f
                isAntiAlias = true
            }

            val borderPaint = Paint().apply {
                color = Color.parseColor("#CBD5E1")
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
                isAntiAlias = true
            }

            val docBgPaint = Paint().apply {
                color = Color.parseColor("#F8FAFC")
            }

            val linePaint = Paint().apply {
                color = Color.parseColor("#E2E8F0")
                strokeWidth = 1f
                isAntiAlias = true
            }

            val highQualityBitmapPaint = Paint().apply {
                isAntiAlias = true
                isFilterBitmap = true
                isDither = true
            }

            val pageMargin = 28f
            val maxTextWidth = pageWidth - (pageMargin * 2f)

            // 1. Top Header Bar
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 68f, headerBgPaint)
            canvas.drawText("DueWise - Scanned Soft Copy Assignment", pageMargin, 28f, titlePaint)

            val dateStr = SimpleDateFormat("MMM dd, yyyy - h:mm a", Locale.US).format(Date())
            val metadataLine = "Title: $title  |  Subject: $subject  |  Scanned: $dateStr"

            val displayMetadata = if (subtitlePaint.measureText(metadataLine) > maxTextWidth) {
                "Title: $title  |  Subject: $subject"
            } else {
                metadataLine
            }
            canvas.drawText(displayMetadata, pageMargin, 48f, subtitlePaint)

            // 2. Document Image Box
            val hasMathSolutions = mathSolutions.isNotBlank() && !mathSolutions.contains("Digital Soft Copy", ignoreCase = true)

            val boxLeft = pageMargin
            val boxTop = 82f
            val boxWidth = maxTextWidth
            val boxHeight = if (hasMathSolutions) 360f else 720f // Expanded height for document scans to preserve layout and maximum detail

            val docRect = RectF(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight)
            canvas.drawRoundRect(docRect, 8f, 8f, docBgPaint)

            val isBitmapValid = scannedBitmap.width > 0 && scannedBitmap.height > 0

            if (isBitmapValid) {
                val aspectRatio = scannedBitmap.width.toFloat() / scannedBitmap.height.toFloat()
                val scaledWidth: Float
                val scaledHeight: Float

                if (aspectRatio > (boxWidth / boxHeight)) {
                    scaledWidth = boxWidth - 16f
                    scaledHeight = scaledWidth / aspectRatio
                } else {
                    scaledHeight = boxHeight - 16f
                    scaledWidth = scaledHeight * aspectRatio
                }

                val leftOffset = boxLeft + (boxWidth - scaledWidth) / 2f
                val topOffset = boxTop + (boxHeight - scaledHeight) / 2f

                val scaledBitmap = Bitmap.createScaledBitmap(scannedBitmap, scaledWidth.toInt().coerceAtLeast(1), scaledHeight.toInt().coerceAtLeast(1), true)
                canvas.drawBitmap(scaledBitmap, leftOffset, topOffset, highQualityBitmapPaint)
            } else {
                // Paper Sheet Fallback
                val paperLeft = boxLeft + 40f
                val paperTop = boxTop + 20f
                val paperWidth = boxWidth - 80f
                val paperHeight = boxHeight - 40f
                val paperRect = RectF(paperLeft, paperTop, paperLeft + paperWidth, paperTop + paperHeight)

                val paperPaint = Paint().apply { color = Color.WHITE }
                canvas.drawRoundRect(paperRect, 6f, 6f, paperPaint)
                canvas.drawRoundRect(paperRect, 6f, 6f, borderPaint)

                val docHeaderPaint = Paint().apply {
                    color = Color.parseColor("#1E2250")
                    textSize = 12f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                canvas.drawText("Scanned Assignment Sheet", paperLeft + 24f, paperTop + 32f, docHeaderPaint)

                var lineY = paperTop + 55f
                while (lineY < paperTop + paperHeight - 20f) {
                    canvas.drawLine(paperLeft + 20f, lineY, paperLeft + paperWidth - 20f, lineY, linePaint)
                    lineY += 24f
                }
            }

            canvas.drawRoundRect(docRect, 8f, 8f, borderPaint)

            // 3. Section 2: Automated Math Solutions & Analysis (Shown ONLY if math solutions exist)
            if (hasMathSolutions) {
                val mathHeaderTop = boxTop + boxHeight + 22f
                canvas.drawText("Automated Math Solutions & Analysis", pageMargin, mathHeaderTop, sectionTitlePaint)

                canvas.drawLine(pageMargin, mathHeaderTop + 6f, pageWidth - pageMargin, mathHeaderTop + 6f, borderPaint)

                var currentY = mathHeaderTop + 24f
                val rawLines = mathSolutions.split("\n")
                for (line in rawLines) {
                    if (currentY > pageHeight - 35f) break
                    val trimmed = line.trim()
                    if (trimmed.isBlank()) continue

                    val paintToUse = if (trimmed.startsWith("1.") || trimmed.startsWith("2.") || trimmed.startsWith("3.") || trimmed.startsWith("4.")) {
                        labelPaint
                    } else {
                        textPaint
                    }

                    currentY = drawWrappedText(
                        canvas = canvas,
                        text = trimmed,
                        x = pageMargin + 4f,
                        startY = currentY,
                        maxWidth = maxTextWidth - 8f,
                        paint = paintToUse,
                        lineSpacing = 16f
                    )
                }
            }

            pdfDocument.finishPage(page)

            // Save PDF
            val dir = File(context.filesDir, "scanned_assignments")
            if (!dir.exists()) {
                dir.mkdirs()
            }

            val pdfFile = File(dir, "assignment_${System.currentTimeMillis()}.pdf")
            val fos = FileOutputStream(pdfFile)
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos.close()

            pdfFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        startY: Float,
        maxWidth: Float,
        paint: Paint,
        lineSpacing: Float
    ): Float {
        var currentY = startY
        val words = text.split(" ")
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    canvas.drawText(currentLine, x, currentY, paint)
                    currentY += lineSpacing
                }
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine, x, currentY, paint)
            currentY += lineSpacing
        }
        return currentY
    }
}
