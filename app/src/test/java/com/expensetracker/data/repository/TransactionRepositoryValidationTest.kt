package com.expensetracker.data.repository

import com.expensetracker.data.local.dao.CategoryDao
import com.expensetracker.data.local.dao.TransactionDao
import com.expensetracker.domain.repository.TransactionRepository
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test

/**
 * Basic validation tests to ensure repository implementation is correctly structured
 */
class TransactionRepositoryValidationTest {
    
    @Test
    fun `repository implementation should implement interface correctly`() {
        // Arrange
        val transactionDao: TransactionDao = mockk()
        val categoryDao: CategoryDao = mockk()
        
        // Act
        val repository: TransactionRepository = TransactionRepositoryImpl(transactionDao, categoryDao)
        
        // Assert
        assertNotNull("Repository should be instantiated", repository)
        assertTrue("Repository should implement TransactionRepository interface", 
            repository is TransactionRepository)
    }
    
    @Test
    fun `repository should have all required methods`() {
        val transactionDao: TransactionDao = mockk()
        val categoryDao: CategoryDao = mockk()
        val repository = TransactionRepositoryImpl(transactionDao, categoryDao)
        
        // Verify all interface methods are implemented by checking they exist
        val methods = repository::class.java.methods
        val methodNames = methods.map { it.name }.toSet()
        
        val requiredMethods = setOf(
            "observeAllTransactions",
            "observeTransactionsByAccount", 
            "getTransactionById",
            "getTransactionsByDateRange",
            "getTransactionsByCategory",
            "insertTransaction",
            "insertTransactions",
            "updateTransaction",
            "deleteTransaction",
            "createTransfer",
            "linkTransferTransactions"
        )
        
        requiredMethods.forEach { methodName ->
            assertTrue("Repository should have method: $methodName", 
                methodNames.contains(methodName))
        }
    }
}