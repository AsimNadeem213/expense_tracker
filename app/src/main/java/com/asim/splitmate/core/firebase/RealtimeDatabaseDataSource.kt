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
    private val networkMonitor: com.asim.splitmate.core.network.NetworkMonitor? = null,
    private val syncDao: com.asim.splitmate.data.local.dao.SyncDao? = null
) {
    private val db get() = FirebaseHelper.database

    private var currentSyncUserId: String? = null

    fun startRealtimeSync(
        userId: String,
        userName: String = "",
        groupDao: GroupDao,
        userDao: UserDao,
        expenseDao: ExpenseDao,
        settlementDao: SettlementDao,
        coroutineScope: kotlinx.coroutines.CoroutineScope
    ) {
        if (currentSyncUserId == userId) return
        currentSyncUserId = userId
        val database = db ?: return

        // 1. Listen for network changes to automatically sync when internet becomes available
        networkMonitor?.let { monitor ->
            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                monitor.isOnline.collect { online ->
                    if (online) {
                        Log.d("FirebaseSync", "Network connection restored! Syncing pending offline data...")
                        syncPendingLocalData(groupDao, expenseDao, settlementDao)
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
            }
        }

        // 2. Realtime listener for remote changes on /groups node
        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                Log.d("FirebaseSync", "onDataChange triggered from Firebase! Children count = ${snapshot.childrenCount}")
                coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    fetchAndSyncRemoteData(
                        userId = userId,
                        userName = userName,
                        groupDao = groupDao,
                        userDao = userDao,
                        expenseDao = expenseDao,
                        settlementDao = settlementDao,
                        cachedSnapshot = snapshot
                    )
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Log.e("FirebaseSync", "Realtime sync listener cancelled: ${error.message}")
            }
        }

        database.getReference("groups").addValueEventListener(listener)
        Log.d("FirebaseSync", "Realtime Firebase listener registered successfully for /groups node (User: $userId)!")
    }

    suspend fun fetchAndSyncRemoteData(
        userId: String,
        userName: String = "",
        groupDao: GroupDao,
        userDao: UserDao,
        expenseDao: ExpenseDao,
        settlementDao: SettlementDao,
        cachedSnapshot: com.google.firebase.database.DataSnapshot? = null
    ) {
        if (networkMonitor?.isCurrentlyOnline() == false) {
            Log.d("FirebaseSync", "Offline: Skipping remote fetchAndSyncRemoteData")
            return
        }
        try {
            // Step 1: Ensure any offline created expenses, groups, and settlements are synced first
            syncPendingLocalData(groupDao, expenseDao, settlementDao)

            val database = db ?: return
            val dbRef = database.getReference("groups")

            val snapshot = cachedSnapshot ?: withTimeoutOrNull(5000L) {
                dbRef.get().await()
            } ?: run {
                Log.w("FirebaseSync", "fetchAndSyncRemoteData: snapshot fetch timed out or null")
                return
            }

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

                val currentUserDb = userDao.getCurrentUserSync()
                val currentEmail = currentUserDb?.email?.trim()?.takeIf { it.isNotBlank() }
                    ?: FirebaseHelper.auth?.currentUser?.email?.trim()?.takeIf { it.isNotBlank() } ?: ""
                val currentName = currentUserDb?.name?.trim()?.takeIf { it.isNotBlank() && it != "You" }
                    ?: FirebaseHelper.auth?.currentUser?.displayName?.trim()?.takeIf { it.isNotBlank() }
                    ?: userName.trim().takeIf { it.isNotBlank() && it != "You" } ?: ""

                var isUserMember = createdBy == currentUid ||
                        memberIdsList.contains(currentUid) ||
                        memberKeys.contains(currentUid)

                if (!isUserMember) {
                    if (currentEmail.isNotBlank()) {
                        for (mSnap in membersSnap.children) {
                            val email = mSnap.child("email").getValue(String::class.java) ?: ""
                            if (email.isNotBlank() && email.equals(currentEmail, ignoreCase = true)) {
                                isUserMember = true
                                break
                            }
                        }
                    }
                    if (!isUserMember && currentName.isNotBlank()) {
                        for (mSnap in membersSnap.children) {
                            val name = mSnap.child("name").getValue(String::class.java) ?: ""
                            if (name.isNotBlank() && name.equals(currentName, ignoreCase = true)) {
                                isUserMember = true
                                break
                            }
                        }
                        if (!isUserMember && memberNamesList.any { it.equals(currentName, ignoreCase = true) }) {
                            isUserMember = true
                        }
                    }
                }

                if (!isUserMember) {
                    continue
                }

                // If user was matched via placeholder name/email, auto-link real UID to group in Firebase
                if (currentUid.isNotBlank() && !memberIdsList.contains(currentUid)) {
                    try {
                        val groupRef = database.getReference("groups").child(groupId)
                        val updatedIds = (memberIdsList + currentUid).distinct()
                        groupRef.child("memberIds").setValue(updatedIds)
                        val linkMap = mapOf(
                            "id" to currentUid,
                            "name" to (if (currentName.isNotBlank()) currentName else "Member"),
                            "email" to currentEmail
                        )
                        groupRef.child("members").child(currentUid).setValue(linkMap)
                    } catch (e: Exception) {
                        Log.w("FirebaseSync", "Auto-link member failed: ${e.message}")
                    }
                }

                remoteGroupIdsForUser.add(groupId)

                val groupEntity = GroupEntity(
                    id = groupId,
                    name = name,
                    description = description,
                    type = type.name,
                    currencySymbol = currencySymbol,
                    currencyCode = currencyCode,
                    createdBy = createdBy,
                    createdAt = createdAt,
                    inviteCode = inviteCode,
                    isSynced = true
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
                        val isCurrent = (mId == userId) || (mId == currentUid) ||
                                (currentEmail.isNotBlank() && mEmail.equals(currentEmail, ignoreCase = true)) ||
                                (currentName.isNotBlank() && mName.equals(currentName, ignoreCase = true))

                        val existingUser = userDao.getUserById(mId)
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
                        val isCurrent = (mId == userId) || (mId == currentUid) ||
                                (currentName.isNotBlank() && mName.equals(currentName, ignoreCase = true))

                        val existingUser = userDao.getUserById(mId)
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

                    val isPendingDeletion = syncDao?.getAllPendingDeletions()?.any { it.id == expId } == true
                    if (isPendingDeletion) {
                        Log.d("FirebaseSync", "Skipping re-insertion of locally deleted expense: $expId")
                        deleteExpense(groupId, expId)
                        continue
                    }

                    val existingExp = expenseDao.getExpenseById(expId)
                    val currentUid = FirebaseHelper.currentUserId ?: userId
                    val isSelfCreated = (createdByExp == currentUid) || (createdByExp == "usr_you" && currentUid.isBlank())
                    val isNewRemoteExpense = (existingExp == null) && !isSelfCreated

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
                        isEdited = isEdited,
                        isSynced = true
                    )
                    expenseDao.insertExpense(expenseEntity)

                    Log.d("FirebaseSync", "Synced remote expense '$title' ($expId)")

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

                    val isPendingSetDeletion = syncDao?.getAllPendingDeletions()?.any { it.id == setId } == true
                    if (isPendingSetDeletion) {
                        Log.d("FirebaseSync", "Skipping re-insertion of locally deleted settlement: $setId")
                        deleteSettlement(groupId, setId)
                        continue
                    }

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
                        notes = notes,
                        isSynced = true
                    )
                    settlementDao.insertSettlement(settlementEntity)
                }
                remoteSettlementIdsByGroup[groupId] = remoteSettlementsForThisGroup
            }

            // -------------------------------------------------------------
            // RECONCILE & PURGE REMOTELY DELETED ITEMS FROM LOCAL ROOM DB
            // -------------------------------------------------------------
            val localGroups = groupDao.getAllGroupsSync()
            for (localGroup in localGroups) {
                if (!remoteGroupIdsForUser.contains(localGroup.id)) {
                    if (localGroup.isSynced) {
                        // Truly deleted on Firebase! Purge locally!
                        groupDao.deleteGroupMembersForGroup(localGroup.id)
                        expenseDao.deleteExpensesForGroup(localGroup.id)
                        settlementDao.deleteSettlementsForGroup(localGroup.id)
                        groupDao.deleteGroup(localGroup.id)
                        Log.d("FirebaseSync", "Purged remotely deleted group from Room DB: ${localGroup.id}")
                    } else {
                        // Created offline, sync to Firebase!
                        val members = groupDao.getGroupMembersSync(localGroup.id).map { it.toDomain() }
                        if (syncGroup(localGroup.toDomain(members))) {
                            groupDao.markGroupSynced(localGroup.id)
                            Log.d("FirebaseSync", "Preserved and synced offline group to Firebase: ${localGroup.id}")
                        }
                    }
                } else {
                    // Group still exists. Reconcile expenses & settlements for this group!
                    val remoteExpenseIds = remoteExpenseIdsByGroup[localGroup.id] ?: emptySet()
                    val localExpenses = expenseDao.getExpensesForGroupSync(localGroup.id)
                    for (localExp in localExpenses) {
                        if (!remoteExpenseIds.contains(localExp.id)) {
                            if (localExp.isSynced) {
                                // Truly deleted remotely!
                                expenseDao.deleteSplitsForExpense(localExp.id)
                                expenseDao.deleteExpense(localExp.id)
                                Log.d("FirebaseSync", "Purged remotely deleted expense from Room DB: ${localExp.id}")
                            } else {
                                // Created offline! Do NOT purge! Sync to Firebase!
                                val splits = expenseDao.getSplitsForExpense(localExp.id).map { it.toDomain() }
                                if (syncExpense(localExp.toDomain(splits))) {
                                    expenseDao.markExpenseSynced(localExp.id)
                                    Log.d("FirebaseSync", "Preserved and synced offline expense to Firebase: ${localExp.id}")
                                }
                            }
                        }
                    }

                    val remoteSettlementIds = remoteSettlementIdsByGroup[localGroup.id] ?: emptySet()
                    val localSettlements = settlementDao.getSettlementsForGroupSync(localGroup.id)
                    for (localSet in localSettlements) {
                        if (!remoteSettlementIds.contains(localSet.id)) {
                            if (localSet.isSynced) {
                                settlementDao.deleteSettlement(localSet.id)
                                Log.d("FirebaseSync", "Purged remotely deleted settlement from Room DB: ${localSet.id}")
                            } else {
                                if (syncSettlement(localSet.toDomain())) {
                                    settlementDao.markSettlementSynced(localSet.id)
                                    Log.d("FirebaseSync", "Preserved and synced offline settlement to Firebase: ${localSet.id}")
                                }
                            }
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

    suspend fun syncPendingLocalData(
        groupDao: GroupDao,
        expenseDao: ExpenseDao,
        settlementDao: SettlementDao
    ) {
        if (networkMonitor?.isCurrentlyOnline() == false) {
            Log.d("FirebaseSync", "Offline: Skipping syncPendingLocalData")
            return
        }

        // 1. Process pending offline deletions
        syncDao?.let { sDao ->
            val pendingDeletions = sDao.getAllPendingDeletions()
            for (deletion in pendingDeletions) {
                try {
                    when (deletion.type) {
                        "EXPENSE" -> deleteExpense(deletion.groupId, deletion.id)
                        "GROUP" -> deleteGroup(deletion.id)
                        "SETTLEMENT" -> deleteSettlement(deletion.groupId, deletion.id)
                    }
                    sDao.removePendingDeletion(deletion.id)
                    Log.d("FirebaseSync", "Processed offline deletion for ${deletion.type}: ${deletion.id}")
                } catch (e: Exception) {
                    Log.e("FirebaseSync", "Error processing deletion ${deletion.id}: ${e.message}")
                }
            }
        }

        // 2. Sync unsynced groups
        val unsyncedGroups = groupDao.getUnsyncedGroups()
        for (groupEntity in unsyncedGroups) {
            try {
                val members = groupDao.getGroupMembersSync(groupEntity.id).map { it.toDomain() }
                if (syncGroup(groupEntity.toDomain(members))) {
                    groupDao.markGroupSynced(groupEntity.id)
                    Log.d("FirebaseSync", "Synced offline group to Firebase: ${groupEntity.id}")
                }
            } catch (e: Exception) {
                Log.e("FirebaseSync", "Failed to sync pending group ${groupEntity.id}: ${e.message}")
            }
        }

        // 3. Sync unsynced expenses
        val unsyncedExpenses = expenseDao.getUnsyncedExpenses()
        for (expenseEntity in unsyncedExpenses) {
            try {
                val splits = expenseDao.getSplitsForExpense(expenseEntity.id).map { it.toDomain() }
                val domainExpense = expenseEntity.toDomain(splits)
                if (syncExpense(domainExpense)) {
                    expenseDao.markExpenseSynced(expenseEntity.id)
                    Log.d("FirebaseSync", "Synced offline expense to Firebase: ${expenseEntity.id} (${domainExpense.title})")
                }
            } catch (e: Exception) {
                Log.e("FirebaseSync", "Failed to sync pending expense ${expenseEntity.id}: ${e.message}")
            }
        }

        // 4. Sync unsynced settlements
        val unsyncedSettlements = settlementDao.getUnsyncedSettlements()
        for (settlementEntity in unsyncedSettlements) {
            try {
                if (syncSettlement(settlementEntity.toDomain())) {
                    settlementDao.markSettlementSynced(settlementEntity.id)
                    Log.d("FirebaseSync", "Synced offline settlement to Firebase: ${settlementEntity.id}")
                }
            } catch (e: Exception) {
                Log.e("FirebaseSync", "Failed to sync pending settlement ${settlementEntity.id}: ${e.message}")
            }
        }
    }

    suspend fun syncGroup(group: Group): Boolean {
        if (networkMonitor?.isCurrentlyOnline() == false) {
            Log.d("FirebaseSync", "Offline: Stored group locally, will sync when online: ${group.id}")
            return false
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

    suspend fun syncExpense(expense: Expense): Boolean {
        if (networkMonitor?.isCurrentlyOnline() == false) {
            Log.d("FirebaseSync", "Offline: Stored expense locally, will sync when online: ${expense.id}")
            return false
        }
        return try {
            val database = db ?: return false
            val splitsList = expense.splits.map { split ->
                mapOf(
                    "userId" to split.userId,
                    "userName" to split.userName,
                    "amount" to split.amount,
                    "percentage" to split.percentage,
                    "shares" to split.shares
                )
            }
            database.getReference("groups").child(expense.groupId)
                .child("expenses").child(expense.id).setValue(
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
                ).await()
            Log.d("FirebaseSync", "Successfully synced expense to Firebase: ${expense.id}")
            true
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Failed to sync expense ${expense.id}: ${e.message}", e)
            false
        }
    }

    suspend fun syncSettlement(settlement: Settlement): Boolean {
        if (networkMonitor?.isCurrentlyOnline() == false) {
            Log.d("FirebaseSync", "Offline: Stored settlement locally, will sync when online: ${settlement.id}")
            return false
        }
        return try {
            val database = db ?: return false
            database.getReference("groups").child(settlement.groupId)
                .child("settlements").child(settlement.id).setValue(
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
                ).await()
            Log.d("FirebaseSync", "Successfully synced settlement to Firebase: ${settlement.id}")
            true
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Failed to sync settlement ${settlement.id}: ${e.message}", e)
            false
        }
    }

    suspend fun deleteGroup(groupId: String) {
        if (networkMonitor?.isCurrentlyOnline() == false) {
            syncDao?.recordPendingDeletion(com.asim.splitmate.data.local.entity.PendingDeletionEntity(id = groupId, groupId = groupId, type = "GROUP"))
            Log.d("FirebaseSync", "Offline: Recorded pending deletion for group $groupId")
            return
        }
        try {
            db?.getReference("groups")?.child(groupId)?.removeValue()?.await()
            syncDao?.removePendingDeletion(groupId)
        } catch (e: Exception) {
            syncDao?.recordPendingDeletion(com.asim.splitmate.data.local.entity.PendingDeletionEntity(id = groupId, groupId = groupId, type = "GROUP"))
            e.printStackTrace()
        }
    }

    suspend fun deleteExpense(groupId: String, expenseId: String) {
        if (networkMonitor?.isCurrentlyOnline() == false) {
            syncDao?.recordPendingDeletion(com.asim.splitmate.data.local.entity.PendingDeletionEntity(id = expenseId, groupId = groupId, type = "EXPENSE"))
            Log.d("FirebaseSync", "Offline: Recorded pending deletion for expense $expenseId")
            return
        }
        try {
            db?.getReference("groups")?.child(groupId)?.child("expenses")?.child(expenseId)?.removeValue()?.await()
            syncDao?.removePendingDeletion(expenseId)
        } catch (e: Exception) {
            syncDao?.recordPendingDeletion(com.asim.splitmate.data.local.entity.PendingDeletionEntity(id = expenseId, groupId = groupId, type = "EXPENSE"))
            e.printStackTrace()
        }
    }

    suspend fun deleteSettlement(groupId: String, settlementId: String) {
        if (networkMonitor?.isCurrentlyOnline() == false) {
            syncDao?.recordPendingDeletion(com.asim.splitmate.data.local.entity.PendingDeletionEntity(id = settlementId, groupId = groupId, type = "SETTLEMENT"))
            Log.d("FirebaseSync", "Offline: Recorded pending deletion for settlement $settlementId")
            return
        }
        try {
            db?.getReference("groups")?.child(groupId)?.child("settlements")?.child(settlementId)?.removeValue()?.await()
            syncDao?.removePendingDeletion(settlementId)
        } catch (e: Exception) {
            syncDao?.recordPendingDeletion(com.asim.splitmate.data.local.entity.PendingDeletionEntity(id = settlementId, groupId = groupId, type = "SETTLEMENT"))
            e.printStackTrace()
        }
    }
}
