package com.asim.splitmate.core.utils

import android.content.Context
import com.asim.splitmate.domain.model.Expense
import com.asim.splitmate.domain.model.Group
import com.asim.splitmate.domain.model.Settlement
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.xssf.usermodel.XSSFColor
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream

object XlsxExportHelper {

    fun generateReportXlsx(
        context: Context,
        groups: List<Group>,
        expensesMap: Map<String, List<Expense>>,
        settlementsMap: Map<String, List<Settlement>> = emptyMap()
    ): File {
        val workbook = XSSFWorkbook()

        // Color palette (Emerald Primary: RGB 16, 185, 129 -> #10B981)
        val emeraldColor = XSSFColor(byteArrayOf(16.toByte(), 185.toByte(), 129.toByte()), null)
        val lightEmerald = XSSFColor(byteArrayOf(236.toByte(), 253.toByte(), 245.toByte()), null)
        val darkHeader = XSSFColor(byteArrayOf(30.toByte(), 41.toByte(), 59.toByte()), null)

        // Styles
        val titleStyle = workbook.createCellStyle().apply {
            val font = workbook.createFont().apply {
                bold = true
                fontHeightInPoints = 16.toShort()
                setColor(emeraldColor)
            }
            setFont(font)
        }

        val metaLabelStyle = workbook.createCellStyle().apply {
            val font = workbook.createFont().apply {
                bold = true
                fontHeightInPoints = 10.toShort()
            }
            setFont(font)
        }

        val headerStyle = workbook.createCellStyle().apply {
            val font = workbook.createFont().apply {
                bold = true
                fontHeightInPoints = 11.toShort()
                color = IndexedColors.WHITE.index
            }
            setFont(font)
            setFillForegroundColor(emeraldColor)
            fillPattern = FillPatternType.SOLID_FOREGROUND
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

        val subHeaderStyle = workbook.createCellStyle().apply {
            val font = workbook.createFont().apply {
                bold = true
                fontHeightInPoints = 12.toShort()
                color = IndexedColors.WHITE.index
            }
            setFont(font)
            setFillForegroundColor(darkHeader)
            fillPattern = FillPatternType.SOLID_FOREGROUND
            verticalAlignment = VerticalAlignment.CENTER
        }

        val cellStyleNormal = workbook.createCellStyle().apply {
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
            verticalAlignment = VerticalAlignment.CENTER
        }

        val cellStyleZebra = workbook.createCellStyle().apply {
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
            setFillForegroundColor(lightEmerald)
            fillPattern = FillPatternType.SOLID_FOREGROUND
            verticalAlignment = VerticalAlignment.CENTER
        }

        val currencyFormat = workbook.createDataFormat().getFormat("#,##0.00")

        val numberStyleNormal = workbook.createCellStyle().apply {
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
            alignment = HorizontalAlignment.RIGHT
            verticalAlignment = VerticalAlignment.CENTER
            dataFormat = currencyFormat
        }

        val numberStyleZebra = workbook.createCellStyle().apply {
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
            setFillForegroundColor(lightEmerald)
            fillPattern = FillPatternType.SOLID_FOREGROUND
            alignment = HorizontalAlignment.RIGHT
            verticalAlignment = VerticalAlignment.CENTER
            dataFormat = currencyFormat
        }

        val totalStyle = workbook.createCellStyle().apply {
            val font = workbook.createFont().apply {
                bold = true
                fontHeightInPoints = 11.toShort()
            }
            setFont(font)
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.DOUBLE
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
            alignment = HorizontalAlignment.RIGHT
            dataFormat = currencyFormat
        }

        val totalLabelStyle = workbook.createCellStyle().apply {
            val font = workbook.createFont().apply {
                bold = true
                fontHeightInPoints = 11.toShort()
            }
            setFont(font)
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.DOUBLE
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
            alignment = HorizontalAlignment.LEFT
        }

        for (group in groups) {
            val sheetName = group.name.replace(Regex("[/\\\\?*\\[\\]]"), "_").take(31).ifBlank { "Group Expenses" }
            val sheet = workbook.createSheet(sheetName)
            sheet.isDisplayGridlines = true

            var rowNum = 0

            // Title Row
            val titleRow = sheet.createRow(rowNum++)
            titleRow.heightInPoints = 26f
            val titleCell = titleRow.createCell(0)
            titleCell.setCellValue("SPLITMATE EXPENSE REPORT")
            titleCell.cellStyle = titleStyle

            // Group Info Block
            val gNameRow = sheet.createRow(rowNum++)
            val gNameLbl = gNameRow.createCell(0)
            gNameLbl.setCellValue("Group Name:")
            gNameLbl.cellStyle = metaLabelStyle
            gNameRow.createCell(1).setCellValue(group.name)

            val gDescRow = sheet.createRow(rowNum++)
            val gDescLbl = gDescRow.createCell(0)
            gDescLbl.setCellValue("Currency:")
            gDescLbl.cellStyle = metaLabelStyle
            gDescRow.createCell(1).setCellValue("${group.currencyCode} (${group.currencySymbol})")

            val gDateRow = sheet.createRow(rowNum++)
            val gDateLbl = gDateRow.createCell(0)
            gDateLbl.setCellValue("Generated On:")
            gDateLbl.cellStyle = metaLabelStyle
            gDateRow.createCell(1).setCellValue(DateFormatter.formatDate(System.currentTimeMillis()))

            rowNum++ // Blank line

            // Section Header: EXPENSES TABLE
            val secExpRow = sheet.createRow(rowNum++)
            secExpRow.heightInPoints = 22f
            val secExpCell = secExpRow.createCell(0)
            secExpCell.setCellValue("  GROUP EXPENSES")
            secExpCell.cellStyle = subHeaderStyle

            // Column Headers
            val headers = arrayOf("Date", "Title", "Category", "Paid By", "Amount (${group.currencySymbol})", "Splits Breakdown", "Notes")
            val headerRow = sheet.createRow(rowNum++)
            headerRow.heightInPoints = 24f
            headers.forEachIndexed { colIdx, headerText ->
                val cell = headerRow.createCell(colIdx)
                cell.setCellValue(headerText)
                cell.cellStyle = headerStyle
            }

            val expenses = expensesMap[group.id] ?: emptyList()
            var totalExpenseAmount = 0.0

            expenses.forEachIndexed { idx, exp ->
                val row = sheet.createRow(rowNum++)
                row.heightInPoints = 20f
                val isEven = (idx % 2 == 0)
                val textStyle = if (isEven) cellStyleNormal else cellStyleZebra
                val numStyle = if (isEven) numberStyleNormal else numberStyleZebra

                totalExpenseAmount += exp.amount

                val splitsText = exp.splits.joinToString(", ") { "${it.userName}: ${group.currencySymbol}${String.format("%.2f", it.amount)}" }

                row.createCell(0).apply { setCellValue(DateFormatter.formatDate(exp.date)); cellStyle = textStyle }
                row.createCell(1).apply { setCellValue(exp.title); cellStyle = textStyle }
                row.createCell(2).apply { setCellValue(exp.category.name); cellStyle = textStyle }
                row.createCell(3).apply { setCellValue(exp.paidByUserName); cellStyle = textStyle }
                row.createCell(4).apply { setCellValue(exp.amount); cellStyle = numStyle }
                row.createCell(5).apply { setCellValue(splitsText); cellStyle = textStyle }
                row.createCell(6).apply { setCellValue(exp.notes); cellStyle = textStyle }
            }

            // Total Summary Row
            val totalRow = sheet.createRow(rowNum++)
            totalRow.heightInPoints = 22f
            totalRow.createCell(0).apply { setCellValue("Total"); cellStyle = totalLabelStyle }
            totalRow.createCell(1).apply { cellStyle = totalLabelStyle }
            totalRow.createCell(2).apply { cellStyle = totalLabelStyle }
            totalRow.createCell(3).apply { setCellValue("Grand Total:"); cellStyle = totalLabelStyle }
            totalRow.createCell(4).apply { setCellValue(totalExpenseAmount); cellStyle = totalStyle }
            totalRow.createCell(5).apply { cellStyle = totalLabelStyle }
            totalRow.createCell(6).apply { cellStyle = totalLabelStyle }

            rowNum++ // Blank line

            // Section Header: SETTLEMENTS TABLE
            val settlements = settlementsMap[group.id] ?: emptyList()
            if (settlements.isNotEmpty()) {
                val secSetRow = sheet.createRow(rowNum++)
                secSetRow.heightInPoints = 22f
                val secSetCell = secSetRow.createCell(0)
                secSetCell.setCellValue("  SETTLEMENTS & PAYMENTS")
                secSetCell.cellStyle = subHeaderStyle

                val setHeaders = arrayOf("Date", "Payer", "Recipient", "Amount (${group.currencySymbol})", "Method", "Notes")
                val setHeaderRow = sheet.createRow(rowNum++)
                setHeaderRow.heightInPoints = 24f
                setHeaders.forEachIndexed { colIdx, headerText ->
                    val cell = setHeaderRow.createCell(colIdx)
                    cell.setCellValue(headerText)
                    cell.cellStyle = headerStyle
                }

                settlements.forEachIndexed { idx, set ->
                    val row = sheet.createRow(rowNum++)
                    row.heightInPoints = 20f
                    val isEven = (idx % 2 == 0)
                    val textStyle = if (isEven) cellStyleNormal else cellStyleZebra
                    val numStyle = if (isEven) numberStyleNormal else numberStyleZebra

                    row.createCell(0).apply { setCellValue(DateFormatter.formatDate(set.date)); cellStyle = textStyle }
                    row.createCell(1).apply { setCellValue(set.payerName); cellStyle = textStyle }
                    row.createCell(2).apply { setCellValue(set.recipientName); cellStyle = textStyle }
                    row.createCell(3).apply { setCellValue(set.amount); cellStyle = numStyle }
                    row.createCell(4).apply { setCellValue(set.paymentMethod); cellStyle = textStyle }
                    row.createCell(5).apply { setCellValue(set.notes); cellStyle = textStyle }
                }
            }

            // Set reasonable column widths (autoSizeColumn crashes on Android due to missing AWT components)
            sheet.setColumnWidth(0, 3500) // Date
            sheet.setColumnWidth(1, 7000) // Title / Payer
            sheet.setColumnWidth(2, 5000) // Category / Recipient
            sheet.setColumnWidth(3, 5000) // Paid By / Amount
            sheet.setColumnWidth(4, 4000) // Amount / Method
            sheet.setColumnWidth(5, 12000) // Splits / Notes
            sheet.setColumnWidth(6, 8000) // Notes (Expenses only)
        }

        val outputFile = File(context.cacheDir, "SplitMate_Expenses_Report.xlsx")
        FileOutputStream(outputFile).use { out ->
            workbook.write(out)
        }
        workbook.close()

        return outputFile
    }
}
