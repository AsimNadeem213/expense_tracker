package com.asim.splitmate.data.repository

import com.asim.splitmate.core.common.Resource
import com.asim.splitmate.core.firebase.FirebaseHelper
import com.asim.splitmate.core.firebase.RealtimeDatabaseDataSource
import com.asim.splitmate.data.local.dao.ExpenseDao
import com.asim.splitmate.data.local.dao.GroupDao
import com.asim.splitmate.data.local.dao.SettlementDao
import com.asim.splitmate.data.local.dao.UserDao
import com.asim.splitmate.data.local.entity.GroupEntity
import com.asim.splitmate.data.local.entity.GroupMemberCrossRef
import com.asim.splitmate.data.local.entity.UserEntity
import com.asim.splitmate.domain.model.Group
import com.asim.splitmate.domain.model.User
import com.asim.splitmate.domain.repository.GroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GroupRepositoryImpl(
    private val groupDao: GroupDao,
    private val userDao: UserDao,
    private val expenseDao: ExpenseDao,
    private val settlementDao: SettlementDao,
    private val realtimeDatabaseDataSource: RealtimeDatabaseDataSource
) : GroupRepository {

    override fun getAllGroups(): Flow<List<Group>> {
        val currentUserId = FirebaseHelper.currentUserId
        return groupDao.getAllGroups().map { entities ->
            entities.mapNotNull { entity ->
                val members = groupDao.getGroupMembersSync(entity.id).map { it.toDomain() }.distinctBy { it.id }
                val isUserMember = (currentUserId != null && entity.createdBy == currentUserId) ||
                        members.any { (currentUserId != null && it.id == currentUserId) || it.isCurrentUser }
                if (isUserMember || currentUserId == null) {
                    val expenses = expenseDao.getExpensesForGroupSync(entity.id)
                    val totalSpent = expenses.sumOf { it.amount }
                    entity.toDomain(members = members, totalExpense = totalSpent)
                } else {
                    null
                }
            }
        }
    }

    override fun getGroupById(groupId: String): Flow<Group?> {
        return groupDao.getGroupById(groupId).map { entity ->
            if (entity == null) null
            else {
                val members = groupDao.getGroupMembersSync(entity.id).map { it.toDomain() }.distinctBy { it.id }
                val expenses = expenseDao.getExpensesForGroupSync(entity.id)
                val totalSpent = expenses.sumOf { it.amount }
                entity.toDomain(members = members, totalExpense = totalSpent)
            }
        }
    }

    override suspend fun syncRemoteData(userId: String) {
        val currentUser = userDao.getCurrentUserSync()
        val userName = currentUser?.name ?: "You"
        realtimeDatabaseDataSource.fetchAndSyncRemoteData(
            userId = userId,
            userName = userName,
            groupDao = groupDao,
            userDao = userDao,
            expenseDao = expenseDao,
            settlementDao = settlementDao
        )
        realtimeDatabaseDataSource.startRealtimeSync(
            userId = userId,
            userName = userName,
            groupDao = groupDao,
            userDao = userDao,
            expenseDao = expenseDao,
            settlementDao = settlementDao,
            coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
        )
    }

    override suspend fun createGroup(group: Group): Resource<Group> {
        return try {
            val uniqueMembers = group.members.distinctBy { it.id }
            val cleanGroup = group.copy(members = uniqueMembers)
            val groupEntity = GroupEntity.fromDomain(cleanGroup)
            groupDao.insertGroup(groupEntity)

            val memberEntities = uniqueMembers.map { UserEntity.fromDomain(it) }
            userDao.insertUsers(memberEntities)

            val crossRefs = uniqueMembers.map { GroupMemberCrossRef(groupId = group.id, userId = it.id) }
            groupDao.insertGroupMembers(crossRefs)

            realtimeDatabaseDataSource.syncGroup(cleanGroup)
            Resource.Success(cleanGroup)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create group", e)
        }
    }

    override suspend fun updateGroup(group: Group): Resource<Group> {
        return try {
            val existingMemberIds = groupDao.getGroupMembersSync(group.id).map { it.id }.toSet()
            val expenses = expenseDao.getExpensesForGroupSync(group.id)
            val newMemberIds = group.members.map { it.id }.filter { !existingMemberIds.contains(it) }

            if (expenses.isNotEmpty() && newMemberIds.isNotEmpty()) {
                return Resource.Error("New members cannot be added to this group because expenses have already been recorded.")
            }

            val uniqueMembers = group.members.distinctBy { it.id }
            val cleanGroup = group.copy(members = uniqueMembers)
            val groupEntity = GroupEntity.fromDomain(cleanGroup)
            groupDao.insertGroup(groupEntity)

            val memberEntities = uniqueMembers.map { UserEntity.fromDomain(it) }
            userDao.insertUsers(memberEntities)

            groupDao.deleteGroupMembersForGroup(group.id)
            val crossRefs = uniqueMembers.map { GroupMemberCrossRef(groupId = group.id, userId = it.id) }
            groupDao.insertGroupMembers(crossRefs)

            realtimeDatabaseDataSource.syncGroup(cleanGroup)
            Resource.Success(cleanGroup)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update group", e)
        }
    }

    override suspend fun addMemberToGroup(groupId: String, user: User): Resource<Unit> {
        return try {
            val existingMembers = groupDao.getGroupMembersSync(groupId)
            if (existingMembers.any { it.id == user.id }) {
                return Resource.Success(Unit)
            }
            val expenses = expenseDao.getExpensesForGroupSync(groupId)
            if (expenses.isNotEmpty()) {
                return Resource.Error("New members cannot be added to this group because expenses have already been recorded.")
            }
            userDao.insertUser(UserEntity.fromDomain(user))
            groupDao.insertGroupMembers(listOf(GroupMemberCrossRef(groupId = groupId, userId = user.id)))
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to add member", e)
        }
    }

    override suspend fun removeMemberFromGroup(groupId: String, userId: String): Resource<Unit> {
        return try {
            groupDao.removeGroupMember(groupId, userId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to remove member", e)
        }
    }

    override suspend fun deleteGroup(groupId: String): Resource<Unit> {
        return try {
            expenseDao.deleteExpensesForGroup(groupId)
            groupDao.deleteGroupMembersForGroup(groupId)
            groupDao.deleteGroup(groupId)
            realtimeDatabaseDataSource.deleteGroup(groupId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete group", e)
        }
    }

    override suspend fun joinGroupWithInviteCode(inviteCode: String): Resource<Group> {
        val firebaseUser = FirebaseHelper.auth?.currentUser
        val dbUser = userDao.getCurrentUserSync()

        val realId = dbUser?.id ?: firebaseUser?.uid ?: ("usr_" + java.util.UUID.randomUUID().toString().replace("-", "").take(16))
        val realName = dbUser?.name?.takeIf { it.isNotBlank() && it != "You" && it != "Guest User" }
            ?: firebaseUser?.displayName?.takeIf { it.isNotBlank() }
            ?: firebaseUser?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
            ?: dbUser?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
            ?: "User"
        val realEmail = firebaseUser?.email?.takeIf { it.isNotBlank() }
            ?: dbUser?.email
            ?: ""

        val currentEntity = UserEntity(
            id = realId,
            name = realName,
            email = realEmail,
            isCurrentUser = true
        )
        userDao.insertUser(currentEntity)

        return realtimeDatabaseDataSource.joinGroupWithInviteCode(
            inviteCode = inviteCode,
            userId = realId,
            userName = realName,
            userEmail = realEmail,
            groupDao = groupDao,
            userDao = userDao,
            expenseDao = expenseDao,
            settlementDao = settlementDao
        )
    }
}
