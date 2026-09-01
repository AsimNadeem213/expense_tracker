package com.asim.splitmate.core.firebase

import android.util.Log
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class RealtimeDatabaseDataSource(
    private val context: android.content.Context? = null,
    private val networkMonitor: com.asim.splitmate.core.network.NetworkMonitor? = null
) {
    private val db get() = FirebaseHelper.database

    private var isRealtimeSyncStarted = false

    fun startRealtimeSync(
        userId: String,
        userName: String = "",
        groupDao: GroupDao,
        userDao: UserDao,
        expenseDao: ExpenseDao,
        settlementDao: SettlementDao,
        coroutineScope: kotlinx.coroutines.CoroutineScope
    ) {
        if (isRealtimeSyncStarted) return
        val database = db ?: return

        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    fetchAndSyncRemoteData(
                        userId = userId,
                        userName = userName,
                        groupDao = groupDao,
                        userDao = userDao,
                        expenseDao = expenseDao,
                        settlementDao = settlementDao
                    )
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Log.e("FirebaseSync", "Realtime sync listener cancelled: ${error.message}")
            }
        }

        database.getReference("groups").addValueEventListener(listener)
        isRealtimeSyncStarted = true
        Log.d("FirebaseSync", "Realtime Firebase listener registered successfully for /groups node!")
    }

    suspend fun fetchAndSyncRemoteData(
        userId: String,
        userName: String = "",
        groupDao: GroupDao,
        userDao: UserDao,
        expenseDao: ExpenseDao,
        settlementDao: SettlementDao
    ) {
        if (networkMonitor?.isCurrentlyOnline() == false) {
            Log.d("FirebaseSync", "Offline: Skipping remote fetchAndSyncRemoteData")
            return
        }
        try {
            val database = db ?: return
            val dbRef = database.getReference("groups")

            val snapshot = withTimeoutOrNull(5000L) {
                dbRef.get().await()
            } ?: return

            val remoteGroupIdsForUser = mutableSetOf<String>()
            val remoteExpenseIdsByGroup = mutableMapOf<String, MutableSet<String>>()
            val remoteSettlementIdsByGroup = mutableMapOf<String, MutableSet<String>>()

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

                val currentUid = FirebaseHelper.currentUserId ?: userId
                val membersSnap = groupSnap.child("members")
                val memberKeys = membersSnap.children.mapNotNull { it.child("id").getValue(String::class.java) ?: it.key }.toSet()

                val isUserMember = createdBy == currentUid ||
                        memberIdsList.contains(currentUid) ||
                        memberKeys.contains(currentUid)

                if (!isUserMember) {
                    continue
                }

                remoteGroupIdsForUser.add(groupId)
                com.asim.splitmate.core.notification.NotificationHelper.subscribeToGroupTopic(groupId)

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

                val membersList = mutableListOf<UserEntity>()
                val resolvedMemberIds = mutableListOf<String>()

                if (membersSnap.children.count() > 0) {
                    for (mSnap in membersSnap.children) {
                        val key = mSnap.key
                        if (key != null && key.toIntOrNull() != null && membersSnap.childrenCount > 1) {
                            continue
                        }
                        val mId = mSnap.child("id").getValue(String::class.java) ?: key ?: continue
                        if (mId.isBlank() || mId.toIntOrNull() != null) continue

                        val mName = mSnap.child("name").getValue(String::class.java) ?: "Member"
                        val mEmail = mSnap.child("email").getValue(String::class.java) ?: ""
                        val isCurrent = (mId == userId)

                        val existingUser = userDao.getUserById(mId)
                        val currentUserDb = userDao.getCurrentUserSync()
                        val finalEmail = when {
                            mEmail.isNotBlank() -> mEmail
                            existingUser != null && existingUser.email.isNotBlank() -> existingUser.email
                            isCurrent && currentUserDb != null && currentUserDb.email.isNotBlank() -> currentUserDb.email
                            else -> ""
                        }
                        val finalIsCurrent = isCurrent || (existingUser?.isCurrentUser == true) || (currentUserDb?.id == mId)

                        val userEntity = UserEntity(
                            id = mId,
                            name = mName,
                            email = finalEmail,
                            isCurrentUser = finalIsCurrent
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

                        val existingUser = userDao.getUserById(mId)
                        val currentUserDb = userDao.getCurrentUserSync()
                        val finalEmail = when {
                            existingUser != null && existingUser.email.isNotBlank() -> existingUser.email
                            isCurrent && currentUserDb != null && currentUserDb.email.isNotBlank() -> currentUserDb.email
                            else -> ""
                        }
                        val finalIsCurrent = isCurrent || (existingUser?.isCurrentUser == true) || (currentUserDb?.id == mId)

                        val userEntity = UserEntity(
                            id = mId,
                            name = mName,
                            email = finalEmail,
                            isCurrentUser = finalIsCurrent
                        )
                        userDao.insertUser(userEntity)
                        membersList.add(userEntity)
                        resolvedMemberIds.add(mId)
                        idx++
                    }
                }

                if (resolvedMemberIds.isEmpty()) {
                    val selfName = if (userName.isNotBlank()) userName else "Member"
                    val currentUserDb = userDao.getCurrentUserSync()
                    val selfEmail = currentUserDb?.email?.takeIf { it.isNotBlank() } ?: "asim@splitmate.app"
                    val selfUser = UserEntity(id = userId, name = selfName, email = selfEmail, isCurrentUser = true)
                    userDao.insertUser(selfUser)
                    membersList.add(selfUser)
                    resolvedMemberIds.add(userId)
                }

                val crossRefs = resolvedMemberIds.distinct().map { GroupMemberCrossRef(groupId = groupId, userId = it) }
                groupDao.insertGroupMembers(crossRefs)

                // Fetch Expenses under this group
                val remoteExpensesForThisGroup = mutableSetOf<String>()
                val expensesSnap = groupSnap.child("expenses")
                for (expSnap in expensesSnap.children) {
                    val expId = expSnap.child("id").getValue(String::class.java) ?: expSnap.key ?: continue
                    remoteExpensesForThisGroup.add(expId)

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
                    val createdByExp = expSnap.child("createdBy").getValue(String::class.java) ?: paidByUserId
                    val isEdited = expSnap.child("isEdited").getValue(Boolean::class.java) ?: false

                    val existingExp = expenseDao.getExpenseById(expId)
                    val currentUid = FirebaseHelper.currentUserId ?: userId
                    val isNewRemoteExpense = (existingExp == null) && (paidByUserId != currentUid) && (createdByExp != currentUid)

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
                        notes = notes,
                        createdBy = createdByExp,
                        isEdited = isEdited
                    )
                    expenseDao.insertExpense(expenseEntity)

                    if (isNewRemoteExpense && context != null) {
                        com.asim.splitmate.core.notification.NotificationHelper.showExpenseAddedNotification(
                            context = context,
                            groupName = name,
                            expenseTitle = title,
                            amount = amount,
                            currencySymbol = currencySymbol,
                            paidByName = paidByUserName
                        )
                    }

                    val splitsSnap = expSnap.child("splits")
                    val remoteSplits = mutableListOf<ExpenseSplitEntity>()
                    if (splitsSnap.children.count() > 0) {
                        for (sSnap in splitsSnap.children) {
                            val sUserId = sSnap.child("userId").getValue(String::class.java)
                                ?: sSnap.child("id").getValue(String::class.java) ?: continue
                            val sUserName = sSnap.child("userName").getValue(String::class.java)
                                ?: sSnap.child("name").getValue(String::class.java) ?: "Member"
                            val sAmount = sSnap.child("amount").getValue(Double::class.java) ?: 0.0
                            val sPercentage = sSnap.child("percentage").getValue(Double::class.java) ?: 0.0
                            val sShares = sSnap.child("shares").getValue(Int::class.java) ?: 1
                            remoteSplits.add(
                                ExpenseSplitEntity(
                                    expenseId = expId,
                                    userId = sUserId,
                                    userName = sUserName,
                                    amount = sAmount,
                                    percentage = sPercentage,
                                    shares = sShares
                                )
                            )
                        }
                    }

                    if (remoteSplits.isNotEmpty()) {
                        expenseDao.deleteSplitsForExpense(expId)
                        expenseDao.insertSplits(remoteSplits)
                    } else {
                        val existingSplits = expenseDao.getSplitsForExpense(expId)
                        if (existingSplits.isEmpty()) {
                            val domainMembers = membersList.map { it.toDomain() }
                            val computedSplits = com.asim.splitmate.core.utils.SplitCalculator.calculateSplits(
                                totalAmount = amount,
                                splitType = splitType,
                                selectedMembers = domainMembers
                            )
                            val fallbackEntities = computedSplits.map { ExpenseSplitEntity.fromDomain(expId, it) }
                            expenseDao.insertSplits(fallbackEntities)
                        }
                    }
                }
                remoteExpenseIdsByGroup[groupId] = remoteExpensesForThisGroup

                // Fetch Settlements under this group
                val remoteSettlementsForThisGroup = mutableSetOf<String>()
                val settlementsSnap = groupSnap.child("settlements")
                for (setSnap in settlementsSnap.children) {
                    val setId = setSnap.child("id").getValue(String::class.java) ?: setSnap.key ?: continue
                    remoteSettlementsForThisGroup.add(setId)
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
                remoteSettlementIdsByGroup[groupId] = remoteSettlementsForThisGroup
            }

            // -------------------------------------------------------------
            // PURGE DELETED GROUPS, EXPENSES & SETTLEMENTS FROM LOCAL ROOM DB
            // -------------------------------------------------------------
            val localGroups = groupDao.getAllGroupsSync()
            for (localGroup in localGroups) {
                if (!remoteGroupIdsForUser.contains(localGroup.id)) {
                    // Group deleted on Firebase! Purge locally!
                    groupDao.deleteGroupMembersForGroup(localGroup.id)
                    expenseDao.deleteExpensesForGroup(localGroup.id)
                    settlementDao.deleteSettlementsForGroup(localGroup.id)
                    groupDao.deleteGroup(localGroup.id)
                    Log.d("FirebaseSync", "Purged remotely deleted group from Room DB: ${localGroup.id}")
                } else {
                    // Group still exists. Purge deleted expenses & settlements for this group!
                    val remoteExpenseIds = remoteExpenseIdsByGroup[localGroup.id] ?: emptySet()
                    val localExpenses = expenseDao.getExpensesForGroupSync(localGroup.id)
                    for (localExp in localExpenses) {
                        if (!remoteExpenseIds.contains(localExp.id)) {
                            expenseDao.deleteSplitsForExpense(localExp.id)
                            expenseDao.deleteExpense(localExp.id)
                            Log.d("FirebaseSync", "Purged remotely deleted expense from Room DB: ${localExp.id}")
                        }
                    }

                    val remoteSettlementIds = remoteSettlementIdsByGroup[localGroup.id] ?: emptySet()
                    val localSettlements = settlementDao.getSettlementsForGroupSync(localGroup.id)
                    for (localSet in localSettlements) {
                        if (!remoteSettlementIds.contains(localSet.id)) {
                            settlementDao.deleteSettlement(localSet.id)
                            Log.d("FirebaseSync", "Purged remotely deleted settlement from Room DB: ${localSet.id}")
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
        userEmail: String = "",
        groupDao: GroupDao,
        userDao: UserDao,
        expenseDao: ExpenseDao,
        settlementDao: SettlementDao
    ): Resource<Group> {
        val cleanCode = inviteCode.trim().uppercase()
        if (cleanCode.isBlank()) return Resource.Error("Invite code cannot be empty")
        if (networkMonitor?.isCurrentlyOnline() == false) {
            return Resource.Error("No internet connection available. Please turn on Wi-Fi or Mobile Data to join group.")
        }

        return try {
            val database = db ?: return Resource.Error("Firebase not initialized")

            // Ensure Firebase Auth session exists (Anonymous auth fallback if null)
            val auth = FirebaseHelper.auth
            if (auth != null && auth.currentUser == null) {
                try {
                    auth.signInAnonymously().await()
                } catch (e: Exception) {
                    Log.e("FirebaseSync", "Anonymous auth failed: ${e.message}")
                }
            }

            val activeUserId = FirebaseHelper.currentUserId ?: userId

            // 1. Direct O(1) lookup via /inviteCodes/{cleanCode}
            var targetGroupId: String? = null
            try {
                val codeSnap = withTimeoutOrNull(3000L) {
                    database.getReference("inviteCodes").child(cleanCode).get().await()
                }
                targetGroupId = codeSnap?.getValue(String::class.java)
            } catch (e: Exception) {
                Log.e("FirebaseSync", "Direct invite code lookup failed: ${e.message}")
            }

            // 2. Fallback scan across /groups if inviteCodes index is missing
            if (targetGroupId.isNullOrBlank()) {
                try {
                    val groupsSnap = withTimeoutOrNull(5000L) {
                        database.getReference("groups").get().await()
                    }
                    if (groupsSnap != null) {
                        for (groupSnap in groupsSnap.children) {
                            val code = groupSnap.child("inviteCode").getValue(String::class.java) ?: continue
                            if (code.equals(cleanCode, ignoreCase = true)) {
                                targetGroupId = groupSnap.child("id").getValue(String::class.java) ?: groupSnap.key
                                break
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("FirebaseSync", "Groups scan fallback failed: ${e.message}")
                }
            }

            if (targetGroupId.isNullOrBlank()) {
                return Resource.Error("No group found with invite code '$cleanCode'")
            }

            // Register user in the remote group node
            val groupRef = database.getReference("groups").child(targetGroupId)

            val existingMemberIdsSnap = try {
                groupRef.child("memberIds").get().await()
            } catch (e: Exception) {
                Log.e("FirebaseSync", "Failed to fetch memberIds: ${e.message}")
                null
            }

            val existingIds = mutableListOf<String>()
            if (existingMemberIdsSnap != null) {
                for (mSnap in existingMemberIdsSnap.children) {
                    mSnap.getValue(String::class.java)?.let { existingIds.add(it) }
                }
            }

            if (!existingIds.contains(activeUserId)) {
                // Check if group already has recorded expenses on Firebase or in local Room database
                val expensesSnap = try { groupRef.child("expenses").get().await() } catch (_: Exception) { null }
                val localExpenses = expenseDao.getExpensesForGroupSync(targetGroupId)
                val remoteExpenseCount = expensesSnap?.childrenCount ?: 0L

                if (remoteExpenseCount > 0 || localExpenses.isNotEmpty()) {
                    return Resource.Error("New members cannot join this group because expenses have already been recorded.")
                }
                existingIds.add(activeUserId)
                try {
                    groupRef.child("memberIds").setValue(existingIds).await()
                } catch (e: Exception) {
                    Log.e("FirebaseSync", "Failed to set memberIds: ${e.message}")
                }
            }

            val currentUserDb = userDao.getCurrentUserSync()
            val nameToUse = if (userName.isNotBlank() && userName != "User" && userName != "Guest User") userName
                            else (currentUserDb?.name?.takeIf { it.isNotBlank() && it != "You" } ?: "Member")
            val emailToUse = if (userEmail.isNotBlank()) userEmail else (currentUserDb?.email ?: "")

            val existingMemberNamesSnap = try { groupRef.child("memberNames").get().await() } catch (_: Exception) { null }
            val existingNames = mutableListOf<String>()
            if (existingMemberNamesSnap != null) {
                for (nSnap in existingMemberNamesSnap.children) {
                    nSnap.getValue(String::class.java)?.let { existingNames.add(it) }
                }
            }
            if (!existingNames.contains(nameToUse)) {
                existingNames.add(nameToUse)
                try {
                    groupRef.child("memberNames").setValue(existingNames).await()
                } catch (_: Exception) {}
            }

            // Update member map object under /groups/{groupId}/members/{activeUserId}
            val memberMap = mapOf("id" to activeUserId, "name" to nameToUse, "email" to emailToUse)
            try {
                groupRef.child("members").child(activeUserId).setValue(memberMap).await()
            } catch (e: Exception) {
                Log.e("FirebaseSync", "Failed to set member map: ${e.message}")
            }

            // Also sync user profile under /users/{activeUserId}
            try {
                database.getReference("users").child(activeUserId).setValue(memberMap).await()
            } catch (_: Exception) {}

            // Fetch and sync all remote group data to local database
            fetchAndSyncRemoteData(activeUserId, nameToUse, groupDao, userDao, expenseDao, settlementDao)

            val groupEntity = groupDao.getGroupByIdSync(targetGroupId)
            if (groupEntity != null) {
                val members = groupDao.getGroupMembersSync(targetGroupId).map { it.toDomain() }
                val expenses = expenseDao.getExpensesForGroupSync(targetGroupId)
                val totalSpent = expenses.sumOf { it.amount }
                Resource.Success(groupEntity.toDomain(members, totalSpent))
            } else {
                Resource.Error("Group joined on server, but failed to sync locally. Please refresh.")
            }
        } catch (e: Exception) {
            val userMsg = when {
                e.message?.contains("Permission denied", ignoreCase = true) == true ->
                    "Permission denied by server. Please check your Firebase Realtime Database Security Rules."
                else -> e.message ?: "Failed to join group with invite code"
            }
            Resource.Error(userMsg)
        }
    }

    suspend fun syncUser(user: User) {
        if (networkMonitor?.isCurrentlyOnline() == false) return
        try {
            db?.getReference("users")?.child(user.id)?.setValue(
                mapOf(
                    "id" to user.id,
                    "name" to user.name,
                    "email" to user.email,
                    "avatarUrl" to user.avatarUrl,
                    "phoneNumber" to user.phoneNumber
                )
            )?.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncGroup(group: Group): Boolean {
        if (networkMonitor?.isCurrentlyOnline() == false) {
            Log.d("FirebaseSync", "Offline: Skipping remote group sync")
            return true
        }
        return try {
            val database = db ?: run {
                Log.e("FirebaseSync", "FirebaseDatabase is null! Cannot sync group")
                return false
            }
            Log.d("FirebaseSync", "Syncing group to Firebase: ${group.id} (${group.name})")

            // Ensure Firebase Auth session exists (Anonymous auth fallback if null)
            val auth = FirebaseHelper.auth
            if (auth != null && auth.currentUser == null) {
                try {
                    auth.signInAnonymously().await()
                } catch (e: Exception) {
                    Log.e("FirebaseSync", "Anonymous auth failed during syncGroup: ${e.message}")
                }
            }

            val activeUid = FirebaseHelper.currentUserId ?: group.createdBy
            val memberIdsList = group.members.map { it.id }.toMutableList()
            if (activeUid.isNotBlank() && !memberIdsList.contains(activeUid)) {
                memberIdsList.add(0, activeUid)
            }
            if (group.createdBy.isNotBlank() && !memberIdsList.contains(group.createdBy)) {
                memberIdsList.add(0, group.createdBy)
            }

            val membersMap = mutableMapOf<String, Map<String, Any>>()
            for (m in group.members) {
                if (m.id.isNotBlank()) {
                    membersMap[m.id] = mapOf(
                        "id" to m.id,
                        "name" to m.name,
                        "email" to (m.email ?: "")
                    )
                }
            }

            if (activeUid.isNotBlank() && !membersMap.containsKey(activeUid)) {
                val activeUser = FirebaseHelper.auth?.currentUser
                val activeName = activeUser?.displayName?.takeIf { it.isNotBlank() }
                    ?: activeUser?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
                    ?: "Member"
                membersMap[activeUid] = mapOf(
                    "id" to activeUid,
                    "name" to activeName,
                    "email" to (activeUser?.email ?: "")
                )
            }

            val groupMap = mapOf(
                "id" to group.id,
                "name" to group.name,
                "description" to group.description,
                "type" to group.type.name,
                "currencySymbol" to group.currencySymbol,
                "currencyCode" to group.currencyCode,
                "createdBy" to group.createdBy,
                "createdAt" to group.createdAt,
                "inviteCode" to group.inviteCode,
                "memberIds" to memberIdsList.distinct(),
                "memberNames" to group.members.map { it.name }.distinct(),
                "members" to membersMap
            )

            // Update group fields on /groups/{groupId} without deleting existing /expenses or /settlements
            database.getReference("groups").child(group.id).updateChildren(groupMap).await()
            Log.d("FirebaseSync", "Group successfully created on Firebase! Path: /groups/${group.id}")

            // Index /inviteCodes/{cleanCode} -> groupId for instant O(1) invitations
            val cleanCode = group.inviteCode.trim().uppercase()
            if (cleanCode.isNotBlank()) {
                database.getReference("inviteCodes").child(cleanCode).setValue(group.id).await()
                Log.d("FirebaseSync", "Invite code index created on Firebase! Path: /inviteCodes/$cleanCode -> ${group.id}")
            }
            true
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Failed to sync group to Firebase: ${e.message}", e)
            false
        }
    }

    fun syncExpense(expense: Expense) {
        if (networkMonitor?.isCurrentlyOnline() == false) return
        try {
            val splitsList = expense.splits.map { split ->
                mapOf(
                    "userId" to split.userId,
                    "userName" to split.userName,
                    "amount" to split.amount,
                    "percentage" to split.percentage,
                    "shares" to split.shares
                )
            }
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
                        "notes" to expense.notes,
                        "createdBy" to expense.createdBy,
                        "isEdited" to expense.isEdited,
                        "splits" to splitsList
                    )
                )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun syncSettlement(settlement: Settlement) {
        if (networkMonitor?.isCurrentlyOnline() == false) return
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
        if (networkMonitor?.isCurrentlyOnline() == false) return
        try {
            db?.getReference("groups")?.child(groupId)?.removeValue()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteExpense(groupId: String, expenseId: String) {
        if (networkMonitor?.isCurrentlyOnline() == false) return
        try {
            db?.getReference("groups")?.child(groupId)?.child("expenses")?.child(expenseId)?.removeValue()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteSettlement(groupId: String, settlementId: String) {
        if (networkMonitor?.isCurrentlyOnline() == false) return
        try {
            db?.getReference("groups")?.child(groupId)?.child("settlements")?.child(settlementId)?.removeValue()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
