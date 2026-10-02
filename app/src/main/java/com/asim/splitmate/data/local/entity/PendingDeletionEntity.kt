package com.asim.splitmate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_deletions")
data class PendingDeletionEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val type: String // "EXPENSE", "GROUP", "SETTLEMENT"
)
