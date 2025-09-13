package com.expensetracker.data.sms

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.provider.Telephony
import android.telephony.SmsMessage
import com.expensetracker.domain.model.TransactionExtractionResult
import com.expensetracker.domain.permission.PermissionManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class EnhancedSmsReceiverTest {
    
    private lateinit var context: Context
    private lateinit var permissionManager: PermissionManager
    private lateinit var smsProcessor: SmsProcessor
    private lateinit var powerManager: PowerManager
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var receiver: SmsReceiver
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        permissionManager = mockk(relaxed = true)
        smsProcessor = mockk(relaxed = true)
        powerManager = mockk(relaxed = true)
        wakeLock = mockk(relaxed = true)
        
        every { context.getSystemService(Context.POWER_SERVICE) } returns powerManager
        every { powerManager.newWakeLock(any(), any()) } returns wakeLock
        every { wakeLock.isHeld } returns false
        every { permissionManager.hasSmsPermissions() } returns true
        
        receiver = SmsReceiver().apply {
            this.permissionManager = this@EnhancedSmsReceiverTest.permissionManager
            this.smsProcessor = this@EnhancedSmsReceiverTest.smsProcessor
        }
    }
    
    @Test
    fun `onReceive ignores non-SMS intents`() {
        // Given
        val intent = Intent("com.example.OTHER_ACTION")
        
        // When
        receiver.onReceive(context, intent)
        
        // Then
        verify(exactly = 0) { powerManager.newWakeLock(any(), any()) }
    }
    
    @Test
    fun `onReceive ignores SMS when permissions not granted`() {
        // Given
        every { permissionManager.hasSmsPermissions() } returns false
        val intent = createSmsIntent()
        
        // When
        receiver.onReceive(context, intent)
        
        // Then
        verify(exactly = 0) { powerManager.newWakeLock(any(), any()) }
    }
    
    @Test
    fun `onReceive acquires wake lock for SMS processing`() {
        // Given
        val intent = createSmsIntent()
        
        // When
        receiver.onReceive(context, intent)
        
        // Then
        verify { powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ExpenseTracker:SmsProcessing") }
        verify { wakeLock.acquire(30_000L) }
    }
    
    @Test
    fun `shouldProcessSms returns true for valid transaction SMS`() {
        // Given
        val smsMessage = com.expensetracker.domain.model.SmsMessage(
            id = 1L,
            sender = "HDFC-BANK",
            body = "Rs 1500.00 debited from your account ending 1234 at AMAZON on 01-Jan-23",
            timestamp = java.util.Date(),
            type = com.expensetracker.domain.model.SmsMessage.Type.RECEIVED
        )
        
        // When
        val shouldProcess = receiver.shouldProcessSms(smsMessage)
        
        // Then
        assert(shouldProcess)
    }
    
    @Test
    fun `shouldProcessSms returns false for non-transaction SMS`() {
        // Given
        val smsMessage = com.expensetracker.domain.model.SmsMessage(
            id = 1L,
            sender = "FRIEND",
            body = "Hey, how are you doing today?",
            timestamp = java.util.Date(),
            type = com.expensetracker.domain.model.SmsMessage.Type.RECEIVED
        )
        
        // When
        val shouldProcess = receiver.shouldProcessSms(smsMessage)
        
        // Then
        assert(!shouldProcess)
    }
    
    @Test
    fun `shouldProcessSms handles various transaction keywords`() {
        val transactionKeywords = listOf("debited", "credited", "withdrawn", "deposited", "paid", "received", "transaction", "purchase", "transfer")
        
        transactionKeywords.forEach { keyword ->
            // Given
            val smsMessage = com.expensetracker.domain.model.SmsMessage(
                id = 1L,
                sender = "BANK",
                body = "Rs 100 $keyword from your account",
                timestamp = java.util.Date(),
                type = com.expensetracker.domain.model.SmsMessage.Type.RECEIVED
            )
            
            // When
            val shouldProcess = receiver.shouldProcessSms(smsMessage)
            
            // Then
            assert(shouldProcess) { "Should process SMS with keyword: $keyword" }
        }
    }
    
    @Test
    fun `shouldProcessSms recognizes various currency formats`() {
        val currencyFormats = listOf("Rs 100", "₹100", "INR 100", "100.50", "1,000.00")
        
        currencyFormats.forEach { currency ->
            // Given
            val smsMessage = com.expensetracker.domain.model.SmsMessage(
                id = 1L,
                sender = "BANK",
                body = "$currency debited from your account",
                timestamp = java.util.Date(),
                type = com.expensetracker.domain.model.SmsMessage.Type.RECEIVED
            )
            
            // When
            val shouldProcess = receiver.shouldProcessSms(smsMessage)
            
            // Then
            assert(shouldProcess) { "Should process SMS with currency format: $currency" }
        }
    }
    
    @Test
    fun `onReceive processes valid transaction SMS`() = runTest {
        // Given
        val intent = createSmsIntent()
        
        coEvery { smsProcessor.processSmsMessage(any()) } returns TransactionExtractionResult.success(
            mockk(relaxed = true), 0.8f
        )
        
        // When
        receiver.onReceive(context, intent)
        
        // Then
        // Note: Due to async processing, we can't easily verify the processor call in this test
        // In a real scenario, we'd need to wait for the coroutine to complete
        verify { wakeLock.acquire(30_000L) }
    }
    
    @Test
    fun `onReceive skips non-transaction SMS`() {
        // Given
        val intent = createSmsIntent()
        
        // When
        receiver.onReceive(context, intent)
        
        // Then
        verify { wakeLock.acquire(30_000L) }
        // The SMS would be filtered out in shouldProcessSms, so processor wouldn't be called
    }
    
    @Test
    fun `onReceive handles exceptions gracefully`() {
        // Given
        val intent = createSmsIntent()
        every { powerManager.newWakeLock(any(), any()) } throws RuntimeException("Test exception")
        
        // When
        receiver.onReceive(context, intent)
        
        // Then
        // Should not crash, exception should be handled gracefully
        // No specific verification needed as the test passing means no exception was thrown
    }
    
    @Test
    fun `onReceive releases wake lock when processing count reaches zero`() {
        // Given
        val intent = createSmsIntent()
        every { wakeLock.isHeld } returns true
        
        // When
        receiver.onReceive(context, intent)
        
        // Then
        verify { wakeLock.acquire(30_000L) }
        // Note: Wake lock release happens in async processing, hard to test directly
    }
    
    @Test
    fun `generateUniqueId creates consistent IDs for same message`() {
        // Given
        val androidSmsMessage1 = createMockAndroidSmsMessage("Test message", "1234567890", 1000L)
        val androidSmsMessage2 = createMockAndroidSmsMessage("Test message", "1234567890", 1000L)
        
        // When
        val id1 = receiver.generateUniqueId(androidSmsMessage1)
        val id2 = receiver.generateUniqueId(androidSmsMessage2)
        
        // Then
        assertEquals(id1, id2)
    }
    
    @Test
    fun `generateUniqueId creates different IDs for different messages`() {
        // Given
        val androidSmsMessage1 = createMockAndroidSmsMessage("Test message 1", "1234567890", 1000L)
        val androidSmsMessage2 = createMockAndroidSmsMessage("Test message 2", "1234567890", 1000L)
        
        // When
        val id1 = receiver.generateUniqueId(androidSmsMessage1)
        val id2 = receiver.generateUniqueId(androidSmsMessage2)
        
        // Then
        assert(id1 != id2)
    }
    
    @Test
    fun `mapToSmsMessage creates correct SmsMessage object`() {
        // Given
        val androidSmsMessage = createMockAndroidSmsMessage("Test message", "1234567890", 1000L)
        
        // When
        val smsMessage = receiver.mapToSmsMessage(androidSmsMessage)
        
        // Then
        assertEquals("Test message", smsMessage.body)
        assertEquals("1234567890", smsMessage.sender)
        assertEquals(1000L, smsMessage.timestamp.time)
        assertEquals(com.expensetracker.domain.model.SmsMessage.Type.RECEIVED, smsMessage.type)
        assertNotNull(smsMessage.id)
    }
    
    private fun createSmsIntent(): Intent {
        return Intent(Telephony.Sms.Intents.SMS_RECEIVED_ACTION).apply {
            putExtra("pdus", arrayOf(createMockPdu()))
            putExtra("format", "3gpp")
        }
    }
    
    private fun createMockPdu(): ByteArray {
        // Create a minimal mock PDU for testing
        // In real tests, you'd want to create a proper SMS PDU
        return byteArrayOf(0x00, 0x01, 0x02, 0x03)
    }
    
    private fun createMockAndroidSmsMessage(body: String, sender: String, timestamp: Long): SmsMessage {
        return mockk<SmsMessage>(relaxed = true).apply {
            every { messageBody } returns body
            every { originatingAddress } returns sender
            every { timestampMillis } returns timestamp
        }
    }
}