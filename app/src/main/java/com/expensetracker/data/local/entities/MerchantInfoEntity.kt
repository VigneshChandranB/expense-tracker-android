package com.expensetracker.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Room entity for merchant information table
 * Stores merchant categorization data and statistics
 */
@Entity(
    tableName = "merchant_info",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["normalizedName"]),
        Index(value = ["categoryId"]),
        Index(value = ["confidence"])
    ]
)
data class MerchantInfoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val normalizedName: String,
    val categoryId: Long?,
    val confidence: Float = 0f,
    val transactionCount: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)