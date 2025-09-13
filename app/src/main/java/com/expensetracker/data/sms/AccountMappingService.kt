package com.expensetracker.data.sms

import com.expensetracker.domain.model.Account
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data class representing mapping between SMS account identifiers and user accounts
 */
data class AccountMapping(
    val id: Long = 0,
    val accountId: Long,
    val bankName: String,
    val accountIdentifier: String, // The identifier found in SMS (e.g., "XXXX1234")
    val isActive: Boolean = true
)

/**
 * Interface for managing account mappings between SMS identifiers and user accounts
 */
interface AccountMappingService {
    suspend fun createMapping(accountId: Long, bankName: String, accountIdentifier: String)
    suspend fun findAccountByIdentifier(bankName: String, accountIdentifier: String): Long?
    suspend fun getMappingsForAccount(accountId: Long): List<AccountMapping>
    suspend fun getAllMappings(): List<AccountMapping>
    suspend fun updateMapping(mapping: AccountMapping)
    suspend fun deleteMapping(mappingId: Long)
    suspend fun deactivateMapping(mappingId: Long)
    suspend fun activateMapping(mappingId: Long)
}

/**
 * In-memory implementation of account mapping service
 * In a real app, this would be backed by a database
 */
@Singleton
class InMemoryAccountMappingService @Inject constructor() : AccountMappingService {
    
    private val mappings = mutableMapOf<Long, AccountMapping>()
    private var nextId = 1L
    
    override suspend fun createMapping(accountId: Long, bankName: String, accountIdentifier: String) {
        // Check if mapping already exists
        val existingMapping = mappings.values.find { 
            it.accountId == accountId && 
            it.bankName.equals(bankName, ignoreCase = true) && 
            it.accountIdentifier == accountIdentifier 
        }
        
        if (existingMapping == null) {
            val mapping = AccountMapping(
                id = nextId++,
                accountId = accountId,
                bankName = bankName,
                accountIdentifier = accountIdentifier,
                isActive = true
            )
            mappings[mapping.id] = mapping
        }
    }
    
    override suspend fun findAccountByIdentifier(bankName: String, accountIdentifier: String): Long? {
        return mappings.values
            .filter { it.isActive }
            .find { 
                it.bankName.equals(bankName, ignoreCase = true) && 
                it.accountIdentifier == accountIdentifier 
            }?.accountId
    }
    
    override suspend fun getMappingsForAccount(accountId: Long): List<AccountMapping> {
        return mappings.values.filter { it.accountId == accountId }
    }
    
    override suspend fun getAllMappings(): List<AccountMapping> {
        return mappings.values.toList()
    }
    
    override suspend fun updateMapping(mapping: AccountMapping) {
        if (mappings.containsKey(mapping.id)) {
            mappings[mapping.id] = mapping
        }
    }
    
    override suspend fun deleteMapping(mappingId: Long) {
        mappings.remove(mappingId)
    }
    
    override suspend fun deactivateMapping(mappingId: Long) {
        mappings[mappingId]?.let { mapping ->
            mappings[mappingId] = mapping.copy(isActive = false)
        }
    }
    
    override suspend fun activateMapping(mappingId: Long) {
        mappings[mappingId]?.let { mapping ->
            mappings[mappingId] = mapping.copy(isActive = true)
        }
    }
}

/**
 * Enhanced SMS transaction extractor with account mapping support
 */
