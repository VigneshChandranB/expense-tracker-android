package com.expensetracker.domain.usecase

import com.expensetracker.domain.model.*
import com.expensetracker.domain.repository.AnalyticsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Unit tests for DetectSpendingAnomaliesUseCase
 * Tests anomaly detection functionality
 */
class DetectSpendingAnomaliesUseCaseTest {

    private lateinit var analyticsRepository: AnalyticsRepository
    private lateinit var detectSpendingAnomaliesUseCase: DetectSpendingAnomaliesUseCase

    @Before
    fun setup() {
        analyticsRepository = mockk()
        detectSpendingAnomaliesUseCase = DetectSpendingAnomaliesUseCase(analyticsRepository)
    }

    @Test
    fun `invoke should return success when repository succeeds`() = runTest {
        // Given
        val dateRange = DateRange.currentMonth()
        val config = createDefaultConfig()
        val expectedAnomalies = createSampleAnomalies()
        coEvery { analyticsRepository.detectSpendingAnomalies(dateRange, config) } returns expectedAnomalies

        // When
        val result = detectSpendingAnomaliesUseCase(dateRange, config)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedAnomalies, result.getOrNull())
        coVerify { analyticsRepository.detectSpendingAnomalies(dateRange, config) }
    }

    @Test
    fun `invoke should return failure when repository throws exception`() = runTest {
        // Given
        val dateRange = DateRange.currentMonth()
        val config = createDefaultConfig()
        val exception = RuntimeException("Database error")
        coEvery { analyticsRepository.detectSpendingAnomalies(dateRange, config) } throws exception

        // When
        val result = detectSpendingAnomaliesUseCase(dateRange, config)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `detectCurrentMonthAnomalies should use current month and default config`() = runTest {
        // Given
        val currentMonth = DateRange.currentMonth()
        val expectedAnomalies = createSampleAnomalies()
        coEvery { 
            analyticsRepository.detectSpendingAnomalies(currentMonth, any()) 
        } returns expectedAnomalies

        // When
        val result = detectSpendingAnomaliesUseCase.detectCurrentMonthAnomalies()

        // Then
        assertTrue(result.isSuccess)
        coVerify { analyticsRepository.detectSpendingAnomalies(currentMonth, any()) }
    }

    @Test
    fun `detectRecentAnomalies should use last 7 days`() = runTest {
        // Given
        val last7Days = DateRange.lastNDays(7)
        val expectedAnomalies = createSampleAnomalies()
        coEvery { 
            analyticsRepository.detectSpendingAnomalies(last7Days, any()) 
        } returns expectedAnomalies

        // When
        val result = detectSpendingAnomaliesUseCase.detectRecentAnomalies()

        // Then
        assertTrue(result.isSuccess)
        coVerify { analyticsRepository.detectSpendingAnomalies(last7Days, any()) }
    }

    @Test
    fun `checkMonthlySpendingAlert should detect spending increase above threshold`() = runTest {
        // Given
        val currentMonth = DateRange.currentMonth()
        val previousMonth = DateRange.previousMonth()
        val threshold = 0.20f // 20%
        
        coEvery { analyticsRepository.getTotalSpending(currentMonth) } returns BigDecimal("12000")
        coEvery { analyticsRepository.getTotalSpending(previousMonth) } returns BigDecimal("10000")

        // When
        val result = detectSpendingAnomaliesUseCase.checkMonthlySpendingAlert(threshold)

        // Then
        assertTrue(result.isSuccess)
        val anomaly = result.getOrNull()
        assertNotNull(anomaly)
        assertEquals(AnomalyType.CATEGORY_SPENDING_SPIKE, anomaly!!.type)
        assertEquals(BigDecimal("12000"), anomaly.actualValue)
        assertTrue(anomaly.description.contains("20%"))
    }

    @Test
    fun `checkMonthlySpendingAlert should return null when increase is below threshold`() = runTest {
        // Given
        val currentMonth = DateRange.currentMonth()
        val previousMonth = DateRange.previousMonth()
        val threshold = 0.20f // 20%
        
        coEvery { analyticsRepository.getTotalSpending(currentMonth) } returns BigDecimal("10500")
        coEvery { analyticsRepository.getTotalSpending(previousMonth) } returns BigDecimal("10000")

        // When
        val result = detectSpendingAnomaliesUseCase.checkMonthlySpendingAlert(threshold)

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `checkMonthlySpendingAlert should return null when previous spending is zero`() = runTest {
        // Given
        val currentMonth = DateRange.currentMonth()
        val previousMonth = DateRange.previousMonth()
        val threshold = 0.20f
        
        coEvery { analyticsRepository.getTotalSpending(currentMonth) } returns BigDecimal("5000")
        coEvery { analyticsRepository.getTotalSpending(previousMonth) } returns BigDecimal.ZERO

        // When
        val result = detectSpendingAnomaliesUseCase.checkMonthlySpendingAlert(threshold)

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `checkMonthlySpendingAlert should set HIGH severity for large increases`() = runTest {
        // Given
        val currentMonth = DateRange.currentMonth()
        val previousMonth = DateRange.previousMonth()
        val threshold = 0.20f
        
        coEvery { analyticsRepository.getTotalSpending(currentMonth) } returns BigDecimal("16000")
        coEvery { analyticsRepository.getTotalSpending(previousMonth) } returns BigDecimal("10000")

        // When
        val result = detectSpendingAnomaliesUseCase.checkMonthlySpendingAlert(threshold)

        // Then
        assertTrue(result.isSuccess)
        val anomaly = result.getOrNull()
        assertNotNull(anomaly)
        assertEquals(AnomalySeverity.HIGH, anomaly!!.severity)
    }

    @Test
    fun `checkMonthlySpendingAlert should set MEDIUM severity for moderate increases`() = runTest {
        // Given
        val currentMonth = DateRange.currentMonth()
        val previousMonth = DateRange.previousMonth()
        val threshold = 0.20f
        
        coEvery { analyticsRepository.getTotalSpending(currentMonth) } returns BigDecimal("13000")
        coEvery { analyticsRepository.getTotalSpending(previousMonth) } returns BigDecimal("10000")

        // When
        val result = detectSpendingAnomaliesUseCase.checkMonthlySpendingAlert(threshold)

        // Then
        assertTrue(result.isSuccess)
        val anomaly = result.getOrNull()
        assertNotNull(anomaly)
        assertEquals(AnomalySeverity.MEDIUM, anomaly!!.severity)
    }

    @Test
    fun `filterBySeverity should filter anomalies correctly`() {
        // Given
        val anomalies = listOf(
            createAnomaly(AnomalySeverity.LOW),
            createAnomaly(AnomalySeverity.MEDIUM),
            createAnomaly(AnomalySeverity.HIGH),
            createAnomaly(AnomalySeverity.CRITICAL)
        )

        // When - filter by MEDIUM and above
        val filtered = detectSpendingAnomaliesUseCase.filterBySeverity(anomalies, AnomalySeverity.MEDIUM)

        // Then
        assertEquals(3, filtered.size)
        assertFalse(filtered.any { it.severity == AnomalySeverity.LOW })
        assertTrue(filtered.any { it.severity == AnomalySeverity.MEDIUM })
        assertTrue(filtered.any { it.severity == AnomalySeverity.HIGH })
        assertTrue(filtered.any { it.severity == AnomalySeverity.CRITICAL })
    }

    @Test
    fun `filterBySeverity should return all anomalies when filtering by LOW`() {
        // Given
        val anomalies = listOf(
            createAnomaly(AnomalySeverity.LOW),
            createAnomaly(AnomalySeverity.MEDIUM),
            createAnomaly(AnomalySeverity.HIGH)
        )

        // When
        val filtered = detectSpendingAnomaliesUseCase.filterBySeverity(anomalies, AnomalySeverity.LOW)

        // Then
        assertEquals(3, filtered.size)
    }

    @Test
    fun `filterBySeverity should return only CRITICAL when filtering by CRITICAL`() {
        // Given
        val anomalies = listOf(
            createAnomaly(AnomalySeverity.LOW),
            createAnomaly(AnomalySeverity.MEDIUM),
            createAnomaly(AnomalySeverity.HIGH),
            createAnomaly(AnomalySeverity.CRITICAL)
        )

        // When
        val filtered = detectSpendingAnomaliesUseCase.filterBySeverity(anomalies, AnomalySeverity.CRITICAL)

        // Then
        assertEquals(1, filtered.size)
        assertEquals(AnomalySeverity.CRITICAL, filtered.first().severity)
    }

    @Test
    fun `filterBySeverity should return empty list when no anomalies match criteria`() {
        // Given
        val anomalies = listOf(
            createAnomaly(AnomalySeverity.LOW),
            createAnomaly(AnomalySeverity.MEDIUM)
        )

        // When
        val filtered = detectSpendingAnomaliesUseCase.filterBySeverity(anomalies, AnomalySeverity.CRITICAL)

        // Then
        assertTrue(filtered.isEmpty())
    }

    @Test
    fun `should handle empty anomaly list correctly`() = runTest {
        // Given
        val dateRange = DateRange.currentMonth()
        val config = createDefaultConfig()
        coEvery { analyticsRepository.detectSpendingAnomalies(dateRange, config) } returns emptyList()

        // When
        val result = detectSpendingAnomaliesUseCase(dateRange, config)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isEmpty())
    }

    private fun createDefaultConfig(): AnomalyDetectionConfig {
        return AnomalyDetectionConfig(
            largeTransactionThreshold = BigDecimal("10000"),
            categorySpikeFactor = 2.0f,
            frequentTransactionCount = 10,
            frequentTransactionTimeWindow = 60,
            duplicateTransactionTimeWindow = 5,
            lateNightStartHour = 23,
            lateNightEndHour = 6
        )
    }

    private fun createSampleAnomalies(): List<SpendingAnomaly> {
        return listOf(
            createAnomaly(AnomalySeverity.HIGH),
            createAnomaly(AnomalySeverity.MEDIUM),
            createAnomaly(AnomalySeverity.LOW)
        )
    }

    private fun createAnomaly(severity: AnomalySeverity): SpendingAnomaly {
        return SpendingAnomaly(
            id = "test_anomaly_${System.currentTimeMillis()}",
            type = AnomalyType.UNUSUAL_LARGE_TRANSACTION,
            severity = severity,
            description = "Test anomaly",
            detectedAt = LocalDateTime.now(),
            relatedTransactions = emptyList(),
            suggestedAction = "Test action",
            threshold = BigDecimal("1000"),
            actualValue = BigDecimal("1500"),
            category = null,
            account = null
        )
    }
}