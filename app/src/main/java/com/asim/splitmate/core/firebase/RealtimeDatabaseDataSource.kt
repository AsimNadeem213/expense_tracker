package com.asim.splitmate.core.firebase

import com.asim.splitmate.core.common.Resource
import com.asim.splitmate.data.local.dao.ExpenseDao
import com.asim.splitmate.data.local.dao.GroupDao
import com.asim.splitmate.data.local.dao.SettlementDao
import com.asim.splitmate.data.local.dao.UserDao
import com.asim.splitmate.data.local.entity.ExpenseEntity
import com.asim.splitmate.data.local.entity.ExpenseSplitEntity
import com.asim.splitmate.data.local.entity.GroupEntity
import com.asim.splitmate.data.local.entity.GroupMemberCrossRef
import com.asim.splitmate.data.local.entity.SettlementEntity
import com.asim.splitmate.data.local.entity.UserEntity
import com.asim.splitmate.domain.model.Category
import com.asim.splitmate.domain.model.Expense
import com.asim.splitmate.domain.model.Group
import com.asim.splitmate.domain.model.GroupType
import com.asim.splitmate.domain.model.Settlement
import com.asim.splitmate.domain.model.SplitType
import com.asim.splitmate.domain.model.User
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class RealtimeDatabaseDataSource {
    private val db get() = FirebaseHelper.database

    suspend fun fetchAndSyncRemoteData(
        userId: String,
        userName: String = "",
        groupDao: GroupDao,
        userDao: UserDao,
        expenseDao: ExpenseDao,
        settlementDao: SettlementDao
    ) {
        try {
            val database = db ?: return
            val dbRef = database.getReference("groups")

            withTimeoutOrNull(4000L) {
                val snapshot = dbRef.get().await() ?: return@withTimeoutOrNull

                for (groupSnap in snapshot.children) {
                    val groupId = groupSnap.child("id").getValue(String::class.java) ?: groupSnap.key ?: continue
                    val name = groupSnap.child("name").getValue(String::class.java) ?: continue
                    val description = groupSnap.child("description").getValue(String::class.java) ?: ""
                    val typeStr = groupSnap.child("type").getValue(String::class.java) ?: "OTHER"
                    val type = try { GroupType.valueOf(typeStr) } catch (_: Exception) { GroupType.OTHER }
                    val currencySymbol = groupSnap.child("currencySymbol").getValue(String::class.java) ?: "Rs"
                    val currencyCode = groupSnap.child("currencyCode").getValue(String::class.java) ?: "PKR"
                    val createdBy = groupSnap.child("createdBy").getValue(String::class.java) ?: ""
                    val createdAt = groupSnap.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
                    val inviteCode = groupSnap.child("inviteCode").getValue(String::class.java) ?: ""

                    val memberIdsList = mutableListOf<String>()
                    val memberIdsSnap = groupSnap.child("memberIds")
                    for (mIdSnap in memberIdsSnap.children) {
                        val mId = mIdSnap.getValue(String::class.java)
                        if (mId != null) memberIdsList.add(mId)
                    }

                    val memberNamesList = mutableListOf<String>()
                    val memberNamesSnap = groupSnap.child("memberNames")
                    for (mNameSnap in memberNamesSnap.children) {
                        val mName = mNameSnap.getValue(String::class.java)
                        if (mName != null) memberNamesList.add(mName)
                    }

                    val isUserMember = createdBy == userId ||
                            memberIdsList.contains(userId) ||
                            (userName.isNotBlank() && memberNamesList.any { it.equals(userName, ignoreCase = true) }) ||
                            memberIdsList.isEmpty()

                    if (isUserMember) {
                        val groupEntity = GroupEntity(
                            id = groupId,
                            name = name,
                            description = description,
                            type = type.name,
                            currencySymbol = currencySymbol,
                            currencyCode = currencyCode,
                            createdBy = createdBy,
                            createdAt = createdAt,
                            inviteCode = inviteCode
                        )
                        groupDao.insertGroup(groupEntity)

                        val membersSnap = groupSnap.child("members")
                        val membersList = mutableListOf<UserEntity>()
                        val resolvedMemberIds = mutableListOf<String>()

                        if (membersSnap.children.count() > 0) {
                            for (mSnap in membersSnap.children) {
                                val mId = mSnap.child("id").getValue(String::class.java) ?: mSnap.key ?: continue
                                val mName = mSnap.child("name").getValue(String::class.java) ?: "Member"
                                val mEmail = mSnap.child("email").getValue(String::class.java) ?: ""
                                val isCurrent = (mId == userId)

                                val userEntity = UserEntity(
                                    id = mId,
                                    name = mName,
                                    email = mEmail,
                                    isCurrentUser = isCurrent
                                )
                                userDao.insertUser(userEntity)
                                membersList.add(userEntity)
                                resolvedMemberIds.add(mId)
                            }
                        } else {
                            var idx = 0
                            for (mId in memberIdsList) {
                                val mName = memberNamesList.getOrNull(idx) ?: "Member"
                                val isCurrent = (mId == userId)

                                val userEntity = UserEntity(
                                    id = mId,
                                    name = mName,
                                    email = "",
                                    isCurrentUser = isCurrent
                                )
                                userDao.insertUser(userEntity)
                                membersList.add(userEntity)
                                resolvedMemberIds.add(mId)
                                idx++
                            }
                        }

                        if (resolvedMemberIds.isEmpty()) {
                            val selfName = if (userName.isNotBlank()) userName else "Member"
                            val selfUser = UserEntity(id = userId, name = selfName, email = "", isCurrentUser = true)
                            userDao.insertUser(selfUser)
                            membersList.add(selfUser)
                            resolvedMemberIds.add(userId)
                        }

                        val crossRefs = resolvedMemberIds.map { GroupMemberCrossRef(groupId = groupId, userId = it) }
                        groupDao.insertGroupMembers(crossRefs)

                        // Fetch Expenses under this group
                        val expensesSnap = groupSnap.child("expenses")
                        for (expSnap in expensesSnap.children) {
                            val expId = expSnap.child("id").getValue(String::class.java) ?: expSnap.key ?: continue
                            val title = expSnap.child("title").getValue(String::class.java) ?: "Expense"
                            val amount = expSnap.child("amount").getValue(Double::class.java) ?: 0.0
                            val catId = expSnap.child("categoryId").getValue(String::class.java) ?: "other"
                            val category = Category.fromId(catId)
                            val paidByUserId = expSnap.child("paidByUserId").getValue(String::class.java) ?: userId
                            val paidByUserName = expSnap.child("paidByUserName").getValue(String::class.java) ?: "Payer"
                            val date = expSnap.child("date").getValue(Long::class.java) ?: System.currentTimeMillis()
                            val splitTypeStr = expSnap.child("splitType").getValue(String::class.java) ?: "EQUAL"
                            val splitType = try { SplitType.valueOf(splitTypeStr) } catch (_: Exception) { SplitType.EQUAL }
                            val notes = expSnap.child("notes").getValue(String::class.java) ?: ""

                            val expenseEntity = ExpenseEntity(
                                id = expId,
                                groupId = groupId,
                                title = title,
                                amount = amount,
                                categoryId = category.id,
                                paidByUserId = paidByUserId,
                                paidByUserName = paidByUserName,
                                date = date,
                                splitType = splitType.name,
                                notes = notes
                            )
                            expenseDao.insertExpense(expenseEntity)

                            val splitAmount = if (membersList.isNotEmpty()) amount / membersList.size else amount
                            val splitEntities = membersList.map { m ->
                                ExpenseSplitEntity(
                                    expenseId = expId,
                                    userId = m.id,
                                    userName = m.name,
                                    amount = splitAmount
                                )
                            }
                            expenseDao.insertSplits(splitEntities)
                        }

                        // Fetch Settlements under this group
                        val settlementsSnap = groupSnap.child("settlements")
                        for (setSnap in settlementsSnap.children) {
                            val setId = setSnap.child("id").getValue(String::class.java) ?: setSnap.key ?: continue
                            val payerId = setSnap.child("payerId").getValue(String::class.java) ?: ""
                            val payerName = setSnap.child("payerName").getValue(String::class.java) ?: ""
                            val recipientId = setSnap.child("recipientId").getValue(String::class.java) ?: ""
                            val recipientName = setSnap.child("recipientName").getValue(String::class.java) ?: ""
                            val setAmount = setSnap.child("amount").getValue(Double::class.java) ?: 0.0
                            val setDate = setSnap.child("date").getValue(Long::class.java) ?: System.currentTimeMillis()
                            val paymentMethod = setSnap.child("paymentMethod").getValue(String::class.java) ?: "Cash"
                            val notes = setSnap.child("notes").getValue(String::class.java) ?: ""

                            val settlementEntity = SettlementEntity(
                                id = setId,
                                groupId = groupId,
                                payerId = payerId,
                                payerName = payerName,
                                recipientId = recipientId,
                                recipientName = recipientName,
                                amount = setAmount,
                                date = setDate,
                                paymentMethod = paymentMethod,
                                notes = notes
                            )
                            settlementDao.insertSettlement(settlementEntity)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun joinGroupWithInviteCode(
        inviteCode: String,
        userId: String,
        userName: String,
        groupDao: GroupDao,
        userDao: UserDao,
        expenseDao: ExpenseDao,
        settlementDao: SettlementDao
    ): Resource<Group> {
        return try {
            val database = db ?: return Resource.Error("Firebase not initialized")
            val snapshot = withTimeoutOrNull(5000L) {
                database.getReference("groups").get().await()
            } ?: return Resource.Error("Network error or timeout. Please check internet connection.")

            for (groupSnap in snapshot.children) {
                val code = groupSnap.child("inviteCode").getValue(String::class.java) ?: continue
                if (code.equals(inviteCode.trim(), ignoreCase = true)) {
                    val groupId = groupSnap.child("id").getValue(String::class.java) ?: groupSnap.key ?: continue

                    val groupRef = database.getReference("groups").child(groupId)

                    val existingMemberIdsSnap = groupRef.child("memberIds").get().await()
                    val existingIds = mutableListOf<String>()
                    for (mSnap in existingMemberIdsSnap.children) {
                        mSnap.getValue(String::class.java)?.let { existingIds.add(it) }
                    }
                    if (!existingIds.contains(userId)) {
                        existingIds.add(userId)
                        groupRef.child("memberIds").setValue(existingIds)
                    }

                    val existingMemberNamesSnap = groupRef.child("memberNames").get().await()
                    val existingNames = mutableListOf<String>()
                    for (nSnap in existingMemberNamesSnap.children) {
                        nSnap.getValue(String::class.java)?.let { existingNames.add(it) }
                    }
                    val nameToUse = if (userName.isNotBlank()) userName else "Member"
                    if (!existingNames.contains(nameToUse)) {
                        existingNames.add(nameToUse)
                        groupRef.child("memberNames").setValue(existingNames)
                    }

                    fetchAndSyncRemoteData(userId, userName, groupDao, userDao, expenseDao, settlementDao)

                    val groupEntity = groupDao.getGroupByIdSync(groupId)
                    if (groupEntity != null) {
                        val members = groupDao.getGroupMembersSync(groupId).map { it.toDomain() }
                        val expenses = expenseDao.getExpensesForGroupSync(groupId)
                        val totalSpent = expenses.sumOf { it.amount }
                        return Resource.Success(groupEntity.toDomain(members, totalSpent))
                    }
                }
            }
            Resource.Error("No group found with invite code '$inviteCode'")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to join group with invite code")
        }
    }

    fun syncUser(user: User) {
        try {
            db?.getReference("users")?.child(user.id)?.setValue(
                mapOf(
                    "id" to user.id,
                    "name" to user.name,
                    "email" to user.email,
                    "avatarUrl" to user.avatarUrl,
                    "phoneNumber" to user.phoneNumber
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun syncGroup(group: Group) {
        try {
            db?.getReference("groups")?.child(group.id)?.setValue(
                mapOf(
                    "id" to group.id,
                    "name" to group.name,
                    "description" to group.description,
                    "type" to group.type.name,
                    "currencySymbol" to group.currencySymbol,
                    "currencyCode" to group.currencyCode,
                    "createdBy" to group.createdBy,
                    "createdAt" to group.createdAt,
                    "inviteCode" to group.inviteCode,
                    "memberIds" to group.members.map { it.id },
                    "memberNames" to group.members.map { it.name },
                    "members" to group.members.map { m ->
                        mapOf(
                            "id" to m.id,
                            "name" to m.name,
                            "email" to m.email
                        )
                    }
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun syncExpense(expense: Expense) {
        try {
            db?.getReference("groups")?.child(expense.groupId)
                ?.child("expenses")?.child(expense.id)?.setValue(
                    mapOf(
                        "id" to expense.id,
                        "groupId" to expense.groupId,
                        "title" to expense.title,
                        "amount" to expense.amount,
                        "categoryId" to expense.category.id,
                        "paidByUserId" to expense.paidByUserId,
                        "paidByUserName" to expense.paidByUserName,
                        "date" to expense.date,
                        "splitType" to expense.splitType.name,
                        "notes" to expense.notes
                    )
                )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun syncSettlement(settlement: Settlement) {
        try {
            db?.getReference("groups")?.child(settlement.groupId)
                ?.child("settlements")?.child(settlement.id)?.setValue(
                    mapOf(
                        "id" to settlement.id,
                        "groupId" to settlement.groupId,
                        "payerId" to settlement.payerId,
                        "payerName" to settlement.payerName,
                        "recipientId" to settlement.recipientId,
                        "recipientName" to settlement.recipientName,
                        "amount" to settlement.amount,
                        "date" to settlement.date,
                        "paymentMethod" to settlement.paymentMethod,
                        "notes" to settlement.notes
                    )
                )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteGroup(groupId: String) {
        try {
            db?.getReference("groups")?.child(groupId)?.removeValue()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteExpense(groupId: String, expenseId: String) {
        try {
            db?.getReference("groups")?.child(groupId)?.child("expenses")?.child(expenseId)?.removeValue()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
