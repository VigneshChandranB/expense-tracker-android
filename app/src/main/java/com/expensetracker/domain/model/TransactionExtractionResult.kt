package com.expensetracker.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Result of SMS transaction extraction process
 */
data class TransactionExtractionResult(
    val transaction: ExtractedTransaction?,
    val confidenceScore: Float,
    val extractionDetails: ExtractionDetails,
    val isSuccessful: Boolean = transaction != null
) {
    companion object {
        fun failure(reason: String, confidenceScore: Float = 0f): TransactionExtractionResult {
            return TransactionExtractionResult(
                transaction = null,
                confidenceScore = confidenceScore,
                extractionDetails = ExtractionDetails(
                    extractedFields = emptyMap(),
                    failureReason = reason,
                    matchedPattern = null
                ),
                isSuccessful = false
            )
        }
        
        fun success(
            transaction: ExtractedTransaction,
            confidenceScore: Float,
            extractionDetails: ExtractionDetails
        ): TransactionExtractionResult {
            return TransactionExtractionResult(
                transaction = transaction,
                confidenceScore = confidenceScore,
                extractionDetails = extractionDetails,
                isSuccessful = true
            )
        }
    }
}

/**
 * Transaction data extracted from SMS
 */
data class ExtractedTransaction(
    val amount: BigDecimal,
    val type: TransactionType,
    val merchant: String,
    val date: LocalDateTime,
    val accountIdentifier: String? = null,
    val description: String? = null,
    val source: TransactionSource = TransactionSource.SMS_AUTO
)

/**
 * Details about the extraction process
 */
data class ExtractionDetails(
    val extractedFields: Map<String, String>,
    val failureReason: String? = null,
    val matchedPattern: SmsPattern? = null,
    val processingTimeMs: Long = 0
)

/**
 * Confidence scoring factors
 */
data class ConfidenceFactors(
    val amountExtracted: Boolean = false,
    val typeExtracted: Boolean = false,
    val merchantExtracted: Boolean = false,
    val dateExtracted: Boolean = false,
    val accountExtracted: Boolean = false,
    val patternMatched: Boolean = false,
    val senderTrusted: Boolean = false
) {
    fun calculateScore(): Float {
        val factors = listOf(
            amountExtracted to 0.3f,      // Amount is most critical
            typeExtracted to 0.2f,        // Transaction type is important
            merchantExtracted to 0.15f,   // Merchant identification
            dateExtracted to 0.1f,        // Date extraction
            accountExtracted to 0.1f,     // Account identification
            patternMatched to 0.1f,       // Pattern matching confidence
            senderTrusted to 0.05f        // Sender verification
        )
        
        return factors.sumOf { (condition, weight) ->
            if (condition) weight else 0f
        }
    }
}