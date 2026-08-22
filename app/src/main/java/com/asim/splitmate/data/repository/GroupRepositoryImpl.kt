package com.asim.splitmate.data.repository

import com.asim.splitmate.core.common.Resource
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
        return groupDao.getAllGroups().map { entities ->
            entities.map { entity ->
                val members = groupDao.getGroupMembersSync(entity.id).map { it.toDomain() }
                val expenses = expenseDao.getExpensesForGroupSync(entity.id)
                val totalSpent = expenses.sumOf { it.amount }
                entity.toDomain(members = members, totalExpense = totalSpent)
            }
        }
    }

    override fun getGroupById(groupId: String): Flow<Group?> {
        return groupDao.getGroupById(groupId).map { entity ->
            if (entity == null) null
            else {
                val members = groupDao.getGroupMembersSync(entity.id).map { it.toDomain() }
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
    }

    override suspend fun createGroup(group: Group): Resource<Group> {
        return try {
            val groupEntity = GroupEntity.fromDomain(group)
            groupDao.insertGroup(groupEntity)

            val memberEntities = group.members.map { UserEntity.fromDomain(it) }
            userDao.insertUsers(memberEntities)

            val crossRefs = group.members.map { GroupMemberCrossRef(groupId = group.id, userId = it.id) }
            groupDao.insertGroupMembers(crossRefs)

            realtimeDatabaseDataSource.syncGroup(group)
            Resource.Success(group)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create group", e)
        }
    }

    override suspend fun updateGroup(group: Group): Resource<Group> {
        return try {
            val groupEntity = GroupEntity.fromDomain(group)
            groupDao.insertGroup(groupEntity)

            val memberEntities = group.members.map { UserEntity.fromDomain(it) }
            userDao.insertUsers(memberEntities)

            groupDao.deleteGroupMembersForGroup(group.id)
            val crossRefs = group.members.map { GroupMemberCrossRef(groupId = group.id, userId = it.id) }
            groupDao.insertGroupMembers(crossRefs)

            realtimeDatabaseDataSource.syncGroup(group)
            Resource.Success(group)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update group", e)
        }
    }

    override suspend fun addMemberToGroup(groupId: String, user: User): Resource<Unit> {
        return try {
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
        val currentUser = userDao.getCurrentUserSync()
        val userId = currentUser?.id ?: "usr_you"
        val userName = currentUser?.name ?: "You"
        return realtimeDatabaseDataSource.joinGroupWithInviteCode(
            inviteCode = inviteCode,
            userId = userId,
            userName = userName,
            groupDao = groupDao,
            userDao = userDao,
            expenseDao = expenseDao,
            settlementDao = settlementDao
        )
    }
}
