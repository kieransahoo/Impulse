package com.impulse.ui.main

import android.graphics.Paint
import android.graphics.pdf.PdfDocument

data class ExportPlanStep(
    val title: String,
    val durationMinutes: Int?,
    val reason: String?
)

data class ExportPlan(
    val goal: String,
    val explanation: String,
    val steps: List<ExportPlanStep>,
    val sourceCount: Int
)

fun ExportPlan.toPdf(): ByteArray {
    val document = PdfDocument()
    val pageWidth = 595
    val pageHeight = 842
    val left = 48f
    val right = 547f
    val bottom = 790f
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(23, 23, 20)
        textSize = 24f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD)
    }
    val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(23, 23, 20)
        textSize = 13f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(70, 70, 65)
        textSize = 11f
    }
    val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(232, 93, 53)
        textSize = 10f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    var pageNumber = 0
    var page: PdfDocument.Page? = null
    var y = 0f

    fun newPage() {
        page?.let(document::finishPage)
        pageNumber += 1
        page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        y = 52f
        page!!.canvas.drawText("IMPULSE · PERSONALIZED PLAN", left, y, accentPaint)
        y += 30f
    }

    fun lines(text: String, maxCharacters: Int): List<String> {
        val words = text.trim().split(Regex("\\s+"))
        val output = mutableListOf<String>()
        var current = ""
        words.forEach { word ->
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (candidate.length > maxCharacters && current.isNotEmpty()) {
                output += current
                current = word
            } else {
                current = candidate
            }
        }
        if (current.isNotEmpty()) output += current
        return output
    }

    fun ensureSpace(height: Float) {
        if (page == null || y + height > bottom) newPage()
    }

    fun canvas() = checkNotNull(page).canvas

    fun drawWrapped(text: String, paint: Paint, lineHeight: Float, maxCharacters: Int) {
        lines(text, maxCharacters).forEach { line ->
            ensureSpace(lineHeight)
            page!!.canvas.drawText(line, left, y, paint)
            y += lineHeight
        }
    }

    newPage()
    drawWrapped(goal, titlePaint, 30f, 42)
    y += 8f
    drawWrapped(explanation, bodyPaint, 16f, 84)
    y += 20f
    canvas().drawText("CHECKLIST", left, y, accentPaint)
    y += 22f

    steps.forEachIndexed { index, step ->
        val reasonLines = step.reason?.let { lines(it, 76).size } ?: 0
        ensureSpace(42f + reasonLines * 15f)
        canvas().drawRect(left, y - 11f, left + 11f, y, headingPaint.apply { style = Paint.Style.STROKE })
        headingPaint.style = Paint.Style.FILL
        val duration = step.durationMinutes?.let { " · $it min" }.orEmpty()
        drawWrapped("${index + 1}. ${step.title}$duration", headingPaint, 18f, 70)
        step.reason?.takeIf(String::isNotBlank)?.let {
            drawWrapped(it, bodyPaint, 15f, 78)
        }
        y += 12f
        canvas().drawLine(left, y, right, y, Paint().apply { color = android.graphics.Color.LTGRAY })
        y += 16f
    }

    ensureSpace(30f)
    canvas().drawText(
        "Grounded in $sourceCount saved ${if (sourceCount == 1) "memory" else "memories"}",
        left,
        y,
        accentPaint
    )
    document.finishPage(checkNotNull(page))

    return java.io.ByteArrayOutputStream().use { output ->
        document.writeTo(output)
        document.close()
        output.toByteArray()
    }
}
