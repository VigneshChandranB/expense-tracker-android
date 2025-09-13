package com.expensetracker.data.sms

import com.expensetracker.domain.model.SmsMessage
import com.expensetracker.domain.model.TransactionExtractionResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface for processing SMS messages to extract transaction information
 */
interface SmsProcessor {
    suspend fun processSmsMessage(smsMessage: SmsMessage): TransactionExtractionResult?
}

/**
 * Enhanced SMS processor with transaction extraction capabilities
 */
@Singleton
class SmartSmsProcessor @Inject constructor(
    private val transactionExtractor: SmsTransactionExtractor
) : SmsProcessor {
    
    companion object {
        private const val MIN_CONFIDENCE_THRESHOLD = 0.6f
    }
    
    override suspend fun processSmsMessage(smsMessage: SmsMessage): TransactionExtractionResult? {
        // Only process messages that appear to be transaction-related
        if (!smsMessage.isPotentialTransactionSms()) {
            return null
        }
        
        // Only process messages from banks or financial institutions
        if (!smsMessage.isFromBankOrFinancialInstitution()) {
            return null
        }
        
        try {
            // Extract transaction from SMS
            val extractionResult = transactionExtractor.extractTransaction(smsMessage)
            
            // Only return results that meet minimum confidence threshold
            return if (extractionResult.isSuccessful && 
                      extractionResult.confidenceScore >= MIN_CONFIDENCE_THRESHOLD) {
                extractionResult
            } else {
                // Log low confidence extractions for debugging
                extractionResult
            }
            
        } catch (e: Exception) {
            // Return failure result for any processing errors
            return TransactionExtractionResult.failure(
                "SMS processing failed: ${e.message}",
                0.0f
            )
        }
    }
}