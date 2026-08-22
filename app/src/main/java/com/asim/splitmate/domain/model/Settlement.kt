package com.asim.splitmate.domain.model

data class Settlement(
    val id: String,
    val groupId: String,
    val payerId: String,
    val payerName: String,
    val recipientId: String,
    val recipientName: String,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val paymentMethod: String = "Cash / UPI",
    val notes: String = ""
)
