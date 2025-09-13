package com.expensetracker.domain.usecase

import com.expensetracker.domain.model.*
import com.expensetracker.domain.repository.TransactionRepository
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for TransactionUndoRedoUseCase
 */
class TransactionUndoRedoUseCaseTest {
    
    private val transactionRepository = mockk<TransactionRepository>()
    private lateinit var useCase: TransactionUndoRedoUseCase
    
    private val testTransaction = Transaction(
        id = 1L,
        amount = BigDecimal("50.00"),
        type = TransactionType.EXPENSE,
        category = Category(1L, "Food", "#FF0000", "food"),
        merchant = "Test Restaurant",
        description = "Lunch",
        date = LocalDateTime.now(),
        source = TransactionSource.MANUAL,
        accountId = 1L
    )
    
    @Before
    fun setup() {
        useCase = TransactionUndoRedoUseCase(transactionRepository)
    }
    
    @Test
    fun `initial state should have no undo or redo available`() = runTest {
        // Then
        assertFalse(useCase.canUndo.first())
        assertFalse(useCase.canRedo.first())
    }
    
    @Test
    fun `recordOperation should enable undo and clear redo`() = runTest {
        // Given
        val operation = TransactionOperation.Create(testTransaction, 1L)
        
        // When
        useCase.recordOperation(operation)
        
        // Then
        assertTrue(useCase.canUndo.first())
        assertFalse(useCase.canRedo.first())
    }
    
    @Test
    fun `undo create operation should delete transaction`() = runTest {
        // Given
        val operation = TransactionOperation.Create(testTransaction, 1L)
        useCase.recordOperation(operation)
        
        coEvery { transactionRepository.deleteTransaction(1L) } just Runs
        
        // When
        val result = useCase.undo()
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { transactionRepository.deleteTransaction(1L) }
        assertFalse(useCase.canUndo.first())
        assertTrue(useCase.canRedo.first())
    }
    
    @Test
    fun `undo update operation should restore original transaction`() = runTest {
        // Given
        val originalTransaction = testTransaction
        val updatedTransaction = testTransaction.copy(merchant = "Updated Restaurant")
        val operation = TransactionOperation.Update(originalTransaction, updatedTransaction)
        useCase.recordOperation(operation)
        
        coEvery { transactionRepository.updateTransaction(originalTransaction) } just Runs
        
        // When
        val result = useCase.undo()
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { transactionRepository.updateTransaction(originalTransaction) }
    }
    
    @Test
    fun `undo delete operation should recreate transaction`() = runTest {
        // Given
        val operation = TransactionOperation.Delete(testTransaction)
        useCase.recordOperation(operation)
        
        coEvery { transactionRepository.insertTransaction(testTransaction) } returns 2L
        
        // When
        val result = useCase.undo()
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { transactionRepository.insertTransaction(testTransaction) }
    }
    
    @Test
    fun `undo transfer operation should delete both transactions`() = runTest {
        // Given
        val fromTransaction = testTransaction.copy(type = TransactionType.TRANSFER_OUT)
        val toTransaction = testTransaction.copy(id = 2L, type = TransactionType.TRANSFER_IN)
        val operation = TransactionOperation.Transfer(fromTransaction, toTransaction, 1L, 2L)
        useCase.recordOperation(operation)
        
        coEvery { transactionRepository.deleteTransaction(1L) } just Runs
        coEvery { transactionRepository.deleteTransaction(2L) } just Runs
        
        // When
        val result = useCase.undo()
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { 
            transactionRepository.deleteTransaction(1L)
            transactionRepository.deleteTransaction(2L)
        }
    }
    
    @Test
    fun `redo create operation should recreate transaction`() = runTest {
        // Given
        val operation = TransactionOperation.Create(testTransaction, 1L)
        useCase.recordOperation(operation)
        
        // Undo first
        coEvery { transactionRepository.deleteTransaction(1L) } just Runs
        useCase.undo()
        
        // Setup for redo
        coEvery { transactionRepository.insertTransaction(testTransaction) } returns 3L
        
        // When
        val result = useCase.redo()
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { transactionRepository.insertTransaction(testTransaction) }
        assertTrue(useCase.canUndo.first())
        assertFalse(useCase.canRedo.first())
    }
    
    @Test
    fun `redo update operation should apply updated transaction`() = runTest {
        // Given
        val originalTransaction = testTransaction
        val updatedTransaction = testTransaction.copy(merchant = "Updated Restaurant")
        val operation = TransactionOperation.Update(originalTransaction, updatedTransaction)
        useCase.recordOperation(operation)
        
        // Undo first
        coEvery { transactionRepository.updateTransaction(originalTransaction) } just Runs
        useCase.undo()
        
        // Setup for redo
        coEvery { transactionRepository.updateTransaction(updatedTransaction) } just Runs
        
        // When
        val result = useCase.redo()
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { transactionRepository.updateTransaction(updatedTransaction) }
    }
    
    @Test
    fun `redo delete operation should delete transaction`() = runTest {
        // Given
        val operation = TransactionOperation.Delete(testTransaction)
        useCase.recordOperation(operation)
        
        // Undo first
        coEvery { transactionRepository.insertTransaction(testTransaction) } returns 2L
        useCase.undo()
        
        // Setup for redo
        coEvery { transactionRepository.deleteTransaction(testTransaction.id) } just Runs
        
        // When
        val result = useCase.redo()
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { transactionRepository.deleteTransaction(testTransaction.id) }
    }
    
    @Test
    fun `undo with empty stack should fail`() = runTest {
        // When
        val result = useCase.undo()
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Nothing to undo", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `redo with empty stack should fail`() = runTest {
        // When
        val result = useCase.redo()
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Nothing to redo", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `clearHistory should reset all stacks`() = runTest {
        // Given
        val operation = TransactionOperation.Create(testTransaction, 1L)
        useCase.recordOperation(operation)
        
        // When
        useCase.clearHistory()
        
        // Then
        assertFalse(useCase.canUndo.first())
        assertFalse(useCase.canRedo.first())
    }
    
    @Test
    fun `multiple operations should maintain correct order`() = runTest {
        // Given
        val operation1 = TransactionOperation.Create(testTransaction, 1L)
        val operation2 = TransactionOperation.Create(testTransaction.copy(id = 2L), 2L)
        
        useCase.recordOperation(operation1)
        useCase.recordOperation(operation2)
        
        coEvery { transactionRepository.deleteTransaction(2L) } just Runs
        coEvery { transactionRepository.deleteTransaction(1L) } just Runs
        
        // When - undo should process in reverse order
        useCase.undo() // Should undo operation2
        useCase.undo() // Should undo operation1
        
        // Then
        coVerify(exactly = 1) { transactionRepository.deleteTransaction(2L) }
        coVerify(exactly = 1) { transactionRepository.deleteTransaction(1L) }
        assertFalse(useCase.canUndo.first())
        assertTrue(useCase.canRedo.first())
    }
    
    @Test
    fun `new operation should clear redo stack`() = runTest {
        // Given
        val operation1 = TransactionOperation.Create(testTransaction, 1L)
        val operation2 = TransactionOperation.Create(testTransaction.copy(id = 2L), 2L)
        
        useCase.recordOperation(operation1)
        
        // Undo to populate redo stack
        coEvery { transactionRepository.deleteTransaction(1L) } just Runs
        useCase.undo()
        
        assertTrue(useCase.canRedo.first())
        
        // When - record new operation
        useCase.recordOperation(operation2)
        
        // Then - redo stack should be cleared
        assertFalse(useCase.canRedo.first())
        assertTrue(useCase.canUndo.first())
    }
}