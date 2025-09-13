package com.expensetracker.data.sms

import com.expensetracker.domain.model.SmsPattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface for managing bank SMS patterns
 */
interface BankPatternRegistry {
    suspend fun registerPattern(pattern: SmsPattern)
    suspend fun getPatternsByBank(bankName: String): List<SmsPattern>
    suspend fun getAllPatterns(): List<SmsPattern>
    suspend fun updatePattern(pattern: SmsPattern)
    suspend fun deactivatePattern(patternId: Long)
    suspend fun activatePattern(patternId: Long)
    suspend fun deletePattern(patternId: Long)
    suspend fun findPatternBySender(sender: String): SmsPattern?
}

/**
 * In-memory implementation of bank pattern registry
 * In a real app, this would be backed by a database
 */
@Singleton
class InMemoryBankPatternRegistry @Inject constructor() : BankPatternRegistry {
    
    private val patterns = mutableMapOf<Long, SmsPattern>()
    private var nextId = 1L
    
    init {
        // Initialize with default patterns
        initializeDefaultPatterns()
    }
    
    override suspend fun registerPattern(pattern: SmsPattern) {
        val patternWithId = if (pattern.id == 0L) {
            pattern.copy(id = nextId++)
        } else {
            pattern
        }
        patterns[patternWithId.id] = patternWithId
    }
    
    override suspend fun getPatternsByBank(bankName: String): List<SmsPattern> {
        return patterns.values.filter { 
            it.bankName.equals(bankName, ignoreCase = true) && it.isActive 
        }
    }
    
    override suspend fun getAllPatterns(): List<SmsPattern> {
        return patterns.values.toList()
    }
    
    override suspend fun updatePattern(pattern: SmsPattern) {
        if (patterns.containsKey(pattern.id)) {
            patterns[pattern.id] = pattern
        }
    }
    
    override suspend fun deactivatePattern(patternId: Long) {
        patterns[patternId]?.let { pattern ->
            patterns[patternId] = pattern.copy(isActive = false)
        }
    }
    
    override suspend fun activatePattern(patternId: Long) {
        patterns[patternId]?.let { pattern ->
            patterns[patternId] = pattern.copy(isActive = true)
        }
    }
    
    override suspend fun deletePattern(patternId: Long) {
        patterns.remove(patternId)
    }
    
    override suspend fun findPatternBySender(sender: String): SmsPattern? {
        return patterns.values
            .filter { it.isActive }
            .find { pattern ->
                try {
                    val regex = Regex(pattern.senderPattern, RegexOption.IGNORE_CASE)
                    regex.containsMatchIn(sender)
                } catch (e: Exception) {
                    false
                }
            }
    }
    
