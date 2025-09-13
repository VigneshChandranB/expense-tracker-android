package com.expensetracker.data.sms

import com.expensetracker.domain.model.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface for extracting transaction information from SMS messages
 */
interface SmsTransactionExtractor {
    suspend fun extractTransaction(smsMessage: SmsMessage): TransactionExtractionResult
    suspend fun registerBankPattern(pattern: SmsPattern)
    suspend fun getBankPatterns(): List<SmsPattern>
    fun calculateConfidenceScore(extractionDetails: ExtractionDetails): Float
}

/**
 * Implementation of SMS transaction extractor with regex-based parsing
 */
@Singleton
class RegexSmsTransactionExtractor @Inject constructor(
    private val bankPatternRegistry: BankPatternRegistry,
    private val accountMappingService: AccountMappingService
) : SmsTransactionExtractor {
    
    override suspend fun extractTransaction(smsMessage: SmsMessage): TransactionExtractionResult {
        val startTime = System.currentTimeMillis()
        
        // Pre-validation: Check if SMS is potentially a transaction SMS
        if (!smsMessage.isPotentialTransactionSms()) {
            return TransactionExtractionResult.failure(
                "SMS does not contain transaction keywords",
                0.0f
            )
        }
        
        // Find matching pattern using registry
        val matchingPattern = bankPatternRegistry.findPatternBySender(smsMessage.sender)
            ?: return TransactionExtractionResult.failure(
                "No matching pattern found for sender: ${smsMessage.sender}",
                0.1f
            )
        
        try {
            // Extract fields using the pattern
            val extractedFields = extractFields(smsMessage.body, matchingPattern)
            
            // Validate extracted fields
            val validationResult = validateExtractedFields(extractedFields)
            if (!validationResult.isValid) {
                return TransactionExtractionResult.failure(
                    "Field validation failed: ${validationResult.errors.joinToString(", ")}",
                    0.2f
                )
            }
            
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
        
        // Enhanced confidence scoring with additional factors
        val factors = ConfidenceFactors(
            amountExtracted = fields.containsKey("amount") && fields["amount"]?.isNotBlank() == true,
            typeExtracted = fields.containsKey("type") && fields["type"]?.isNotBlank() == true,
            merchantExtracted = fields.containsKey("merchant") && fields["merchant"]?.isNotBlank() == true,
            dateExtracted = fields.containsKey("date") && fields["date"]?.isNotBlank() == true,
            accountExtracted = fields.containsKey("account") && fields["account"]?.isNotBlank() == true,
            patternMatched = extractionDetails.matchedPattern != null,
            senderTrusted = extractionDetails.matchedPattern?.let { 
                isKnownBankSender(it.bankName) 
            } ?: false
        )
        
        var baseScore = factors.calculateScore()
        
        // Apply additional scoring factors
        baseScore = applyProcessingTimeBonus(baseScore, extractionDetails.processingTimeMs)
        baseScore = applyFieldQualityBonus(baseScore, fields)
        
        return baseScore.coerceIn(0.0f, 1.0f)
    }
    
    private suspend fun findAssociatedAccount(bankName: String, accountIdentifier: String?): Long? {
        return if (accountIdentifier != null) {
            accountMappingService.findAccountByIdentifier(bankName, accountIdentifier)
        } else {
            null
        }
    }
    
    private fun validateExtractedFields(fields: Map<String, String>): FieldValidationResult {
        val errors = mutableListOf<String>()
        
        // Validate amount
        if (!fields.containsKey("amount") || fields["amount"].isNullOrBlank()) {
            errors.add("Amount not found")
        } else {
            val amount = parseAmount(fields["amount"]!!)
            if (amount == null || amount <= BigDecimal.ZERO) {
                errors.add("Invalid amount format or negative value")
            }
        }
        
        // Validate merchant (optional but should be meaningful if present)
        fields["merchant"]?.let { merchant ->
            if (merchant.length < 2 || merchant.matches(Regex("^[^a-zA-Z]*$"))) {
                errors.add("Invalid merchant name")
            }
        }
        
        return FieldValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }
    
    private fun isKnownBankSender(bankName: String): Boolean {
        val knownBanks = setOf(
            "HDFC Bank", "ICICI Bank", "State Bank of India", "Axis Bank",
            "Kotak Mahindra Bank", "Paytm Payments Bank", "PhonePe", "Google Pay"
        )
        return knownBanks.any { it.equals(bankName, ignoreCase = true) }
    }
    
    private fun applyProcessingTimeBonus(baseScore: Float, processingTimeMs: Long): Float {
        // Bonus for fast processing (under 100ms gets small bonus)
        return if (processingTimeMs < 100) {
            (baseScore + 0.05f).coerceAtMost(1.0f)
        } else {
            baseScore
        }
    }
    
    private fun applyFieldQualityBonus(baseScore: Float, fields: Map<String, String>): Float {
        var bonus = 0.0f
        
        // Bonus for high-quality merchant extraction
        fields["merchant"]?.let { merchant ->
            if (merchant.length > 5 && merchant.contains(Regex("[A-Za-z]"))) {
                bonus += 0.05f
            }
        }
        
        // Bonus for precise amount extraction (with decimals)
        fields["amount"]?.let { amount ->
            if (amount.contains(".")) {
                bonus += 0.02f
            }
        }
        
        return (baseScore + bonus).coerceAtMost(1.0f)
    }
    
    private fun extractFields(smsBody: String, pattern: SmsPattern): Map<String, String> {
        val fields = mutableMapOf<String, String>()
        
        // Extract amount with enhanced patterns
        extractField(smsBody, pattern.amountPattern, "amount")?.let { 
            fields["amount"] = cleanAmountString(it)
        }
        
        // Extract merchant with fallback patterns
        extractField(smsBody, pattern.merchantPattern, "merchant")?.let { 
            fields["merchant"] = cleanMerchantString(it)
        } ?: run {
            // Fallback merchant extraction
            extractMerchantFallback(smsBody)?.let {
                fields["merchant"] = it
            }
        }
        
        // Extract date with multiple format support
        extractField(smsBody, pattern.datePattern, "date")?.let { 
            fields["date"] = it 
        } ?: run {
            // Fallback date extraction
            extractDateFallback(smsBody)?.let {
                fields["date"] = it
            }
        }
        
        // Extract transaction type with enhanced detection
        extractField(smsBody, pattern.typePattern, "type")?.let { 
            fields["type"] = it 
        } ?: run {
            // Fallback type detection
            fields["type"] = detectTransactionTypeFallback(smsBody)
        }
        
        // Extract account (if pattern exists)
        pattern.accountPattern?.let { accountPattern ->
            extractField(smsBody, accountPattern, "account")?.let { 
                fields["account"] = cleanAccountString(it)
            }
        }
        
        return fields
    }
    
    private fun extractField(text: String, patternString: String, fieldName: String): String? {
        return try {
            val pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE)
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
            val merchant = extractedFields["merchant"] ?: extractMerchantFromDescription(smsMessage.body)
            
            // Parse date
            val date = parseDate(extractedFields["date"]) ?: LocalDateTime.now()
            
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
    
    private fun cleanAmountString(amount: String): String {
        return amount.replace(Regex("[₹$,\\s]"), "").trim()
    }
    
    private fun cleanMerchantString(merchant: String): String {
        return merchant.trim()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[^a-zA-Z0-9\\s&.-]"), "")
            .take(50) // Limit length
    }
    
    private fun cleanAccountString(account: String): String {
        return account.replace(Regex("[^X\\d]"), "").trim()
    }
    
    private fun extractMerchantFallback(smsBody: String): String? {
        // Common patterns for merchant extraction
        val patterns = listOf(
            "(?:at|to|from)\\s+([A-Za-z0-9\\s&.-]+?)(?:\\s+on|\\s+dt|\\.|,|$)",
            "(?:paid to|received from)\\s+([A-Za-z0-9\\s&.-]+?)(?:\\s+on|\\.|,|$)",
            "(?:transaction at)\\s+([A-Za-z0-9\\s&.-]+?)(?:\\s+on|\\.|,|$)"
        )
        
        for (patternStr in patterns) {
            extractField(smsBody, patternStr, "merchant")?.let { return it }
        }
        return null
    }
    
    private fun extractDateFallback(smsBody: String): String? {
        val patterns = listOf(
            "(\\d{2}-\\d{2}-\\d{4}\\s+\\d{2}:\\d{2}:\\d{2})",
            "(\\d{2}/\\d{2}/\\d{4}\\s+\\d{2}:\\d{2})",
            "(\\d{2}-\\d{2}-\\d{4})",
            "(\\d{2}/\\d{2}/\\d{4})"
        )
        
        for (patternStr in patterns) {
            extractField(smsBody, patternStr, "date")?.let { return it }
        }
        return null
    }
    
    private fun detectTransactionTypeFallback(smsBody: String): String {
        val bodyLower = smsBody.lowercase()
        return when {
            bodyLower.contains("credited") || bodyLower.contains("received") -> "credit"
            bodyLower.contains("debited") || bodyLower.contains("paid") -> "debit"
            else -> "debit" // Default
        }
    }
    
    private fun extractMerchantFromDescription(description: String): String {
        // Extract merchant from common SMS patterns
        val patterns = listOf(
            "(?:at|to)\\s+([A-Za-z0-9\\s]+?)(?:\\s+on|\\.|$)",
            "(?:paid to)\\s+([A-Za-z0-9\\s]+?)(?:\\s+on|\\.|$)",
            "(?:from)\\s+([A-Za-z0-9\\s]+?)(?:\\s+on|\\.|$)"
        )
        
        for (patternStr in patterns) {
            extractField(description, patternStr, "merchant")?.let { 
                return cleanMerchantString(it)
            }
        }
        
        return "Unknown Merchant"
    }
    
    private fun parseAmount(amountStr: String): BigDecimal? {
        return try {
            // Remove currency symbols and commas, keep only digits and decimal point
            val cleanAmount = amountStr
                .replace(Regex("[₹$,\\s]"), "")
                .replace(Regex("[^\\d.]"), "")
            
            if (cleanAmount.isNotBlank() && cleanAmount.matches(Regex("\\d+(\\.\\d{1,2})?"))) {
                BigDecimal(cleanAmount)
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
    
    private fun parseDate(dateStr: String?): LocalDateTime? {
        if (dateStr.isNullOrBlank()) return null
        
        val dateFormats = listOf(
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd-MM-yy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd-MM-yy HH:mm"),
            DateTimeFormatter.ofPattern("dd/MM/yy HH:mm"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yy"),
            DateTimeFormatter.ofPattern("dd/MM/yy")
        )
        
        for (formatter in dateFormats) {
            try {
                return if (dateStr.contains(":")) {
                    LocalDateTime.parse(dateStr, formatter)
                } else {
                    LocalDateTime.parse(dateStr + " 00:00:00", 
                        DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
                            .withResolverStyle(java.time.format.ResolverStyle.LENIENT))
                }
            } catch (e: Exception) {
                // Try next format
            }
        }
        
        return null
    }
    
}

/**
 * Result of field validation
 */
data class FieldValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)