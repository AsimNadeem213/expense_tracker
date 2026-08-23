package com.asim.splitmate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.asim.splitmate.data.local.entity.GroupEntity
import com.asim.splitmate.data.local.entity.GroupMemberCrossRef
import com.asim.splitmate.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups ORDER BY createdAt DESC")
    fun getAllGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups ORDER BY createdAt DESC")
    suspend fun getAllGroupsSync(): List<GroupEntity>

    @Query("SELECT * FROM groups WHERE id = :groupId LIMIT 1")
    fun getGroupById(groupId: String): Flow<GroupEntity?>

    @Query("SELECT * FROM groups WHERE id = :groupId LIMIT 1")
    suspend fun getGroupByIdSync(groupId: String): GroupEntity?

    @Query("""
        SELECT u.* FROM users u 
        INNER JOIN group_members gm ON u.id = gm.userId 
        WHERE gm.groupId = :groupId
    """)
    fun getGroupMembers(groupId: String): Flow<List<UserEntity>>

    @Query("""
        SELECT u.* FROM users u 
        INNER JOIN group_members gm ON u.id = gm.userId 
        WHERE gm.groupId = :groupId
    """)
    suspend fun getGroupMembersSync(groupId: String): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupMembers(crossRefs: List<GroupMemberCrossRef>)

    @Query("DELETE FROM group_members WHERE groupId = :groupId AND userId = :userId")
    suspend fun removeGroupMember(groupId: String, userId: String)

    @Query("DELETE FROM group_members WHERE groupId = :groupId")
    suspend fun deleteGroupMembersForGroup(groupId: String)

    @Query("DELETE FROM groups WHERE id = :groupId")
    suspend fun deleteGroup(groupId: String)
}
