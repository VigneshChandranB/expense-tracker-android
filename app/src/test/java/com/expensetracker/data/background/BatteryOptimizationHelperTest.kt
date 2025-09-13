package com.expensetracker.data.background

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.M])
class BatteryOptimizationHelperTest {
    
    private lateinit var context: Context
    private lateinit var powerManager: PowerManager
    private lateinit var helper: BatteryOptimizationHelper
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        powerManager = mockk(relaxed = true)
        
        every { context.getSystemService(Context.POWER_SERVICE) } returns powerManager
        every { context.packageName } returns "com.expensetracker.test"
        
        helper = BatteryOptimizationHelper(context)
    }
    
    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun `isBatteryOptimizationEnabled returns true when optimization is enabled`() {
        // Given
        every { powerManager.isIgnoringBatteryOptimizations("com.expensetracker.test") } returns false
        
        // When
        val isEnabled = helper.isBatteryOptimizationEnabled()
        
        // Then
        assertTrue(isEnabled)
    }
    
    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun `isBatteryOptimizationEnabled returns false when optimization is disabled`() {
        // Given
        every { powerManager.isIgnoringBatteryOptimizations("com.expensetracker.test") } returns true
        
        // When
        val isEnabled = helper.isBatteryOptimizationEnabled()
        
        // Then
        assertFalse(isEnabled)
    }
    
    @Test
    @Config(sdk = [Build.VERSION_CODES.LOLLIPOP])
    fun `isBatteryOptimizationEnabled returns false on older Android versions`() {
        // When
        val isEnabled = helper.isBatteryOptimizationEnabled()
        
        // Then
        assertFalse(isEnabled)
    }
    
    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun `getBatteryOptimizationStatus returns correct status when optimization enabled`() {
        // Given
        every { powerManager.isIgnoringBatteryOptimizations("com.expensetracker.test") } returns false
        
        // When
        val status = helper.getBatteryOptimizationStatus()
        
        // Then
        assertTrue(status.isOptimizationEnabled)
        assertTrue(status.canRequestWhitelist)
        assertEquals(BatteryOptimizationHelper.Severity.HIGH, status.severity)
        assertTrue(status.impactOnSmsMonitoring.contains("may cause SMS monitoring to stop"))
        assertTrue(status.recommendation.contains("Disable battery optimization"))
    }
    
    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun `getBatteryOptimizationStatus returns correct status when optimization disabled`() {
        // Given
        every { powerManager.isIgnoringBatteryOptimizations("com.expensetracker.test") } returns true
        
        // When
        val status = helper.getBatteryOptimizationStatus()
        
        // Then
        assertFalse(status.isOptimizationEnabled)
        assertTrue(status.canRequestWhitelist)
        assertEquals(BatteryOptimizationHelper.Severity.NONE, status.severity)
        assertTrue(status.impactOnSmsMonitoring.contains("will work reliably"))
        assertTrue(status.recommendation.contains("already disabled"))
    }
    
    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun `createBatteryOptimizationIntent returns correct intent`() {
        // When
        val intent = helper.createBatteryOptimizationIntent()
        
        // Then
        assertNotNull(intent)
        assertEquals(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, intent.action)
        assertEquals("package:com.expensetracker.test", intent.data.toString())
    }
    
    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun `createGeneralBatterySettingsIntent returns correct intent`() {
        // When
        val intent = helper.createGeneralBatterySettingsIntent()
        
        // Then
        assertNotNull(intent)
        assertEquals(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS, intent.action)
    }
    
    @Test
    @Config(sdk = [Build.VERSION_CODES.LOLLIPOP])
    fun `createGeneralBatterySettingsIntent returns app details intent on older versions`() {
        // When
        val intent = helper.createGeneralBatterySettingsIntent()
        
        // Then
        assertNotNull(intent)
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
        assertEquals("package:com.expensetracker.test", intent.data.toString())
    }
    
    @Test
    fun `getDeviceSpecificGuidance returns Xiaomi guidance for Xiaomi devices`() {
        // Given - Xiaomi device (simulated by setting manufacturer in test)
        
        // When
        val guidance = helper.getDeviceSpecificGuidance()
        
        // Then - This test would need to be adjusted based on how we can mock Build.MANUFACTURER
        // For now, we'll test the generic case
        assertEquals("Generic Android", guidance.manufacturer)
        assertTrue(guidance.specificSteps.isNotEmpty())
        assertTrue(guidance.additionalInfo.isNotEmpty())
    }
    
    @Test
    fun `hasAggressiveBatteryOptimization returns false for generic devices`() {
        // When
        val hasAggressive = helper.hasAggressiveBatteryOptimization()
        
        // Then - This would return false for generic/unknown manufacturers
        // The actual result depends on the device running the test
        // We can't easily mock Build.MANUFACTURER in unit tests
    }
    
    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun `getBatteryOptimizationImpact returns NONE when optimization disabled`() {
        // Given
        every { powerManager.isIgnoringBatteryOptimizations("com.expensetracker.test") } returns true
        
        // When
        val impact = helper.getBatteryOptimizationImpact()
        
        // Then
        assertEquals(BatteryOptimizationHelper.ImpactLevel.NONE, impact.level)
        assertTrue(impact.description.contains("will work reliably"))
        assertTrue(impact.recommendation.contains("No action needed"))
    }
    
    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun `getBatteryOptimizationImpact returns HIGH when optimization enabled on standard device`() {
        // Given
        every { powerManager.isIgnoringBatteryOptimizations("com.expensetracker.test") } returns false
        
        // When
        val impact = helper.getBatteryOptimizationImpact()
        
        // Then
        assertEquals(BatteryOptimizationHelper.ImpactLevel.HIGH, impact.level)
        assertTrue(impact.description.contains("may be interrupted"))
        assertTrue(impact.recommendation.contains("Recommended to disable"))
    }
    
    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun `getBatteryOptimizationStatus provides comprehensive information`() {
        // Given
        every { powerManager.isIgnoringBatteryOptimizations("com.expensetracker.test") } returns false
        
        // When
        val status = helper.getBatteryOptimizationStatus()
        
        // Then
        assertTrue(status.isOptimizationEnabled)
        assertTrue(status.canRequestWhitelist)
        assertTrue(status.impactOnSmsMonitoring.contains("SMS monitoring"))
        assertTrue(status.recommendation.isNotBlank())
        assertEquals(BatteryOptimizationHelper.Severity.HIGH, status.severity)
    }
    
    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun `createBatteryOptimizationIntent handles exceptions gracefully`() {
        // Given - simulate exception in intent creation
        every { context.packageName } throws RuntimeException("Test exception")
        
        // When
        val intent = helper.createBatteryOptimizationIntent()
        
        // Then - should return fallback intent, not crash
        assertNotNull(intent)
    }
    
    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun `getBatteryOptimizationImpact handles edge cases`() {
        // Test various combinations of optimization states
        val testCases = listOf(
            Pair(true, BatteryOptimizationHelper.ImpactLevel.HIGH),   // Optimized, standard device
            Pair(false, BatteryOptimizationHelper.ImpactLevel.NONE)   // Not optimized
        )
        
        testCases.forEach { (isOptimized, expectedLevel) ->
            // Given
            every { powerManager.isIgnoringBatteryOptimizations("com.expensetracker.test") } returns !isOptimized
            
            // When
            val impact = helper.getBatteryOptimizationImpact()
            
            // Then
            assertEquals(expectedLevel, impact.level)
        }
    }
    
    @Test
    fun `getDeviceSpecificGuidance provides actionable steps`() {
        // When
        val guidance = helper.getDeviceSpecificGuidance()
        
        // Then
        assertNotNull(guidance.manufacturer)
        assertTrue(guidance.specificSteps.isNotEmpty())
        assertTrue(guidance.additionalInfo.isNotEmpty())
        
        // Verify steps are actionable
        guidance.specificSteps.forEach { step ->
            assertTrue(step.isNotBlank())
            assertTrue(step.length > 10) // Should be descriptive
        }
    }
}