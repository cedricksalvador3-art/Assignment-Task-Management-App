package com.example.assignment_task_management_app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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

        val cleanTitle = title.replace(" ", "_")
            .replace(".pdf", "", ignoreCase = true)
            .replace(".docx", "", ignoreCase = true)
            .replace(".png", "", ignoreCase = true)
            .replace(".jpg", "", ignoreCase = true)
            .ifBlank { "DueWise_Scan_$timestamp" }

        // Always decode the true high-resolution original image bitmap
        val originalBitmap: Bitmap? = loadOriginalBitmap(context, sourceUri, bitmap)

        // 1. PDF Export Format
        if (extension == "pdf") {
            val bitmapToUse = originalBitmap ?: Bitmap.createBitmap(600, 800, Bitmap.Config.ARGB_8888)
            return AssignmentPdfGenerator.generatePdfFromScan(
                context = context,
                scannedBitmap = bitmapToUse,
                title = cleanTitle,
                subject = "Scanned Assignment",
                mathSolutions = ""
            )
        }

        val targetFile = File(dir, "${cleanTitle}_$timestamp.$extension")

        try {
            // 2. Direct PNG/JPG Export Format
            if (sourceUri != null && (extension == "png" || extension == "jpg")) {
                val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
                if (inputStream != null) {
                    FileOutputStream(targetFile).use { output ->
                        inputStream.copyTo(output)
                    }
                    return targetFile.absolutePath
                }
            }

            // 3. DOCX Export Format (Embeds exact original image in OpenXML DOCX container)
            if (extension == "docx") {
                val imageBytes = getImageBytes(context, sourceUri, originalBitmap)
                if (imageBytes != null && imageBytes.isNotEmpty()) {
                    createDocxWithEmbeddedImage(imageBytes, "jpg", targetFile)
                    return targetFile.absolutePath
                }
            }

            // Fallback Bitmap Compress
            val bitmapToUse = originalBitmap ?: Bitmap.createBitmap(600, 800, Bitmap.Config.ARGB_8888)
            FileOutputStream(targetFile).use { output ->
                when (extension) {
                    "png" -> bitmapToUse.compress(Bitmap.CompressFormat.PNG, 100, output)
                    "jpg" -> bitmapToUse.compress(Bitmap.CompressFormat.JPEG, 95, output)
                    else -> bitmapToUse.compress(Bitmap.CompressFormat.JPEG, 95, output)
                }
            }
            return targetFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    private fun loadOriginalBitmap(context: Context, sourceUri: Uri?, fallbackBitmap: Bitmap?): Bitmap? {
        if (sourceUri != null) {
            try {
                context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                    val decoded = BitmapFactory.decodeStream(stream)
                    if (decoded != null && decoded.width > 0 && decoded.height > 0) {
                        return decoded
                    }
                }
            } catch (_: Exception) {}
        }
        if (fallbackBitmap != null && fallbackBitmap.width > 10 && fallbackBitmap.height > 10) {
            return fallbackBitmap
        }
        return null
    }

    private fun getImageBytes(context: Context, sourceUri: Uri?, bitmap: Bitmap?): ByteArray? {
        if (sourceUri != null) {
            try {
                context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                    return stream.readBytes()
                }
            } catch (_: Exception) {}
        }
        if (bitmap != null) {
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, baos)
            return baos.toByteArray()
        }
        return null
    }

    private fun createDocxWithEmbeddedImage(
        imageBytes: ByteArray,
        imageExtension: String,
        targetFile: File
    ) {
        val zipOut = ZipOutputStream(FileOutputStream(targetFile))

        fun addEntry(name: String, content: String) {
            val entry = ZipEntry(name)
            zipOut.putNextEntry(entry)
            zipOut.write(content.toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()
        }

        fun addBinaryEntry(name: String, bytes: ByteArray) {
            val entry = ZipEntry(name)
            zipOut.putNextEntry(entry)
            zipOut.write(bytes)
            zipOut.closeEntry()
        }

        val imgFileName = if (imageExtension.lowercase().contains("png")) "image1.png" else "image1.jpg"

        val contentTypesXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                <Default Extension="xml" ContentType="application/xml"/>
                <Default Extension="jpg" ContentType="image/jpeg"/>
                <Default Extension="jpeg" ContentType="image/jpeg"/>
                <Default Extension="png" ContentType="image/png"/>
                <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
            </Types>
        """.trimIndent()
        addEntry("[Content_Types].xml", contentTypesXml)

        val relsXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
            </Relationships>
        """.trimIndent()
        addEntry("_rels/.rels", relsXml)

        val docRelsXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/$imgFileName"/>
            </Relationships>
        """.trimIndent()
        addEntry("word/_rels/document.xml.rels", docRelsXml)

        addBinaryEntry("word/media/$imgFileName", imageBytes)

        val documentXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
                        xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
                        xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
                        xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"
                        xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">
                <w:body>
                    <w:p>
                        <w:r>
                            <w:drawing>
                                <wp:inline>
                                    <wp:extent cx="5283200" cy="7040800"/>
                                    <wp:docPr id="1" name="Scanned Document"/>
                                    <a:graphic>
                                        <a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">
                                            <pic:pic>
                                                <pic:nvPicPr>
                                                    <pic:cNvPr id="0" name="Scanned Image"/>
                                                    <pic:cNvPicPr/>
                                                </pic:nvPicPr>
                                                <pic:blipFill>
                                                    <a:blip r:embed="rId1"/>
                                                    <a:stretch><a:fillRect/></a:stretch>
                                                </pic:blipFill>
                                                <pic:spPr>
                                                    <a:xfrm>
                                                        <a:off x="0" y="0"/>
                                                        <a:ext cx="5283200" cy="7040800"/>
                                                    </a:xfrm>
                                                    <a:prstGeom prst="rect"><a:avLst/></a:prstGeom>
                                                </pic:spPr>
                                            </pic:pic>
                                        </a:graphicData>
                                    </a:graphic>
                                </wp:inline>
                            </w:drawing>
                        </w:r>
                    </w:p>
                </w:body>
            </w:document>
        """.trimIndent()
        addEntry("word/document.xml", documentXml)

        zipOut.finish()
        zipOut.close()
    }
}
