package com.expensetracker.suite

import com.expensetracker.e2e.EndToEndUserJourneyTest
import com.expensetracker.integration.ComprehensiveIntegrationTest
import com.expensetracker.regression.RegressionTestSuite
import com.expensetracker.ui.CriticalUserFlowsTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Comprehensive test suite for integration and end-to-end testing
 * 
 * This suite includes:
 * - Integration tests across all modules
 * - End-to-end user journey tests
 * - Critical user flow tests
 * - Regression tests for core functionality
 * 
 * Run this suite to verify complete app functionality before release.
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    ComprehensiveIntegrationTest::class,
    EndToEndUserJourneyTest::class,
    CriticalUserFlowsTest::class,
    RegressionTestSuite::class
)
class IntegrationTestSuite {
    // This class remains empty, it is used only as a holder for the above annotations
}