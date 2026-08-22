package com.asim.splitmate.core.utils

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap

object QrCodeUtils {

    const val QR_PREFIX = "splitmate://join?code="

    /**
     * Formats an invite code into a standardized QR code payload URI.
     */
    fun formatInviteCodeQrPayload(inviteCode: String): String {
        val cleanCode = inviteCode.trim().uppercase()
        return "$QR_PREFIX$cleanCode"
    }

    /**
     * Parses an invite code from a QR code text payload.
     * Supports both URI format ("splitmate://join?code=TRIP1234") and raw invite code strings ("TRIP1234").
     */
    fun parseInviteCodeFromQr(scannedText: String): String {
        val trimmed = scannedText.trim()
        return when {
            trimmed.contains("code=") -> {
                trimmed.substringAfter("code=").substringBefore("&").trim().uppercase()
            }
            else -> trimmed.uppercase()
        }
    }

    /**
     * Generates an [ImageBitmap] QR code representing the given text content.
     */
    fun generateQrCodeBitmap(content: String, sizePx: Int = 512): ImageBitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
                put(EncodeHintType.MARGIN, 1)
            }
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap.asImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
