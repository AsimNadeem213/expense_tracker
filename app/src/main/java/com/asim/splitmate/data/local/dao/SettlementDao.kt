package com.asim.splitmate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.asim.splitmate.data.local.entity.SettlementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettlementDao {
    @Query("SELECT * FROM settlements WHERE groupId = :groupId ORDER BY date DESC")
    fun getSettlementsForGroup(groupId: String): Flow<List<SettlementEntity>>

    @Query("SELECT * FROM settlements WHERE groupId = :groupId ORDER BY date DESC")
    suspend fun getSettlementsForGroupSync(groupId: String): List<SettlementEntity>

    @Query("SELECT * FROM settlements ORDER BY date DESC LIMIT :limit")
    fun getRecentSettlements(limit: Int = 10): Flow<List<SettlementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: SettlementEntity)

    @Query("DELETE FROM settlements WHERE id = :settlementId")
    suspend fun deleteSettlement(settlementId: String)
}
