package com.expensetracker.domain.model

/**
 * Domain model for Category entity
 * Represents transaction categories for organization
 */
data class Category(
    val id: Long,
    val name: String,
    val icon: String,
    val color: String,
    val isDefault: Boolean,
    val parentCategory: Category? = null
)