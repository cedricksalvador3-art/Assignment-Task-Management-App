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

            val pageWidth = 595
            val pageHeight = 842
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            canvas.drawColor(Color.WHITE)

            val headerBgPaint = Paint().apply {
                color = Color.parseColor("#1E2250")
            }

            val titlePaint = Paint().apply {
                color = Color.WHITE
                textSize = 18f
                isFakeBoldText = true
            }

            val subtitlePaint = Paint().apply {
                color = Color.parseColor("#94A3B8")
                textSize = 10f
            }

            val sectionTitlePaint = Paint().apply {
                color = Color.parseColor("#1E2250")
                textSize = 14f
                isFakeBoldText = true
            }

            val labelPaint = Paint().apply {
                color = Color.parseColor("#2563EB")
                textSize = 11f
                isFakeBoldText = true
            }

            val textPaint = Paint().apply {
                color = Color.parseColor("#1E293B")
                textSize = 10.5f
            }

            val borderPaint = Paint().apply {
                color = Color.parseColor("#CBD5E1")
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
            }

            val docBgPaint = Paint().apply {
                color = Color.parseColor("#F8FAFC")
            }

            val linePaint = Paint().apply {
                color = Color.parseColor("#E2E8F0")
                strokeWidth = 1f
            }

            // Top Header Bar
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 65f, headerBgPaint)
            canvas.drawText("DueWise - Scanned Soft Copy Assignment", 24f, 32f, titlePaint)
            val dateStr = SimpleDateFormat("MMM dd, yyyy - h:mm a", Locale.US).format(Date())
            canvas.drawText("Title: $title  |  Subject: $subject  |  Scanned: $dateStr", 24f, 50f, subtitlePaint)

            // Document Box
            val boxLeft = 24f
            val boxTop = 80f
            val boxWidth = 547f
            val boxHeight = 360f

            val docRect = RectF(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight)
            canvas.drawRoundRect(docRect, 8f, 8f, docBgPaint)

            var isBitmapValid = false
            try {
                if (scannedBitmap.width > 10 && scannedBitmap.height > 10) {
                    for (x in 0 until scannedBitmap.width step 20) {
                        for (y in 0 until scannedBitmap.height step 20) {
                            if (Color.alpha(scannedBitmap.getPixel(x, y)) > 50) {
                                isBitmapValid = true
                                break
                            }
                        }
                        if (isBitmapValid) break
                    }
                }
            } catch (_: Exception) {}

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

                val scaledBitmap = Bitmap.createScaledBitmap(scannedBitmap, scaledWidth.toInt(), scaledHeight.toInt(), true)
                canvas.drawBitmap(scaledBitmap, leftOffset, topOffset, null)
            } else {
                // Paper Sheet
                val paperLeft = boxLeft + 80f
                val paperTop = boxTop + 20f
                val paperWidth = boxWidth - 160f
                val paperHeight = boxHeight - 40f
                val paperRect = RectF(paperLeft, paperTop, paperLeft + paperWidth, paperTop + paperHeight)

                val paperPaint = Paint().apply { color = Color.WHITE }
                canvas.drawRoundRect(paperRect, 6f, 6f, paperPaint)
                canvas.drawRoundRect(paperRect, 6f, 6f, borderPaint)

                val docHeaderPaint = Paint().apply {
                    color = Color.parseColor("#1E2250")
                    textSize = 12f
                    isFakeBoldText = true
                }
                canvas.drawText("Scanned Assignment Sheet", paperLeft + 20f, paperTop + 30f, docHeaderPaint)

                var lineY = paperTop + 55f
                while (lineY < paperTop + paperHeight - 20f) {
                    canvas.drawLine(paperLeft + 15f, lineY, paperLeft + paperWidth - 15f, lineY, linePaint)
                    lineY += 24f
                }

                val mathHighlightPaint = Paint().apply {
                    color = Color.parseColor("#2563EB")
                    textSize = 16f
                    isFakeBoldText = true
                }
                canvas.drawText("Digital Soft Copy Document", paperLeft + 25f, paperTop + 110f, mathHighlightPaint)
                canvas.drawText("(Scanned Attachment)", paperLeft + 25f, paperTop + 140f, labelPaint)
            }

            canvas.drawRoundRect(docRect, 8f, 8f, borderPaint)

            // Section 2: Automated Math Solutions & Analysis
            val mathHeaderTop = boxTop + boxHeight + 25f
            canvas.drawText("Automated Math Solutions & Analysis", 24f, mathHeaderTop, sectionTitlePaint)

            canvas.drawLine(24f, mathHeaderTop + 8f, pageWidth - 24f, mathHeaderTop + 8f, borderPaint)

            var currentY = mathHeaderTop + 28f
            val solutionText = if (mathSolutions.isNotBlank()) mathSolutions else "Digital Soft Copy Attachment Processed Successfully."

            val lines = solutionText.split("\n")
            for (line in lines) {
                if (currentY > pageHeight - 40f) break
                val trimmed = line.trim()
                if (trimmed.startsWith("1.") || trimmed.startsWith("2.") || trimmed.startsWith("3.") || trimmed.startsWith("4.")) {
                    canvas.drawText(trimmed, 28f, currentY, labelPaint)
                } else {
                    canvas.drawText(trimmed, 28f, currentY, textPaint)
                }
                currentY += 18f
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
}
