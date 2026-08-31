package com.asim.splitmate.core.utils

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.asim.splitmate.domain.model.Expense
import com.asim.splitmate.domain.model.Group
import com.asim.splitmate.domain.model.Settlement
import java.io.File
import java.io.FileOutputStream

object PdfExportHelper {

    fun generateReportPdf(
        context: Context,
        groups: List<Group>,
        expensesMap: Map<String, List<Expense>>,
        settlementsMap: Map<String, List<Settlement>> = emptyMap()
    ): File {
        val pdfDocument = PdfDocument()

        val pageWidth = 595
        val pageHeight = 842

        val primaryPaint = Paint().apply {
            color = Color.parseColor("#0F766E") // Deep Emerald
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val headerBgPaint = Paint().apply {
            color = Color.parseColor("#0F766E")
            style = Paint.Style.FILL
        }

        val headerTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val zebraPaint = Paint().apply {
            color = Color.parseColor("#F1F5F9")
            style = Paint.Style.FILL
        }

        val linePaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 1f
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        // Header Banner
        canvas.drawRect(30f, 30f, pageWidth - 30f, 75f, headerBgPaint)
        val titleTextPaint = Paint(headerTextPaint).apply { textSize = 16f }
        canvas.drawText("SPLITMATE - EXPENSE REPORT", 45f, 60f, titleTextPaint)
        canvas.drawText("Date: ${DateFormatter.formatDate(System.currentTimeMillis())}", pageWidth - 180f, 60f, headerTextPaint)

        var y = 100f

        for (group in groups) {
            val expenses = expensesMap[group.id] ?: emptyList()

            if (y > pageHeight - 120) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 40f
            }

            // Group Title
            canvas.drawText("Group: ${group.name} (${group.currencyCode} ${group.currencySymbol})", 30f, y, primaryPaint)
            y += 18f

            // Table Header
            canvas.drawRect(30f, y, pageWidth - 30f, y + 22f, headerBgPaint)
            canvas.drawText("Date", 40f, y + 15f, headerTextPaint)
            canvas.drawText("Title", 120f, y + 15f, headerTextPaint)
            canvas.drawText("Category", 260f, y + 15f, headerTextPaint)
            canvas.drawText("Paid By", 360f, y + 15f, headerTextPaint)
            canvas.drawText("Amount (${group.currencySymbol})", 460f, y + 15f, headerTextPaint)

            y += 22f

            var groupTotal = 0.0

            if (expenses.isEmpty()) {
                canvas.drawText("No expenses recorded", 40f, y + 14f, textPaint)
                y += 20f
            } else {
                expenses.forEachIndexed { index, exp ->
                    if (y > pageHeight - 60) {
                        pdfDocument.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        y = 40f
                    }

                    if (index % 2 == 1) {
                        canvas.drawRect(30f, y, pageWidth - 30f, y + 20f, zebraPaint)
                    }

                    groupTotal += exp.amount

                    val dateStr = DateFormatter.formatDate(exp.date)
                    val titleTruncated = if (exp.title.length > 20) exp.title.take(18) + ".." else exp.title
                    val catStr = exp.category.name
                    val paidByStr = if (exp.paidByUserName.length > 14) exp.paidByUserName.take(12) + ".." else exp.paidByUserName
                    val amountStr = String.format("%.2f", exp.amount)

                    canvas.drawText(dateStr, 40f, y + 14f, textPaint)
                    canvas.drawText(titleTruncated, 120f, y + 14f, textPaint)
                    canvas.drawText(catStr, 260f, y + 14f, textPaint)
                    canvas.drawText(paidByStr, 360f, y + 14f, textPaint)
                    canvas.drawText(amountStr, 460f, y + 14f, textPaint)

                    y += 20f
                }
            }

            // Summary Total
            canvas.drawLine(30f, y, pageWidth - 30f, y, linePaint)
            val totalTextPaint = Paint(textPaint).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            canvas.drawText("Total (${group.name}):", 340f, y + 16f, totalTextPaint)
            canvas.drawText("${group.currencySymbol}${String.format("%.2f", groupTotal)}", 460f, y + 16f, totalTextPaint)
            y += 35f
        }

        pdfDocument.finishPage(page)

        val outputFile = File(context.cacheDir, "SplitMate_Expenses_Report.pdf")
        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return outputFile
    }
}
