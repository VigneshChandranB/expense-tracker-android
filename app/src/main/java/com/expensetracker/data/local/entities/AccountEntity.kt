package com.expensetracker.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Room entity for Account table
 */
@Entity(
    tableName = "accounts",
    indices = [
        Index(value = ["accountNumber"], unique = true),
        Index(value = ["isActive"])
    ]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bankName: String,
    val accountType: String,
    val accountNumber: String,
    val nickname: String,
    val currentBalance: String, // Stored as string to preserve precision
    val isActive: Boolean,
    val createdAt: Long
)