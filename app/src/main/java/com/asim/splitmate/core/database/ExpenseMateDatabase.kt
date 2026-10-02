package com.asim.splitmate.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.asim.splitmate.data.local.dao.ExpenseDao
import com.asim.splitmate.data.local.dao.GroupDao
import com.asim.splitmate.data.local.dao.SettlementDao
import com.asim.splitmate.data.local.dao.SyncDao
import com.asim.splitmate.data.local.dao.UserDao
import com.asim.splitmate.data.local.entity.ExpenseEntity
import com.asim.splitmate.data.local.entity.ExpenseSplitEntity
import com.asim.splitmate.data.local.entity.GroupEntity
import com.asim.splitmate.data.local.entity.GroupMemberCrossRef
import com.asim.splitmate.data.local.entity.PendingDeletionEntity
import com.asim.splitmate.data.local.entity.SettlementEntity
import com.asim.splitmate.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        GroupEntity::class,
        GroupMemberCrossRef::class,
        ExpenseEntity::class,
        ExpenseSplitEntity::class,
        SettlementEntity::class,
        PendingDeletionEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ExpenseMateDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun groupDao(): GroupDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun settlementDao(): SettlementDao
    abstract fun syncDao(): SyncDao
}
