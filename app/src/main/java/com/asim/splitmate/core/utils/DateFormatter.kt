package com.asim.splitmate.core.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormatter {
    private val fullDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val monthDayFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

    fun formatDate(timestamp: Long): String {
        return fullDateFormat.format(Date(timestamp))
    }

    fun formatMonthDay(timestamp: Long): String {
        return monthDayFormat.format(Date(timestamp))
    }

    fun formatRelative(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days == 0L && hours < 1 -> "Just now"
            days == 0L && hours < 24 -> "${hours}h ago"
            days == 1L -> "Yesterday"
            days < 7L -> "${days}d ago"
            else -> formatDate(timestamp)
        }
    }
}
