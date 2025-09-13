package com.expensetracker.data.sms

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import com.expensetracker.domain.model.SmsMessage
import com.expensetracker.domain.permission.PermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for reading SMS messages from the device
 */
@Singleton
class SmsReader @Inject constructor(
    private val context: Context,
    private val permissionManager: PermissionManager
) {
    
    /**
     * Read SMS messages from a specific date range
     */
    suspend fun readSmsMessages(
        fromDate: Date? = null,
        limit: Int = 100
    ): List<SmsMessage> = withContext(Dispatchers.IO) {
        if (!permissionManager.hasSmsPermissions()) {
            return@withContext emptyList()
        }
        
        try {
            val messages = mutableListOf<SmsMessage>()
            val uri = Telephony.Sms.CONTENT_URI
            
            val selection = buildSelection(fromDate)
            val selectionArgs = buildSelectionArgs(fromDate)
            val sortOrder = "${Telephony.Sms.DATE} DESC LIMIT $limit"
            
            val cursor: Cursor? = context.contentResolver.query(
                uri,
                SMS_PROJECTION,
                selection,
                selectionArgs,
                sortOrder
            )
            
            cursor?.use { c ->
                while (c.moveToNext()) {
                    val smsMessage = mapCursorToSmsMessage(c)
                    messages.add(smsMessage)
                }
            }
            
            messages
        } catch (e: Exception) {
            // Log error and return empty list for graceful degradation
            emptyList()
        }
    }
    
    /**
     * Read the most recent SMS messages
     */
    suspend fun readRecentSmsMessages(count: Int = 50): List<SmsMessage> {
        return readSmsMessages(limit = count)
    }
    
    /**
     * Check if SMS reading is available
     */
    fun isSmsReadingAvailable(): Boolean {
        return permissionManager.hasSmsPermissions()
    }
    
    private fun buildSelection(fromDate: Date?): String? {
        return if (fromDate != null) {
            "${Telephony.Sms.DATE} >= ?"
        } else {
            null
        }
    }
    
    private fun buildSelectionArgs(fromDate: Date?): Array<String>? {
        return if (fromDate != null) {
            arrayOf(fromDate.time.toString())
        } else {
            null
        }
    }
    
    private fun mapCursorToSmsMessage(cursor: Cursor): SmsMessage {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms._ID))
        val address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)) ?: ""
        val body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)) ?: ""
        val date = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE))
        val type = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE))
        
        return SmsMessage(
            id = id,
            sender = address,
            body = body,
            timestamp = Date(date),
            type = mapSmsType(type)
        )
    }
    
    private fun mapSmsType(type: Int): SmsMessage.Type {
        return when (type) {
            Telephony.Sms.MESSAGE_TYPE_INBOX -> SmsMessage.Type.RECEIVED
            Telephony.Sms.MESSAGE_TYPE_SENT -> SmsMessage.Type.SENT
            else -> SmsMessage.Type.RECEIVED
        }
    }
    
    companion object {
        private val SMS_PROJECTION = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )
    }
}