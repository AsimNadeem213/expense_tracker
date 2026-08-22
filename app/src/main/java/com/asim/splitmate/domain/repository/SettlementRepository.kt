package com.asim.splitmate.domain.repository

import com.asim.splitmate.core.common.Resource
import com.asim.splitmate.domain.model.Settlement
import kotlinx.coroutines.flow.Flow

interface SettlementRepository {
    fun getSettlementsForGroup(groupId: String): Flow<List<Settlement>>
    fun getRecentSettlements(limit: Int = 10): Flow<List<Settlement>>
    suspend fun recordSettlement(settlement: Settlement): Resource<Settlement>
    suspend fun deleteSettlement(settlementId: String): Resource<Unit>
}
