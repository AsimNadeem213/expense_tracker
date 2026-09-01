package com.asim.splitmate.data.repository

import com.asim.splitmate.core.common.Resource
import com.asim.splitmate.core.firebase.RealtimeDatabaseDataSource
import com.asim.splitmate.data.local.dao.SettlementDao
import com.asim.splitmate.data.local.entity.SettlementEntity
import com.asim.splitmate.domain.model.Settlement
import com.asim.splitmate.domain.repository.SettlementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettlementRepositoryImpl(
    private val settlementDao: SettlementDao,
    private val realtimeDatabaseDataSource: RealtimeDatabaseDataSource
) : SettlementRepository {

    override fun getSettlementsForGroup(groupId: String): Flow<List<Settlement>> {
        return settlementDao.getSettlementsForGroup(groupId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRecentSettlements(limit: Int): Flow<List<Settlement>> {
        return settlementDao.getRecentSettlements(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun recordSettlement(settlement: Settlement): Resource<Settlement> {
        return try {
            val entity = SettlementEntity.fromDomain(settlement)
            settlementDao.insertSettlement(entity)
            realtimeDatabaseDataSource.syncSettlement(settlement)
            Resource.Success(settlement)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to record settlement", e)
        }
    }

    override suspend fun deleteSettlement(settlementId: String): Resource<Unit> {
        return try {
            val settlement = settlementDao.getSettlementById(settlementId)
            settlementDao.deleteSettlement(settlementId)
            if (settlement != null) {
                realtimeDatabaseDataSource.deleteSettlement(settlement.groupId, settlementId)
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete settlement", e)
        }
    }
}
