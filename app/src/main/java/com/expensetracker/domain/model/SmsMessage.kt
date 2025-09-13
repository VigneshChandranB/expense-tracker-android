package com.expensetracker.domain.model

import java.util.Date

/**
 * Domain model representing an SMS message
 */
data class SmsMessage(
    val id: Long,
    val sender: String,
    val body: String,
    val timestamp: Date,
    val type: Type = Type.RECEIVED
) {
    
    /**
     * SMS message type
     */
    enum class Type {
        RECEIVED,
        SENT
    }
    
    /**
     * Check if this SMS might contain transaction information
     */
    fun isPotentialTransactionSms(): Boolean {
        val transactionKeywords = listOf(
            "debited", "credited", "withdrawn", "deposited", 
            "paid", "received", "transfer", "transaction",
            "balance", "account", "bank", "atm", "pos",
            "upi", "imps", "neft", "rtgs"
        )
        
        val bodyLowerCase = body.lowercase()
        return transactionKeywords.any { keyword ->
            bodyLowerCase.contains(keyword)
        }
    }
    
    /**
     * Check if sender appears to be from a bank or financial institution
     */
    fun isFromBankOrFinancialInstitution(): Boolean {
        val bankKeywords = listOf(
            "bank", "hdfc", "icici", "sbi", "axis", "kotak",
            "paytm", "phonepe", "gpay", "amazonpay", "mobikwik",
            "freecharge", "airtel", "jio", "vodafone"
        )
        
        val senderLowerCase = sender.lowercase()
        return bankKeywords.any { keyword ->
            senderLowerCase.contains(keyword)
        }
    }
}