package com.expensetracker.data.sms

import com.expensetracker.domain.error.ErrorResult
import com.expensetracker.domain.error.ErrorType
import com.expensetracker.domain.error.safeCall
import com.expensetracker.domain.model.SmsMessage
import com.expensetracker.domain.model.Transaction
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles SMS processing errors with retry logic and recovery mechanisms
 */
@Singleton
class SmsErrorHandler @Inject constructor() {
    
    private val maxRetries = 3
    private val retryDelayMs = 1000L
    
    /**
     * Processes SMS with error handling and retry logic
     */
    suspend fun processWithRetry(
        smsMessage: SmsMessage,
        processor: suspend (SmsMessage) -> Transaction?
    ): ErrorResult<Transaction?> {
        return retryOperation(maxRetries) {
            safeCall(
                errorType = ErrorType.SmsError.ProcessingTimeout(5000L),
                message = "SMS processing failed"
            ) {
                processor(smsMessage)
            }
        }
    }
    
    /**
     * Handles amount parsing with fallback strategies
     */
    fun parseAmountWithFallback(
        text: String,
        patterns: List<Regex>
    ): ErrorResult<Double> {
        // Try each pattern in order
        for (pattern in patterns) {
            val result = tryParseAmount(text, pattern)
            if (result is ErrorResult.Success) {
                return result
            }
        }
        
        // Try manual extraction as fallback
        return tryManualAmountExtraction(text)
    }
    
    private fun tryParseAmount(text: String, pattern: Regex): ErrorResult<Double> {
        return safeCall(
            errorType = ErrorType.SmsError.AmountParsingFailed,
            message = "Failed to parse amount with pattern: ${pattern.pattern}"
        ) {
            val match = pattern.find(text)
                ?: throw IllegalArgumentException("Pattern not found")
            
            val amountStr = match.groupValues.getOrNull(1)
                ?: throw IllegalArgumentException("Amount group not found")
            
            // Clean and parse amount
            val cleanAmount = amountStr
                .replace(",", "")
                .replace("₹", "")
                .replace("Rs", "")
                .replace("INR", "")
                .trim()
            
            cleanAmount.toDouble()
        }
    }
    
    private fun tryManualAmountExtraction(text: String): ErrorResult<Double> {
        return safeCall(
            errorType = ErrorType.SmsError.AmountParsingFailed,
            message = "Manual amount extraction failed"
        ) {
            // Look for number patterns that could be amounts
            val numberPattern = Regex("""(\d{1,3}(?:,\d{3})*(?:\.\d{2})?)""")
            val matches = numberPattern.findAll(text).toList()
            
            // Find the most likely amount (usually the largest number)
            val amounts = matches.mapNotNull { match ->
                try {
                    match.value.replace(",", "").toDouble()
                } catch (e: NumberFormatException) {
                    null
                }
            }.filter { it > 0 }
            
            amounts.maxOrNull() ?: throw IllegalArgumentException("No valid amount found")
        }
    }
    
    /**
     * Handles merchant name extraction with fallback
     */
    fun extractMerchantWithFallback(
        text: String,
        patterns: List<Regex>
    ): ErrorResult<String> {
        // Try each pattern
        for (pattern in patterns) {
            val result = tryExtractMerchant(text, pattern)
            if (result is ErrorResult.Success) {
                return result
            }
        }
        
        // Fallback to generic extraction
        return extractGenericMerchant(text)
    }
    
    private fun tryExtractMerchant(text: String, pattern: Regex): ErrorResult<String> {
        return safeCall(
            errorType = ErrorType.SmsError.PatternMatchFailed(pattern.pattern),
            message = "Merchant extraction failed"
        ) {
            val match = pattern.find(text)
                ?: throw IllegalArgumentException("Merchant pattern not found")
            
            match.groupValues.getOrNull(1)?.trim()
                ?: throw IllegalArgumentException("Merchant group not found")
        }
    }
    
    private fun extractGenericMerchant(text: String): ErrorResult<String> {
        return safeCall(
            errorType = ErrorType.SmsError.UnknownBankFormat,
            message = "Generic merchant extraction failed"
        ) {
            // Look for common merchant indicators
            val merchantPatterns = listOf(
                Regex("""at\s+([A-Z][A-Z0-9\s]{2,20})""", RegexOption.IGNORE_CASE),
                Regex("""to\s+([A-Z][A-Z0-9\s]{2,20})""", RegexOption.IGNORE_CASE),
                Regex("""from\s+([A-Z][A-Z0-9\s]{2,20})""", RegexOption.IGNORE_CASE)
            )
            
            for (pattern in merchantPatterns) {
                val match = pattern.find(text)
                if (match != null) {
                    return@safeCall match.groupValues[1].trim()
                }
            }
            
            "Unknown Merchant"
        }
    }
    
    /**
     * Validates SMS message before processing
     */
    fun validateSmsMessage(smsMessage: SmsMessage): ErrorResult<SmsMessage> {
        return when {
            smsMessage.body.isBlank() -> ErrorResult.Error(
                ErrorType.SmsError.InvalidFormat,
                "SMS body is empty"
            )
            smsMessage.body.length < 10 -> ErrorResult.Error(
                ErrorType.SmsError.InvalidFormat,
                "SMS body too short"
            )
            !containsFinancialKeywords(smsMessage.body) -> ErrorResult.Error(
                ErrorType.SmsError.InvalidFormat,
                "SMS does not contain financial keywords"
            )
            else -> ErrorResult.Success(smsMessage)
        }
    }
    
    private fun containsFinancialKeywords(text: String): Boolean {
        val keywords = listOf(
            "debited", "credited", "paid", "received", "transfer", "transaction",
            "amount", "balance", "account", "card", "upi", "payment"
        )
        
        return keywords.any { keyword ->
            text.contains(keyword, ignoreCase = true)
        }
    }
    
    /**
     * Generic retry mechanism with exponential backoff
     */
    private suspend fun <T> retryOperation(
        maxAttempts: Int,
        operation: suspend () -> ErrorResult<T>
    ): ErrorResult<T> {
        var lastError: ErrorResult.Error? = null
        
        repeat(maxAttempts) { attempt ->
            when (val result = operation()) {
                is ErrorResult.Success -> return result
                is ErrorResult.Error -> {
                    lastError = result
                    if (attempt < maxAttempts - 1) {
                        // Exponential backoff
                        delay(retryDelayMs * (attempt + 1))
                    }
                }
            }
        }
        
        return lastError ?: ErrorResult.Error(
            ErrorType.GeneralError.Unknown(RuntimeException("Retry failed")),
            "Operation failed after $maxAttempts attempts"
        )
    }
    
    /**
     * Logs SMS processing errors for debugging
     */
    fun logSmsError(
        smsMessage: SmsMessage,
        error: ErrorResult.Error,
        context: String = ""
    ) {
        val logMessage = buildString {
            append("SMS Processing Error: ")
            append(error.message)
            if (context.isNotEmpty()) {
                append(" | Context: $context")
            }
            append(" | SMS: ${smsMessage.body.take(100)}")
            append(" | Sender: ${smsMessage.sender}")
        }
        
        // In a real app, this would use a proper logging framework
        println("ERROR: $logMessage")
        
        // Could also send to crash reporting service
        error.cause?.let { throwable ->
            println("Caused by: ${throwable.message}")
            throwable.printStackTrace()
        }
    }
}