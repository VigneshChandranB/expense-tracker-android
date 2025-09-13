package com.expensetracker.presentation.accessibility

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.util.*

class AccessibilityUtilsTest {

    @Test
    fun formatCurrencyForAccessibility_formatsCorrectly() {
        val amount = BigDecimal("1234.56")
        val result = AccessibilityUtils.formatCurrencyForAccessibility(amount)
        
        // Result should be a properly formatted currency string
        assertTrue(result.contains("1,234.56") || result.contains("1234.56"))
    }

    @Test
    fun formatPercentageForAccessibility_formatsCorrectly() {
        val percentage = 75.5f
        val result = AccessibilityUtils.formatPercentageForAccessibility(percentage)
        
        assertEquals("75 percent", result)
    }

    @Test
    fun formatPercentageForAccessibility_roundsDown() {
        val percentage = 75.9f
        val result = AccessibilityUtils.formatPercentageForAccessibility(percentage)
        
        assertEquals("75 percent", result)
    }

    @Test
    fun createTransactionContentDescription_income_formatsCorrectly() {
        val result = AccessibilityUtils.createTransactionContentDescription(
            merchant = "Salary Corp",
            amount = BigDecimal("5000.00"),
            category = "Income",
            date = "2024-01-15",
            isIncome = true
        )
        
        assertTrue(result.contains("income"))
        assertTrue(result.contains("Salary Corp"))
        assertTrue(result.contains("Income"))
        assertTrue(result.contains("2024-01-15"))
    }

    @Test
    fun createTransactionContentDescription_expense_formatsCorrectly() {
        val result = AccessibilityUtils.createTransactionContentDescription(
            merchant = "Amazon",
            amount = BigDecimal("150.75"),
            category = "Shopping",
            date = "2024-01-15",
            isIncome = false
        )
        
        assertTrue(result.contains("expense"))
        assertTrue(result.contains("Amazon"))
        assertTrue(result.contains("Shopping"))
        assertTrue(result.contains("2024-01-15"))
    }

    @Test
    fun createAccountBalanceContentDescription_formatsCorrectly() {
        val result = AccessibilityUtils.createAccountBalanceContentDescription(
            accountName = "Main Checking",
            balance = BigDecimal("2500.50"),
            accountType = "Checking"
        )
        
        assertTrue(result.contains("Checking account"))
        assertTrue(result.contains("Main Checking"))
        assertTrue(result.contains("balance"))
    }

    @Test
    fun createCategoryBreakdownContentDescription_formatsCorrectly() {
        val result = AccessibilityUtils.createCategoryBreakdownContentDescription(
            categoryName = "Food & Dining",
            amount = BigDecimal("450.25"),
            percentage = 25.5f
        )
        
        assertTrue(result.contains("Food & Dining"))
        assertTrue(result.contains("25 percent"))
    }

    @Test
    fun formatCurrencyForAccessibility_handlesZero() {
        val amount = BigDecimal.ZERO
        val result = AccessibilityUtils.formatCurrencyForAccessibility(amount)
        
        assertTrue(result.contains("0"))
    }

    @Test
    fun formatCurrencyForAccessibility_handlesNegative() {
        val amount = BigDecimal("-100.50")
        val result = AccessibilityUtils.formatCurrencyForAccessibility(amount)
        
        assertTrue(result.contains("100.50"))
    }

    @Test
    fun formatPercentageForAccessibility_handlesZero() {
        val percentage = 0f
        val result = AccessibilityUtils.formatPercentageForAccessibility(percentage)
        
        assertEquals("0 percent", result)
    }

    @Test
    fun formatPercentageForAccessibility_handlesHundred() {
        val percentage = 100f
        val result = AccessibilityUtils.formatPercentageForAccessibility(percentage)
        
        assertEquals("100 percent", result)
    }

    @Test
    fun createTransactionContentDescription_handlesSpecialCharacters() {
        val result = AccessibilityUtils.createTransactionContentDescription(
            merchant = "McDonald's",
            amount = BigDecimal("12.99"),
            category = "Food & Dining",
            date = "2024-01-15",
            isIncome = false
        )
        
        assertTrue(result.contains("McDonald's"))
        assertTrue(result.contains("Food & Dining"))
    }

    @Test
    fun createAccountBalanceContentDescription_handlesLongNames() {
        val result = AccessibilityUtils.createAccountBalanceContentDescription(
            accountName = "My Very Long Account Name For Testing",
            balance = BigDecimal("1000.00"),
            accountType = "Savings"
        )
        
        assertTrue(result.contains("My Very Long Account Name For Testing"))
        assertTrue(result.contains("Savings account"))
    }

    @Test
    fun createCategoryBreakdownContentDescription_handlesSmallPercentages() {
        val result = AccessibilityUtils.createCategoryBreakdownContentDescription(
            categoryName = "Miscellaneous",
            amount = BigDecimal("5.00"),
            percentage = 0.1f
        )
        
        assertTrue(result.contains("Miscellaneous"))
        assertTrue(result.contains("0 percent"))
    }
}