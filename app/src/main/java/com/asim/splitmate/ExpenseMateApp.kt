package com.asim.splitmate

import android.app.Application
import com.asim.splitmate.data.local.dao.ExpenseDao
import com.asim.splitmate.data.local.dao.GroupDao
import com.asim.splitmate.data.local.dao.UserDao
import com.asim.splitmate.data.local.entity.ExpenseEntity
import com.asim.splitmate.data.local.entity.ExpenseSplitEntity
import com.asim.splitmate.data.local.entity.GroupEntity
import com.asim.splitmate.data.local.entity.GroupMemberCrossRef
import com.asim.splitmate.data.local.entity.UserEntity
import com.asim.splitmate.di.appModule
import com.asim.splitmate.domain.model.Category
import com.asim.splitmate.domain.model.GroupType
import com.asim.splitmate.domain.model.SplitType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.getKoin

class ExpenseMateApp : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@ExpenseMateApp)
            modules(appModule)
        }

        com.asim.splitmate.core.notification.NotificationHelper.createNotificationChannel(this)
    }

    private suspend fun seedSampleDataIfEmpty(userDao: UserDao, groupDao: GroupDao, expenseDao: ExpenseDao) {
        if (userDao.getCurrentUserSync() != null) return

        val currentUser = UserEntity(
            id = "usr_you",
            name = "You",
            email = "you@splitmate.app",
            isCurrentUser = true
        )
        val ali = UserEntity(id = "usr_ali", name = "Ali", email = "ali@example.com")
        val sarah = UserEntity(id = "usr_sarah", name = "Sarah", email = "sarah@example.com")

        userDao.insertUsers(listOf(currentUser, ali, sarah))

        val group1 = GroupEntity(
            id = "grp_goa",
            name = "Goa Trip 🌴",
            description = "Weekend getaway to North Goa",
            type = GroupType.TRIP.name,
            currencySymbol = "₹",
            currencyCode = "INR",
            createdBy = "usr_you",
            createdAt = System.currentTimeMillis() - 86400000 * 3,
            inviteCode = "GOA2026"
        )

        groupDao.insertGroup(group1)
        groupDao.insertGroupMembers(
            listOf(
                GroupMemberCrossRef("grp_goa", "usr_you"),
                GroupMemberCrossRef("grp_goa", "usr_ali"),
                GroupMemberCrossRef("grp_goa", "usr_sarah")
            )
        )

        // Expense 1: Dinner paid by You (Rs 2000 split equally)
        val exp1 = ExpenseEntity(
            id = "exp_1",
            groupId = "grp_goa",
            title = "Seafood Dinner at Brittos",
            amount = 2000.0,
            categoryId = Category.FOOD.id,
            paidByUserId = "usr_you",
            paidByUserName = "You",
            date = System.currentTimeMillis() - 86400000 * 2,
            splitType = SplitType.EQUAL.name,
            notes = "Delicious dinner on beach",
        )
        val splits1 = listOf(
            ExpenseSplitEntity("exp_1", "usr_you", "You", 666.67, 33.33, 1),
            ExpenseSplitEntity("exp_1", "usr_ali", "Ali", 666.67, 33.33, 1),
            ExpenseSplitEntity("exp_1", "usr_sarah", "Sarah", 666.66, 33.33, 1)
        )
        expenseDao.insertExpense(exp1)
        expenseDao.insertSplits(splits1)

        // Expense 2: Taxi paid by Ali (Rs 1200 split equally)
        val exp2 = ExpenseEntity(
            id = "exp_2",
            groupId = "grp_goa",
            title = "Airport Cab Transfer",
            amount = 1200.0,
            categoryId = Category.TRANSPORT.id,
            paidByUserId = "usr_ali",
            paidByUserName = "Ali",
            date = System.currentTimeMillis() - 86400000,
            splitType = SplitType.EQUAL.name,
            notes = "Airport to resort",
        )
        val splits2 = listOf(
            ExpenseSplitEntity("exp_2", "usr_you", "You", 400.0, 33.33, 1),
            ExpenseSplitEntity("exp_2", "usr_ali", "Ali", 400.0, 33.33, 1),
            ExpenseSplitEntity("exp_2", "usr_sarah", "Sarah", 400.0, 33.33, 1)
        )
        expenseDao.insertExpense(exp2)
        expenseDao.insertSplits(splits2)
    }

}