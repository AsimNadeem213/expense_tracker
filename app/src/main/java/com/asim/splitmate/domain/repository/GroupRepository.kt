package com.asim.splitmate.domain.repository

import com.asim.splitmate.core.common.Resource
import com.asim.splitmate.domain.model.Group
import com.asim.splitmate.domain.model.User
import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    fun getAllGroups(): Flow<List<Group>>
    fun getGroupById(groupId: String): Flow<Group?>
    suspend fun createGroup(group: Group): Resource<Group>
    suspend fun updateGroup(group: Group): Resource<Group>
    suspend fun addMemberToGroup(groupId: String, user: User): Resource<Unit>
    suspend fun removeMemberFromGroup(groupId: String, userId: String): Resource<Unit>
    suspend fun deleteGroup(groupId: String): Resource<Unit>
    suspend fun joinGroupWithInviteCode(inviteCode: String): Resource<Group>
    suspend fun syncRemoteData(userId: String)
}
