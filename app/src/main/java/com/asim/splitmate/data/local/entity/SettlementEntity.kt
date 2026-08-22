package com.asim.splitmate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.asim.splitmate.domain.model.Settlement

@Entity(tableName = "settlements")
data class SettlementEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val payerId: String,
    val payerName: String,
    val recipientId: String,
    val recipientName: String,
    val amount: Double,
    val date: Long,
    val paymentMethod: String,
    val notes: String
) {
    fun toDomain(): Settlement = Settlement(
        id = id,
        groupId = groupId,
        payerId = payerId,
        payerName = payerName,
        recipientId = recipientId,
        recipientName = recipientName,
        amount = amount,
        date = date,
        paymentMethod = paymentMethod,
        notes = notes
    )

    companion object {
        fun fromDomain(settlement: Settlement): SettlementEntity = SettlementEntity(
            id = settlement.id,
            groupId = settlement.groupId,
            payerId = settlement.payerId,
            payerName = settlement.payerName,
            recipientId = settlement.recipientId,
            recipientName = settlement.recipientName,
            amount = settlement.amount,
            date = settlement.date,
            paymentMethod = settlement.paymentMethod,
            notes = settlement.notes
        )
    }
}
