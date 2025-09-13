package com.expensetracker.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Telephony
import android.telephony.SmsMessage
import com.expensetracker.domain.permission.PermissionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

/**
 * Enhanced broadcast receiver for incoming SMS messages with efficient processing and error handling
 */
@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var permissionManager: PermissionManager
    
    @Inject
    lateinit var smsProcessor: SmsProcessor
    
    private val receiverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val processingCount = AtomicInteger(0)
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION || context == null) {
            return
        }
        
        // Check permissions first
        if (!permissionManager.hasSmsPermissions()) {
            return
        }
        
        // Acquire wake lock to ensure processing completes
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ExpenseTracker:SmsProcessing"
        )
        
        try {
            wakeLock.acquire(30_000L) // 30 seconds timeout
            
            val smsMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            smsMessages?.forEach { androidSmsMessage ->
                val smsMessage = mapToSmsMessage(androidSmsMessage)
                
                // Quick pre-filtering to avoid unnecessary processing
                if (shouldProcessSms(smsMessage)) {
                    processSmsAsync(smsMessage, wakeLock)
                }
            }
            
        } catch (e: Exception) {
            // Log error but don't crash the receiver
            handleReceiverError("SMS receiver error", e)
        } finally {
            // Release wake lock if no processing is ongoing
            if (processingCount.get() == 0 && wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }
    
    internal fun shouldProcessSms(smsMessage: com.expensetracker.domain.model.SmsMessage): Boolean {
        return try {
            // Quick pre-filtering checks to avoid unnecessary processing
            val body = smsMessage.body.lowercase()
            val sender = smsMessage.sender.lowercase()
            
            // Check for common transaction keywords
            val hasTransactionKeywords = body.contains("debited") || 
                                       body.contains("credited") || 
                                       body.contains("withdrawn") ||
                                       body.contains("deposited") ||
                                       body.contains("paid") ||
                                       body.contains("received") ||
                                       body.contains("transaction") ||
                                       body.contains("purchase") ||
                                       body.contains("transfer")
            
            // Check for currency symbols or amount patterns
            val hasCurrencyInfo = body.contains("rs") || 
                                body.contains("₹") || 
                                body.contains("inr") ||
                                body.matches(Regex(".*\\d+\\.?\\d*.*"))
            
            // Check if sender looks like a bank or financial institution
            val isFromFinancialInstitution = sender.contains("bank") ||
                                           sender.contains("card") ||
                                           sender.contains("pay") ||
                                           sender.matches(Regex(".*[a-z]{2,4}-?[a-z]{2,6}.*")) ||
                                           sender.length <= 6 // Short codes are often from banks
            
            hasTransactionKeywords && hasCurrencyInfo && isFromFinancialInstitution
            
        } catch (e: Exception) {
            // If any error occurs in filtering, err on the side of caution and process
            handleReceiverError("SMS filtering error", e)
            true
        }
    }
    
    private fun processSmsAsync(
        smsMessage: com.expensetracker.domain.model.SmsMessage,
        wakeLock: PowerManager.WakeLock
    ) {
        processingCount.incrementAndGet()
        
        receiverScope.launch {
            try {
                // Use timeout to prevent hanging
                withTimeout(SMS_PROCESSING_TIMEOUT) {
                    smsProcessor.processSmsMessage(smsMessage)
                }
            } catch (e: Exception) {
                handleProcessingError("SMS processing failed", e, smsMessage)
            } finally {
                val remaining = processingCount.decrementAndGet()
                
                // Release wake lock when all processing is complete
                if (remaining == 0 && wakeLock.isHeld) {
                    wakeLock.release()
                }
            }
        }
    }
    
    private fun handleReceiverError(message: String, exception: Exception) {
        // Log error for debugging
        exception.printStackTrace()
        
        // Could send error metrics or notifications here
        // For now, just ensure graceful handling
    }
    
    private fun handleProcessingError(
        message: String, 
        exception: Exception, 
        smsMessage: com.expensetracker.domain.model.SmsMessage
    ) {
        // Log error with SMS context for debugging
        exception.printStackTrace()
        
        // Could implement retry logic or error reporting here
        // For now, just ensure graceful handling
    }
    
    private fun mapToSmsMessage(androidSmsMessage: SmsMessage): com.expensetracker.domain.model.SmsMessage {
        return com.expensetracker.domain.model.SmsMessage(
            id = generateUniqueId(androidSmsMessage),
            sender = androidSmsMessage.originatingAddress ?: "",
            body = androidSmsMessage.messageBody ?: "",
            timestamp = Date(androidSmsMessage.timestampMillis),
            type = com.expensetracker.domain.model.SmsMessage.Type.RECEIVED
        )
    }
    
    private fun generateUniqueId(androidSmsMessage: SmsMessage): Long {
        // Generate a more unique ID based on message content and timestamp
        val contentHash = androidSmsMessage.messageBody?.hashCode() ?: 0
        val senderHash = androidSmsMessage.originatingAddress?.hashCode() ?: 0
        val timestamp = androidSmsMessage.timestampMillis
        
        return (timestamp + contentHash + senderHash).toLong()
    }
    
    companion object {
        private const val SMS_PROCESSING_TIMEOUT = 15_000L // 15 seconds
    }
}