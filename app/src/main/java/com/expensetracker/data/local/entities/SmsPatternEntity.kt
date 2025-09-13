package com.expensetracker.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Room entity for SMS Pattern table
 */
@Entity(
    tableName = "sms_patterns",
    indices = [
        Index(value = ["bankName"]),
        Index(value = ["isActive"])
    ]
)
data class SmsPatternEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bankName: String,
    val senderPattern: String,
    val amountPattern: String,
    val merchantPattern: String,
    val datePattern: String,
    val typePattern: String,
    val accountPattern: String?,
    val isActive: Boolean
)