@Singleton
class AccountAwareSmsTransactionExtractor @Inject constructor(
    private val bankPatternRegistry: BankPatternRegistry,
    private val accountMappingService: AccountMappingService
) : SmsTransactionExtractor {
    
    override suspend fun extractTransaction(smsMessage: SmsMessage): TransactionExtractionResult {
        val startTime = System.currentTimeMillis()
        
        // Find matching pattern using registry
        val matchingPattern = bankPatternRegistry.findPatternBySender(smsMessage.sender)
            ?: return TransactionExtractionResult.failure(
                "No matching pattern found for sender: ${smsMessage.sender}",
                0.1f
            )
        
        try {
            // Extract fields using the pattern
            val extractedFields = extractFields(smsMessage.body, matchingPattern)
            
            // Find associated account
            val accountId = findAssociatedAccount(matchingPattern.bankName, extractedFields["account"])
            
            // Build transaction from extracted fields
            val transaction = buildTransaction(extractedFields, smsMessage, accountId)
            
            val processingTime = System.currentTimeMillis() - startTime
            val extractionDetails = ExtractionDetails(
                extractedFields = extractedFields,
                matchedPattern = matchingPattern,
                processingTimeMs = processingTime
            )
            
            val confidenceScore = calculateConfidenceScore(extractionDetails)
            
            return if (transaction != null) {
                TransactionExtractionResult.success(transaction, confidenceScore, extractionDetails)
            } else {
                TransactionExtractionResult.failure(
                    "Failed to build transaction from extracted fields",
                    confidenceScore
                )
            }
            
        } catch (e: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            return TransactionExtractionResult.failure(
                "Extraction failed: ${e.message}",
                0.1f
            )
        }
    }
    
    override suspend fun registerBankPattern(pattern: SmsPattern) {
        bankPatternRegistry.registerPattern(pattern)
    }
    
    override suspend fun getBankPatterns(): List<SmsPattern> {
        return bankPatternRegistry.getAllPatterns()
    }
    
    override fun calculateConfidenceScore(extractionDetails: ExtractionDetails): Float {
        val fields = extractionDetails.extractedFields
        
        val factors = ConfidenceFactors(
            amountExtracted = fields.containsKey("amount") && fields["amount"]?.isNotBlank() == true,
            typeExtracted = fields.containsKey("type") && fields["type"]?.isNotBlank() == true,
            merchantExtracted = fields.containsKey("merchant") && fields["merchant"]?.isNotBlank() == true,
            dateExtracted = fields.containsKey("date") && fields["date"]?.isNotBlank() == true,
            accountExtracted = fields.containsKey("account") && fields["account"]?.isNotBlank() == true,
            patternMatched = extractionDetails.matchedPattern != null,
            senderTrusted = true // Assume sender is trusted if pattern matched
        )
        
        return factors.calculateScore()
    }
    
    private suspend fun findAssociatedAccount(bankName: String, accountIdentifier: String?): Long? {
        return if (accountIdentifier != null) {
            accountMappingService.findAccountByIdentifier(bankName, accountIdentifier)
        } else {
            null
        }
    }
    
    private fun extractFields(smsBody: String, pattern: SmsPattern): Map<String, String> {
        val fields = mutableMapOf<String, String>()
        
        // Extract amount
        extractField(smsBody, pattern.amountPattern, "amount")?.let { 
            fields["amount"] = it 
        }
        
        // Extract merchant
        extractField(smsBody, pattern.merchantPattern, "merchant")?.let { 
            fields["merchant"] = it 
        }
        
        // Extract date
        extractField(smsBody, pattern.datePattern, "date")?.let { 
            fields["date"] = it 
        }
        
        // Extract transaction type
        extractField(smsBody, pattern.typePattern, "type")?.let { 
            fields["type"] = it 
        }
        
        // Extract account (if pattern exists)
        pattern.accountPattern?.let { accountPattern ->
            extractField(smsBody, accountPattern, "account")?.let { 
                fields["account"] = it 
            }
        }
        
        return fields
    }
    
    private fun extractField(text: String, patternString: String, fieldName: String): String? {
        return try {
            val pattern = java.util.regex.Pattern.compile(patternString, java.util.regex.Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(text)
            
            if (matcher.find()) {
                // Try to get named group first, then first group, then full match
                when {
                    matcher.groupCount() > 0 -> matcher.group(1)?.trim()
                    else -> matcher.group(0)?.trim()
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun buildTransaction(
        extractedFields: Map<String, String>,
        smsMessage: SmsMessage,
        accountId: Long?
    ): ExtractedTransaction? {
        return try {
            // Parse amount
            val amountStr = extractedFields["amount"] ?: return null
            val amount = parseAmount(amountStr) ?: return null
            
            // Parse transaction type
            val type = parseTransactionType(extractedFields["type"], smsMessage.body)
            
            // Get merchant
            val merchant = extractedFields["merchant"] ?: "Unknown"
            
            // Parse date
            val date = parseDate(extractedFields["date"]) ?: java.time.LocalDateTime.now()
            
            // Get account identifier
            val accountIdentifier = extractedFields["account"]
            
            ExtractedTransaction(
                amount = amount,
                type = type,
                merchant = merchant,
                date = date,
                accountIdentifier = accountIdentifier,
                description = smsMessage.body,
                source = TransactionSource.SMS_AUTO
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseAmount(amountStr: String): java.math.BigDecimal? {
        return try {
            // Remove currency symbols and commas
            val cleanAmount = amountStr
                .replace(Regex("[₹$,\\s]"), "")
                .replace(Regex("[^\\d.]"), "")
            
            if (cleanAmount.isNotBlank()) {
                java.math.BigDecimal(cleanAmount)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseTransactionType(typeStr: String?, smsBody: String): TransactionType {
        val bodyLower = smsBody.lowercase()
        val typeLower = typeStr?.lowercase() ?: ""
        
        return when {
            typeLower.contains("credit") || 
            bodyLower.contains("credited") || 
            bodyLower.contains("received") ||
            bodyLower.contains("deposited") -> TransactionType.INCOME
            
            typeLower.contains("debit") || 
            bodyLower.contains("debited") || 
            bodyLower.contains("withdrawn") ||
            bodyLower.contains("paid") -> TransactionType.EXPENSE
            
            bodyLower.contains("transfer") -> {
                if (bodyLower.contains("received") || bodyLower.contains("credited")) {
                    TransactionType.TRANSFER_IN
                } else {
                    TransactionType.TRANSFER_OUT
                }
            }
            
            else -> TransactionType.EXPENSE // Default to expense
        }
    }
    
    private fun parseDate(dateStr: String?): java.time.LocalDateTime? {
        if (dateStr.isNullOrBlank()) return null
        
        val dateFormats = listOf(
            java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"),
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            java.time.format.DateTimeFormatter.ofPattern("dd-MM-yy HH:mm"),
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yy HH:mm"),
            java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
        )
        
        for (formatter in dateFormats) {
            try {
                return java.time.LocalDateTime.parse(dateStr, formatter)
            } catch (e: Exception) {
                // Try next format
            }
        }
        
        return null
    }
}