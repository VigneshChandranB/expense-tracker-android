package com.expensetracker.presentation.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.domain.model.TransactionType
import com.expensetracker.domain.usecase.GetTransactionsUseCase
import com.expensetracker.domain.usecase.ManageAccountsUseCase
import com.expensetracker.domain.usecase.ManageCategoriesUseCase
import com.expensetracker.domain.usecase.ManageTransactionsUseCase
import com.expensetracker.domain.usecase.TransactionUndoRedoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * ViewModel for transaction management screen
 * Handles transaction listing, filtering, and operations
 */
@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val manageTransactionsUseCase: ManageTransactionsUseCase,
    private val manageAccountsUseCase: ManageAccountsUseCase,
    private val manageCategoriesUseCase: ManageCategoriesUseCase,
    private val undoRedoUseCase: TransactionUndoRedoUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TransactionUiState())
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()
    
    private var selectedAccountId: Long? = null
    
    init {
        loadInitialData()
        observeUndoRedoState()
    }
    
    fun onEvent(event: TransactionEvent) {
        when (event) {
            is TransactionEvent.LoadTransactions -> loadTransactions()
            is TransactionEvent.LoadAccounts -> loadAccounts()
            is TransactionEvent.LoadCategories -> loadCategories()
            is TransactionEvent.DeleteTransaction -> deleteTransaction(event.transactionId)
            is TransactionEvent.Undo -> undo()
            is TransactionEvent.Redo -> redo()
            is TransactionEvent.FilterByAccount -> filterByAccount(event.accountId)
            is TransactionEvent.UpdateSearchQuery -> updateSearchQuery(event.query)
            is TransactionEvent.ToggleAccountFilter -> toggleAccountFilter(event.accountId)
            is TransactionEvent.ToggleCategoryFilter -> toggleCategoryFilter(event.categoryId)
            is TransactionEvent.ToggleTypeFilter -> toggleTypeFilter(event.type)
            is TransactionEvent.UpdateDateRange -> updateDateRange(event.startDate, event.endDate)
            is TransactionEvent.UpdateSortOrder -> updateSortOrder(event.sortOrder)
            is TransactionEvent.ToggleFilters -> toggleFilters()
            is TransactionEvent.ClearAllFilters -> clearAllFilters()
            is TransactionEvent.ApplyFilters -> applyFilters()
        }
    }
    
    private fun loadInitialData() {
        loadTransactions()
        loadAccounts()
        loadCategories()
    }
    
    private fun loadTransactions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val transactionsFlow = if (selectedAccountId != null) {
                    getTransactionsUseCase.getTransactionsByAccount(selectedAccountId!!)
                } else {
                    getTransactionsUseCase()
                }
                
                transactionsFlow.collect { transactions ->
                    _uiState.value = _uiState.value.copy(
                        transactions = transactions,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load transactions: ${e.message}"
                )
            }
        }
    }
    
    private fun loadAccounts() {
        viewModelScope.launch {
            try {
                manageAccountsUseCase.observeAccounts().collect { accounts ->
                    _uiState.value = _uiState.value.copy(accounts = accounts)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load accounts: ${e.message}"
                )
            }
        }
    }
    
    private fun loadCategories() {
        viewModelScope.launch {
            try {
                manageCategoriesUseCase.observeCategories().collect { categories ->
                    _uiState.value = _uiState.value.copy(categories = categories)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load categories: ${e.message}"
                )
            }
        }
    }
    
    private fun deleteTransaction(transactionId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Get the transaction before deleting for undo functionality
                val transaction = manageTransactionsUseCase.getTransaction(transactionId)
                if (transaction != null) {
                    val result = manageTransactionsUseCase.deleteTransaction(transactionId)
                    
                    if (result.isSuccess) {
                        // Record operation for undo
                        undoRedoUseCase.recordOperation(
                            com.expensetracker.domain.usecase.TransactionOperation.Delete(transaction)
                        )
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to delete transaction: ${result.exceptionOrNull()?.message}"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Transaction not found"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to delete transaction: ${e.message}"
                )
            }
        }
    }
    
    private fun undo() {
        viewModelScope.launch {
            val result = undoRedoUseCase.undo()
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    error = "Undo failed: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }
    
    private fun redo() {
        viewModelScope.launch {
            val result = undoRedoUseCase.redo()
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    error = "Redo failed: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }
    
    private fun filterByAccount(accountId: Long?) {
        selectedAccountId = accountId
        loadTransactions()
    }
    
    private fun observeUndoRedoState() {
        viewModelScope.launch {
            combine(
                undoRedoUseCase.canUndo,
                undoRedoUseCase.canRedo
            ) { canUndo, canRedo ->
                _uiState.value = _uiState.value.copy(
                    canUndo = canUndo,
                    canRedo = canRedo
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    private fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }
    
    private fun toggleAccountFilter(accountId: Long) {
        val currentFilters = _uiState.value.selectedAccountIds.toMutableList()
        if (currentFilters.contains(accountId)) {
            currentFilters.remove(accountId)
        } else {
            currentFilters.add(accountId)
        }
        _uiState.value = _uiState.value.copy(selectedAccountIds = currentFilters)
        applyFilters()
    }
    
    private fun toggleCategoryFilter(categoryId: Long) {
        val currentFilters = _uiState.value.selectedCategoryIds.toMutableList()
        if (currentFilters.contains(categoryId)) {
            currentFilters.remove(categoryId)
        } else {
            currentFilters.add(categoryId)
        }
        _uiState.value = _uiState.value.copy(selectedCategoryIds = currentFilters)
        applyFilters()
    }
    
    private fun toggleTypeFilter(type: TransactionType) {
        val currentFilters = _uiState.value.selectedTypes.toMutableList()
        if (currentFilters.contains(type)) {
            currentFilters.remove(type)
        } else {
            currentFilters.add(type)
        }
        _uiState.value = _uiState.value.copy(selectedTypes = currentFilters)
        applyFilters()
    }
    
    private fun updateDateRange(startDate: LocalDateTime?, endDate: LocalDateTime?) {
        _uiState.value = _uiState.value.copy(startDate = startDate, endDate = endDate)
        applyFilters()
    }
    
    private fun updateSortOrder(sortOrder: SortOrder) {
        _uiState.value = _uiState.value.copy(sortOrder = sortOrder)
        applyFilters()
    }
    
    private fun toggleFilters() {
        _uiState.value = _uiState.value.copy(showFilters = !_uiState.value.showFilters)
    }
    
    private fun clearAllFilters() {
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            selectedAccountIds = emptyList(),
            selectedCategoryIds = emptyList(),
            selectedTypes = emptyList(),
            startDate = null,
            endDate = null,
            sortOrder = SortOrder.DATE_DESC
        )
        applyFilters()
    }
    
    private fun applyFilters() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val state = _uiState.value
                val transactions = getTransactionsUseCase.searchTransactions(
                    query = state.searchQuery.takeIf { it.isNotBlank() },
                    accountIds = state.selectedAccountIds.takeIf { it.isNotEmpty() },
                    categoryIds = state.selectedCategoryIds.takeIf { it.isNotEmpty() },
                    types = state.selectedTypes.takeIf { it.isNotEmpty() },
                    startDate = state.startDate?.toLocalDate(),
                    endDate = state.endDate?.toLocalDate()
                )
                
                val sortedTransactions = sortTransactions(transactions, state.sortOrder)
                
                _uiState.value = _uiState.value.copy(
                    transactions = sortedTransactions,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to filter transactions: ${e.message}"
                )
            }
        }
    }
    
    private fun sortTransactions(transactions: List<com.expensetracker.domain.model.Transaction>, sortOrder: SortOrder): List<com.expensetracker.domain.model.Transaction> {
        return when (sortOrder) {
            SortOrder.DATE_ASC -> transactions.sortedBy { it.date }
            SortOrder.DATE_DESC -> transactions.sortedByDescending { it.date }
            SortOrder.AMOUNT_ASC -> transactions.sortedBy { it.amount }
            SortOrder.AMOUNT_DESC -> transactions.sortedByDescending { it.amount }
            SortOrder.MERCHANT_ASC -> transactions.sortedBy { it.merchant }
            SortOrder.MERCHANT_DESC -> transactions.sortedByDescending { it.merchant }
        }
    }
}