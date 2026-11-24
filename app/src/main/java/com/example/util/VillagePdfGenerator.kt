package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.GovernmentScheme
import com.example.data.model.VillageRecord
import com.example.ml.VillageMlAnalysisResult
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object VillagePdfGenerator {

    private const val PAGE_WIDTH = 595 // A4 width in points
    private const val PAGE_HEIGHT = 842 // A4 height in points
    private const val MARGIN = 36f

    fun writeReportToCacheAndGetUri(
        context: Context,
        village: VillageRecord,
        mlResult: VillageMlAnalysisResult,
        aiSummary: String?,
        matchedSchemes: List<GovernmentScheme>
    ): Uri {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val safeName = village.name.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(30)
        val outFile = File(reportsDir, "SmartVillage_${safeName}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outFile).use { stream ->
            writeReportToStream(stream, village, mlResult, aiSummary, matchedSchemes)
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outFile
        )
    }

    fun writeReportToStream(
        outputStream: OutputStream,
        village: VillageRecord,
        mlResult: VillageMlAnalysisResult,
        aiSummary: String?,
        matchedSchemes: List<GovernmentScheme>
    ) {
        val pdf = PdfDocument()
        var pageNum = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
        var canvas = page.canvas

        val greenPaint = Paint().apply { color = Color.rgb(27, 107, 70) }
        val skyPaint = Paint().apply { color = Color.rgb(2, 132, 199) }
        val lightMintBg = Paint().apply { color = Color.rgb(244, 250, 247) }
        val dividerPaint = Paint().apply { color = Color.rgb(210, 226, 217); strokeWidth = 1f }

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint().apply {
            color = Color.rgb(220, 248, 234)
            textSize = 10f
        }
        val sectionHeaderPaint = Paint().apply {
            color = Color.rgb(27, 107, 70)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(20, 30, 40)
            textSize = 10.5f
        }
        val boldBodyPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val mutedPaint = Paint().apply {
            color = Color.rgb(90, 105, 120)
            textSize = 9f
        }

        // Top Header Banner
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 88f, greenPaint)
        canvas.drawText("AI-Based Smart Village Development System", MARGIN, 36f, titlePaint)
        canvas.drawText(
            "Empowering Villages Through Artificial Intelligence • Official Evaluation Report",
            MARGIN,
            56f,
            subtitlePaint
        )
        val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Generated: $dateStr", MARGIN, 73f, subtitlePaint)

        var y = 112f

        fun ensureSpace(needed: Float) {
            if (y + needed > PAGE_HEIGHT - 44f) {
                pdf.finishPage(page)
                pageNum++
                page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
                canvas = page.canvas
                y = 44f
            }
        }

        fun drawWrappedText(text: String, paint: Paint, indent: Float = 0f, lineHeight: Float = 14f) {
            val maxWidth = PAGE_WIDTH - (MARGIN * 2) - indent
            val paragraphs = text.split("\n")
            for (para in paragraphs) {
                val words = para.split(" ")
                var line = ""
                for (word in words) {
                    val candidate = if (line.isEmpty()) word else "$line $word"
                    if (paint.measureText(candidate) > maxWidth) {
                        ensureSpace(lineHeight)
                        canvas.drawText(line, MARGIN + indent, y, paint)
                        y += lineHeight
                        line = word
                    } else {
                        line = candidate
                    }
                }
                if (line.isNotEmpty()) {
                    ensureSpace(lineHeight)
                    canvas.drawText(line, MARGIN + indent, y, paint)
                    y += lineHeight
                }
            }
        }

        // Village Metadata Card
        canvas.drawRoundRect(RectF(MARGIN, y - 10f, PAGE_WIDTH - MARGIN, y + 76f), 8f, 8f, lightMintBg)
        canvas.drawText("Village: ${village.name}  (${village.district}, ${village.state})", MARGIN + 12f, y + 10f, boldBodyPaint)
        canvas.drawText(
            "Data Classification: ${village.sourceEnum.label}   |   Population: ${village.population.total} (${village.population.households} households)",
            MARGIN + 12f,
            y + 28f,
            bodyPaint
        )
        canvas.drawText(
            "Composite Development Score: ${mlResult.scores.overallScore}/100   |   Tier: ${mlResult.scores.developmentLevel}",
            MARGIN + 12f,
            y + 46f,
            boldBodyPaint
        )
        canvas.drawText(
            "Weakest Sector: ${mlResult.scores.weakestSector}   |   Area: ${village.areaSqKm} sq.km",
            MARGIN + 12f,
            y + 64f,
            mutedPaint
        )
        y += 96f

        // Sector Scores Visual Bars
        canvas.drawText("1. Sector-Wise Development Scores", MARGIN, y, sectionHeaderPaint)
        y += 18f
        val sectors = listOf(
            "Agriculture" to mlResult.scores.agricultureScore,
            "Healthcare" to mlResult.scores.healthcareScore,
            "Education" to mlResult.scores.educationScore,
            "Infrastructure" to mlResult.scores.infrastructureScore
        )
        val barStartX = MARGIN + 115f
        val barMaxW = PAGE_WIDTH - MARGIN - barStartX - 55f
        sectors.forEach { (label, score) ->
            ensureSpace(22f)
            canvas.drawText(label, MARGIN, y, boldBodyPaint)
            val track = RectF(barStartX, y - 10f, barStartX + barMaxW, y + 2f)
            canvas.drawRoundRect(track, 4f, 4f, Paint().apply { color = Color.rgb(226, 232, 240) })
            val fillPaint = when {
                score >= 75 -> greenPaint
                score >= 55 -> skyPaint
                else -> Paint().apply { color = Color.rgb(217, 119, 6) }
            }
            val fillW = (barMaxW * (score / 100.0).toFloat()).coerceIn(6f, barMaxW)
            canvas.drawRoundRect(RectF(barStartX, y - 10f, barStartX + fillW, y + 2f), 4f, 4f, fillPaint)
            canvas.drawText("${score}%", barStartX + barMaxW + 8f, y, boldBodyPaint)
            y += 22f
        }

        y += 6f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, dividerPaint)
        y += 18f

        // ML Model Output & Holdout Evaluation
        ensureSpace(70f)
        canvas.drawText("2. Machine Learning Analysis & Holdout Metrics", MARGIN, y, sectionHeaderPaint)
        y += 16f
        drawWrappedText(
            "• Ridge Linear Regression Prediction: ${mlResult.predictedRegressionScore}/100  |  k-NN (k=3) Tier: ${mlResult.knnClassifiedTier.label}",
            bodyPaint
        )
        drawWrappedText(
            "• Holdout Evaluation (n=${mlResult.evaluationReport.testHoldoutCount} test / ${mlResult.evaluationReport.trainingSampleCount} train synthetic benchmark): MAE=${mlResult.evaluationReport.linearRegressionMae}, RMSE=${mlResult.evaluationReport.linearRegressionRmse}, R²=${mlResult.evaluationReport.linearRegressionR2}, k-NN Acc=${mlResult.evaluationReport.knnAccuracyPct}%",
            mutedPaint
        )
        y += 8f

        // Sector Gaps & Recommendations
        ensureSpace(40f)
        canvas.drawText("3. Identified Gaps & Priority Recommendations", MARGIN, y, sectionHeaderPaint)
        y += 16f
        mlResult.prioritizedRecommendations.forEach { rec ->
            drawWrappedText("• $rec", bodyPaint, indent = 6f)
        }

        // AI Executive Summary (if generated)
        if (!aiSummary.isNullOrBlank()) {
            y += 8f
            ensureSpace(40f)
            canvas.drawText("4. Gemini AI Executive Summary", MARGIN, y, sectionHeaderPaint)
            y += 16f
            drawWrappedText(aiSummary, bodyPaint, indent = 4f)
        }

        // Matched Government Schemes
        if (matchedSchemes.isNotEmpty()) {
            y += 8f
            ensureSpace(40f)
            canvas.drawText("5. Recommended Government Schemes", MARGIN, y, sectionHeaderPaint)
            y += 16f
            matchedSchemes.take(6).forEach { scheme ->
                drawWrappedText("• ${scheme.name} [${scheme.sector}] — ${scheme.benefits} (${scheme.officialWebsite})", bodyPaint, indent = 6f)
            }
        }

        pdf.finishPage(page)
        pdf.writeTo(outputStream)
        pdf.close()
    }
}