    private fun initializeDefaultPatterns() {
        val defaultPatterns = listOf(
            // HDFC Bank
            SmsPattern(
                id = 1,
                bankName = "HDFC Bank",
                senderPattern = ".*HDFC.*|.*VK-HDFCBK.*|.*HDFCBK.*",
                amountPattern = "(?:Rs\\.?|INR|₹)\\s*([\\d,]+(?:\\.\\d{2})?)",
                merchantPattern = "(?:at|to|from)\\s+([A-Za-z0-9\\s&.-]+?)(?:\\s+on|\\s+dt|\\.|,|$)",
                datePattern = "(\\d{2}-\\d{2}-\\d{4}\\s+\\d{2}:\\d{2}:\\d{2}|\\d{2}/\\d{2}/\\d{4})",
                typePattern = "(debited|credited|debit|credit)",
                accountPattern = "(?:A/c|account)\\s+(?:no\\.?)?\\s*([X\\d]+)",
                isActive = true
            ),
            
            // ICICI Bank
            SmsPattern(
                id = 2,
                bankName = "ICICI Bank",
                senderPattern = ".*ICICI.*|.*ICICIB.*|.*ICICI.*",
                amountPattern = "(?:Rs\\.?|INR|₹)\\s*([\\d,]+(?:\\.\\d{2})?)",
                merchantPattern = "(?:at|to|from)\\s+([A-Za-z0-9\\s&.-]+?)(?:\\s+on|\\s+dt|\\.|,|$)",
                datePattern = "(\\d{2}-\\d{2}-\\d{4}\\s+\\d{2}:\\d{2}:\\d{2}|\\d{2}/\\d{2}/\\d{4})",
                typePattern = "(debited|credited|debit|credit)",
                accountPattern = "(?:card|account)\\s+(?:ending\\s+)?([X\\d]+)",
                isActive = true
            ),
            
            // State Bank of India
            SmsPattern(
                id = 3,
                bankName = "State Bank of India",
                senderPattern = ".*SBI.*|.*SBIIN.*|.*SBIINB.*",
                amountPattern = "(?:Rs\\.?|INR|₹)\\s*([\\d,]+(?:\\.\\d{2})?)",
                merchantPattern = "(?:at|to|from)\\s+([A-Za-z0-9\\s&.-]+?)(?:\\s+on|\\s+dt|\\.|,|$)",
                datePattern = "(\\d{2}-\\d{2}-\\d{4}\\s+\\d{2}:\\d{2}:\\d{2}|\\d{2}/\\d{2}/\\d{4})",
                typePattern = "(debited|credited|debit|credit)",
                accountPattern = "(?:A/c|account)\\s+(?:no\\.?)?\\s*([X\\d]+)",
                isActive = true
            ),
            
            // Axis Bank
            SmsPattern(
                id = 4,
                bankName = "Axis Bank",
                senderPattern = ".*AXIS.*|.*AXISBK.*|.*AXIBNK.*",
                amountPattern = "(?:Rs\\.?|INR|₹)\\s*([\\d,]+(?:\\.\\d{2})?)",
                merchantPattern = "(?:at|to|from)\\s+([A-Za-z0-9\\s&.-]+?)(?:\\s+on|\\s+dt|\\.|,|$)",
                datePattern = "(\\d{2}-\\d{2}-\\d{4}\\s+\\d{2}:\\d{2}:\\d{2}|\\d{2}/\\d{2}/\\d{4})",
                typePattern = "(debited|credited|debit|credit)",
                accountPattern = "(?:card|account)\\s+(?:ending\\s+)?([X\\d]+)",
                isActive = true
            ),
            
            // Kotak Mahindra Bank
            SmsPattern(
                id = 5,
                bankName = "Kotak Mahindra Bank",
                senderPattern = ".*KOTAK.*|.*KMB.*|.*KMBL.*",
                amountPattern = "(?:Rs\\.?|INR|₹)\\s*([\\d,]+(?:\\.\\d{2})?)",
                merchantPattern = "(?:at|to|from)\\s+([A-Za-z0-9\\s&.-]+?)(?:\\s+on|\\s+dt|\\.|,|$)",
                datePattern = "(\\d{2}-\\d{2}-\\d{4}\\s+\\d{2}:\\d{2}:\\d{2}|\\d{2}/\\d{2}/\\d{4})",
                typePattern = "(debited|credited|debit|credit)",
                accountPattern = "(?:A/c|account)\\s+(?:no\\.?)?\\s*([X\\d]+)",
                isActive = true
            ),
            
            // Paytm Payments Bank
            SmsPattern(
                id = 6,
                bankName = "Paytm Payments Bank",
                senderPattern = ".*PAYTM.*|.*PYTM.*",
                amountPattern = "(?:Rs\\.?|INR|₹)\\s*([\\d,]+(?:\\.\\d{2})?)",
                merchantPattern = "(?:at|to|from)\\s+([A-Za-z0-9\\s&.-]+?)(?:\\s+on|\\s+dt|\\.|,|$)",
                datePattern = "(\\d{2}-\\d{2}-\\d{4}\\s+\\d{2}:\\d{2}:\\d{2}|\\d{2}/\\d{2}/\\d{4})",
                typePattern = "(debited|credited|debit|credit|paid|received)",
                accountPattern = "(?:wallet|account)\\s+(?:ending\\s+)?([X\\d]+)",
                isActive = true
            ),
            
            // PhonePe
            SmsPattern(
                id = 7,
                bankName = "PhonePe",
                senderPattern = ".*PHONEPE.*|.*PHONPE.*",
                amountPattern = "(?:Rs\\.?|INR|₹)\\s*([\\d,]+(?:\\.\\d{2})?)",
                merchantPattern = "(?:at|to|from)\\s+([A-Za-z0-9\\s&.-]+?)(?:\\s+on|\\s+dt|\\.|,|$)",
                datePattern = "(\\d{2}-\\d{2}-\\d{4}\\s+\\d{2}:\\d{2}:\\d{2}|\\d{2}/\\d{2}/\\d{4})",
                typePattern = "(debited|credited|debit|credit|paid|received)",
                accountPattern = "(?:wallet|account)\\s+(?:ending\\s+)?([X\\d]+)",
                isActive = true
            ),
            
            // Google Pay
            SmsPattern(
                id = 8,
                bankName = "Google Pay",
                senderPattern = ".*GPAY.*|.*GOOGLEPAY.*",
                amountPattern = "(?:Rs\\.?|INR|₹)\\s*([\\d,]+(?:\\.\\d{2})?)",
                merchantPattern = "(?:at|to|from)\\s+([A-Za-z0-9\\s&.-]+?)(?:\\s+on|\\s+dt|\\.|,|$)",
                datePattern = "(\\d{2}-\\d{2}-\\d{4}\\s+\\d{2}:\\d{2}:\\d{2}|\\d{2}/\\d{2}/\\d{4})",
                typePattern = "(debited|credited|debit|credit|paid|received)",
                accountPattern = "(?:wallet|account)\\s+(?:ending\\s+)?([X\\d]+)",
                isActive = true
            )
        )
        
        defaultPatterns.forEach { pattern ->
            patterns[pattern.id] = pattern
        }
        nextId = defaultPatterns.maxOf { it.id } + 1
    }
}