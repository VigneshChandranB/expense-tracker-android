package com.expensetracker.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Room entity for category rules table
 * Stores learned categorization patterns
 */
@Entity(
    tableName = "category_rules",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["merchantPattern"]),
        Index(value = ["categoryId"]),
        Index(value = ["confidence"])
    ]
)
data class CategoryRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val merchantPattern: String,
    val categoryId: Long,
    val confidence: Float,
    val isUserDefined: Boolean = false,
    val usageCount: Int = 0,
    val lastUsed: Long = 0
)