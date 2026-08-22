package com.asim.splitmate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.asim.splitmate.domain.model.Group
import com.asim.splitmate.domain.model.GroupType
import com.asim.splitmate.domain.model.User

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val type: String,
    val currencySymbol: String,
    val currencyCode: String,
    val createdBy: String,
    val createdAt: Long,
    val inviteCode: String
) {
    fun toDomain(members: List<User> = emptyList(), totalExpense: Double = 0.0): Group = Group(
        id = id,
        name = name,
        description = description,
        type = try { GroupType.valueOf(type) } catch (e: Exception) { GroupType.OTHER },
        currencySymbol = currencySymbol,
        currencyCode = currencyCode,
        createdBy = createdBy,
        createdAt = createdAt,
        members = members,
        totalExpense = totalExpense,
        inviteCode = inviteCode
    )

    companion object {
        fun fromDomain(group: Group): GroupEntity = GroupEntity(
            id = group.id,
            name = group.name,
            description = group.description,
            type = group.type.name,
            currencySymbol = group.currencySymbol,
            currencyCode = group.currencyCode,
            createdBy = group.createdBy,
            createdAt = group.createdAt,
            inviteCode = group.inviteCode
        )
    }
}

@Entity(tableName = "group_members", primaryKeys = ["groupId", "userId"])
data class GroupMemberCrossRef(
    val groupId: String,
    val userId: String
)
