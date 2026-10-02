package com.asim.splitmate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.asim.splitmate.data.local.entity.PendingDeletionEntity

@Dao
interface SyncDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordPendingDeletion(item: PendingDeletionEntity)

    @Query("SELECT * FROM pending_deletions")
    suspend fun getAllPendingDeletions(): List<PendingDeletionEntity>

    @Query("DELETE FROM pending_deletions WHERE id = :id")
    suspend fun removePendingDeletion(id: String)
}
