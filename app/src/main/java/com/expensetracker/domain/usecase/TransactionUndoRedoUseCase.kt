package com.expensetracker.domain.usecase

import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for handling undo/redo operations for transactions
 * Maintains a stack of operations that can be undone and redone
 */
@Singleton
class TransactionUndoRedoUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    
    private val undoStack = mutableListOf<TransactionOperation>()
    private val redoStack = mutableListOf<TransactionOperation>()
    
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()
    
    /**
     * Record a transaction operation for undo/redo
     */
    fun recordOperation(operation: TransactionOperation) {
        undoStack.add(operation)
        redoStack.clear() // Clear redo stack when new operation is performed
        updateCanUndoRedo()
    }
    
    /**
     * Undo the last operation
     */
    suspend fun undo(): Result<Unit> {
        if (undoStack.isEmpty()) {
            return Result.failure(IllegalStateException("Nothing to undo"))
        }
        
        return try {
            val operation = undoStack.removeLastOrNull()
                ?: return Result.failure(IllegalStateException("Nothing to undo"))
            
            when (operation) {
                is TransactionOperation.Create -> {
                    // Undo create by deleting the transaction
                    transactionRepository.deleteTransaction(operation.transactionId)
                    redoStack.add(TransactionOperation.Delete(operation.transaction))
                }
                is TransactionOperation.Update -> {
                    // Undo update by restoring original transaction
                    transactionRepository.updateTransaction(operation.originalTransaction)
                    redoStack.add(TransactionOperation.Update(operation.updatedTransaction, operation.originalTransaction))
                }
                is TransactionOperation.Delete -> {
                    // Undo delete by recreating the transaction
                    val newId = transactionRepository.insertTransaction(operation.transaction)
                    redoStack.add(TransactionOperation.Create(operation.transaction.copy(id = newId), newId))
                }
                is TransactionOperation.Transfer -> {
                    // Undo transfer by deleting both transactions
                    transactionRepository.deleteTransaction(operation.fromTransactionId)
                    transactionRepository.deleteTransaction(operation.toTransactionId)
                    redoStack.add(TransactionOperation.TransferDelete(operation.fromTransaction, operation.toTransaction))
                }
                is TransactionOperation.TransferDelete -> {
                    // Undo transfer delete by recreating both transactions
                    val fromId = transactionRepository.insertTransaction(operation.fromTransaction)
                    val toId = transactionRepository.insertTransaction(operation.toTransaction)
                    transactionRepository.linkTransferTransactions(fromId, toId)
                    redoStack.add(TransactionOperation.Transfer(
                        operation.fromTransaction.copy(id = fromId),
                        operation.toTransaction.copy(id = toId),
                        fromId,
                        toId
                    ))
                }
            }
            
            updateCanUndoRedo()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Redo the last undone operation
     */
    suspend fun redo(): Result<Unit> {
        if (redoStack.isEmpty()) {
            return Result.failure(IllegalStateException("Nothing to redo"))
        }
        
        return try {
            val operation = redoStack.removeLastOrNull()
                ?: return Result.failure(IllegalStateException("Nothing to redo"))
            
            when (operation) {
                is TransactionOperation.Create -> {
                    val newId = transactionRepository.insertTransaction(operation.transaction)
                    undoStack.add(TransactionOperation.Create(operation.transaction.copy(id = newId), newId))
                }
                is TransactionOperation.Update -> {
                    transactionRepository.updateTransaction(operation.updatedTransaction)
                    undoStack.add(TransactionOperation.Update(operation.originalTransaction, operation.updatedTransaction))
                }
                is TransactionOperation.Delete -> {
                    transactionRepository.deleteTransaction(operation.transaction.id)
                    undoStack.add(TransactionOperation.Delete(operation.transaction))
                }
                is TransactionOperation.Transfer -> {
                    val fromId = transactionRepository.insertTransaction(operation.fromTransaction)
                    val toId = transactionRepository.insertTransaction(operation.toTransaction)
                    transactionRepository.linkTransferTransactions(fromId, toId)
                    undoStack.add(TransactionOperation.Transfer(
                        operation.fromTransaction.copy(id = fromId),
                        operation.toTransaction.copy(id = toId),
                        fromId,
                        toId
                    ))
                }
                is TransactionOperation.TransferDelete -> {
                    transactionRepository.deleteTransaction(operation.fromTransaction.id)
                    transactionRepository.deleteTransaction(operation.toTransaction.id)
                    undoStack.add(TransactionOperation.TransferDelete(operation.fromTransaction, operation.toTransaction))
                }
            }
            
            updateCanUndoRedo()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Clear all undo/redo history
     */
    fun clearHistory() {
        undoStack.clear()
        redoStack.clear()
        updateCanUndoRedo()
    }
    
    private fun updateCanUndoRedo() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }
}

/**
 * Sealed class representing different transaction operations that can be undone/redone
 */
sealed class TransactionOperation {
    data class Create(val transaction: Transaction, val transactionId: Long) : TransactionOperation()
    data class Update(val originalTransaction: Transaction, val updatedTransaction: Transaction) : TransactionOperation()
    data class Delete(val transaction: Transaction) : TransactionOperation()
    data class Transfer(
        val fromTransaction: Transaction,
        val toTransaction: Transaction,
        val fromTransactionId: Long,
        val toTransactionId: Long
    ) : TransactionOperation()
    data class TransferDelete(val fromTransaction: Transaction, val toTransaction: Transaction) : TransactionOperation()
}