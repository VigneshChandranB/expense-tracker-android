package com.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Room entity for account-specific notification preferences
 */
@Entity(
    tableName = "account_notification_preferences",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["accountId"])]
)
data class AccountNotificationPreferencesEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountId: Long,
    val spendingLimitEnabled: Boolean = false,
    val spendingLimit: String? = null, // Stored as string to preserve precision
    val lowBalanceEnabled: Boolean = false,
    val lowBalanceThreshold: String? = null, // Stored as string to preserve precision
    val unusualSpendingEnabled: Boolean = false,
    val largeTransactionEnabled: Boolean = false,
    val largeTransactionThreshold: String? = null, // Stored as string to preserve precision
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)