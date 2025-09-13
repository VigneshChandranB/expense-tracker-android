package com.expensetracker.domain.model

/**
 * Domain model for categorization rules
 * Used to store learned patterns for automatic categorization
 */
data class CategoryRule(
    val id: Long = 0,
    val merchantPattern: String,
    val categoryId: Long,
    val confidence: Float,
    val isUserDefined: Boolean = false,
    val usageCount: Int = 0,
    val lastUsed: Long = 0
)

/**
 * Result of categorization process
 */
data class CategorizationResult(
    val category: Category,
    val confidence: Float,
    val reason: CategorizationReason
)

/**
 * Reason for categorization decision
 */
sealed class CategorizationReason {
    object UserRule : CategorizationReason()
    object KeywordMatch : CategorizationReason()
    object MerchantHistory : CategorizationReason()
    object DefaultCategory : CategorizationReason()
    data class MachineLearning(val features: List<String>) : CategorizationReason()
}

/**
 * Merchant information for categorization
 */
data class MerchantInfo(
    val name: String,
    val normalizedName: String,
    val categoryId: Long?,
    val confidence: Float = 0f,
    val transactionCount: Int = 0
)