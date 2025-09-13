package com.expensetracker.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Room entity for keyword mappings table
 * Stores keyword to category mappings for categorization
 */
@Entity(
    tableName = "keyword_mappings",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["keyword"], unique = true),
        Index(value = ["categoryId"])
    ]
)
data class KeywordMappingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val keyword: String,
    val categoryId: Long,
    val isDefault: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)