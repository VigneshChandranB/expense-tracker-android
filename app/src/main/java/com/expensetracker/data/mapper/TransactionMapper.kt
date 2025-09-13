package com.expensetracker.data.mapper

import com.expensetracker.data.local.entities.CategoryEntity
import com.expensetracker.data.local.entities.TransactionEntity
import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.model.TransactionSource
import com.expensetracker.domain.model.TransactionType
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Mapper functions for converting between domain models and data entities
 */

/**
 * Convert CategoryEntity to domain Category model
 */
fun CategoryEntity.toDomainModel(): Category {
    return Category(
        id = id,
        name = name,
        icon = icon,
        color = color,
        isDefault = isDefault,
        parentCategory = null // Parent category would need to be resolved separately
    )
}

/**
 * Convert domain Category model to CategoryEntity
 */
fun Category.toEntity(): CategoryEntity {
    return CategoryEntity(
        id = id,
        name = name,
        icon = icon,
        color = color,
        isDefault = isDefault,
        parentCategoryId = parentCategory?.id
    )
}

/**
 * Convert TransactionEntity to domain Transaction model
 */
suspend fun TransactionEntity.toDomainModel(
    category: Category
): Transaction {
    return Transaction(
        id = id,
        amount = BigDecimal(amount),
        type = TransactionType.valueOf(type),
        category = category,
        merchant = merchant,
        description = description,
        date = LocalDateTime.ofEpochSecond(date, 0, ZoneOffset.UTC),
        source = TransactionSource.valueOf(source),
        accountId = accountId,
        transferAccountId = transferAccountId,
        transferTransactionId = transferTransactionId,
        isRecurring = isRecurring
    )
}

/**
 * Convert domain Transaction model to TransactionEntity
 */
fun Transaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id,
        amount = amount.toString(),
        type = type.name,
        categoryId = category.id,
        accountId = accountId,
        merchant = merchant,
        description = description,
        date = date.toEpochSecond(ZoneOffset.UTC),
        source = source.name,
        transferAccountId = transferAccountId,
        transferTransactionId = transferTransactionId,
        isRecurring = isRecurring,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}