package com.expensetracker.presentation.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.domain.model.TransactionType
import com.expensetracker.domain.usecase.ManageAccountsUseCase
import com.expensetracker.domain.usecase.ManageCategoriesUseCase
import com.expensetracker.domain.usecase.ManageTransactionsUseCase
import com.expensetracker.domain.usecase.TransactionOperation
import com.expensetracker.domain.usecase.TransactionUndoRedoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * ViewModel for transaction form (create/edit)
 */
@HiltViewModel
class TransactionFormViewModel @Inject constructor(
    private val manageTransactionsUseCase: ManageTransactionsUseCase,
    private val manageAccountsUseCase: ManageAccountsUseCase,
    private val manageCategoriesUseCase: ManageCategoriesUseCase,
    private val undoRedoUseCase: TransactionUndoRedoUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val transactionId: Long? = savedStateHandle.get<Long>("transactionId")
    
    private val _uiState = MutableStateFlow(TransactionFormUiState())
    val uiState: StateFlow<TransactionFormUiState> = _uiState.asStateFlow()
    
    init {
        loadFormData()
        transactionId?.let { loadTransaction(it) }
    }
    
    fun onEvent(event: TransactionFormEvent) {
        when (event) {
            is TransactionFormEvent.LoadTransaction -> loadTransaction(event.transactionId)
            is TransactionFormEvent.UpdateAmount -> updateAmount(event.amount)
            is TransactionFormEvent.UpdateType -> updateType(event.type)
            is TransactionFormEvent.UpdateCategory -> updateCategory(event.category)
            is TransactionFormEvent.UpdateMerchant -> updateMerchant(event.merchant)
            is TransactionFormEvent.UpdateDescription -> updateDescription(event.description)
            is TransactionFormEvent.UpdateDate -> updateDate(event.date)
            is TransactionFormEvent.UpdateAccount -> updateAccount(event.account)
            is TransactionFormEvent.SaveTransaction -> saveTransaction()
            is TransactionFormEvent.LoadFormData -> loadFormData()
        }
    }
    
    private fun loadFormData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Load accounts
                manageAccountsUseCase.observeAllAccounts().collect { accounts ->
                    _uiState.value = _uiState.value.copy(
                        accounts = accounts.filter { it.isActive },
                        isLoading = false
                    )
                }
                
                // Load categories
                manageCategoriesUseCase.observeAllCategories().collect { categories ->
                    _uiState.value = _uiState.value.copy(categories = categories)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load form data: ${e.message}"
                )
            }
        }
    }
    
    private fun loadTransaction(transactionId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val transaction = manageTransactionsUseCase.getTransaction(transactionId)
                if (transaction != null) {
                    _uiState.value = _uiState.value.copy(
                        transactionId = transaction.id,
                        amount = transaction.amount.toString(),
                        type = transaction.type,
                        selectedCategory = transaction.category,
                        merchant = transaction.merchant,
                        description = transaction.description ?: "",
                        date = transaction.date,
                        selectedAccount = _uiState.value.accounts.find { it.id == transaction.accountId },
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Transaction not found"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load transaction: ${e.message}"
                )
            }
        }
    }
    
    private fun updateAmount(amount: String) {
        _uiState.value = _uiState.value.copy(
            amount = amount,
            validationErrors = _uiState.value.validationErrors.copy(
                amount = validateAmount(amount)
            )
        )
    }
    
    private fun updateType(type: TransactionType) {
        _uiState.value = _uiState.value.copy(type = type)
    }
    
    private fun updateCategory(category: com.expensetracker.domain.model.Category) {
        _uiState.value = _uiState.value.copy(
            selectedCategory = category,
            validationErrors = _uiState.value.validationErrors.copy(category = null)
        )
    }
    
    private fun updateMerchant(merchant: String) {
        _uiState.value = _uiState.value.copy(
            merchant = merchant,
            validationErrors = _uiState.value.validationErrors.copy(
                merchant = if (merchant.isBlank()) "Merchant is required" else null
            )
        )
    }
    
    private fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }
    
    private fun updateDate(date: LocalDateTime) {
        _uiState.value = _uiState.value.copy(
            date = date,
            validationErrors = _uiState.value.validationErrors.copy(
                date = validateDate(date)
            )
        )
    }
    
    private fun updateAccount(account: com.expensetracker.domain.model.Account) {
        _uiState.value = _uiState.value.copy(
            selectedAccount = account,
            validationErrors = _uiState.value.validationErrors.copy(account = null)
        )
    }
    
    private fun saveTransaction() {
        val currentState = _uiState.value
        
        // Validate all fields
        val validationErrors = TransactionValidationErrors(
            amount = validateAmount(currentState.amount),
            category = if (currentState.selectedCategory == null) "Category is required" else null,
            merchant = if (currentState.merchant.isBlank()) "Merchant is required" else null,
            account = if (currentState.selectedAccount == null) "Account is required" else null,
            date = validateDate(currentState.date)
        )
        
        if (!validationErrors.isEmpty()) {
            _uiState.value = currentState.copy(validationErrors = validationErrors)
            return
        }
        
        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, error = null)
            
            try {
                val amount = BigDecimal(currentState.amount)
                val category = currentState.selectedCategory!!
                val account = currentState.selectedAccount!!
                
                val result = if (currentState.transactionId != null) {
                    // Update existing transaction
                    val originalTransaction = manageTransactionsUseCase.getTransaction(currentState.transactionId)
                    
                    val updateResult = manageTransactionsUseCase.updateTransaction(
                        transactionId = currentState.transactionId,
                        amount = amount,
                        type = currentState.type,
                        category = category,
                        merchant = currentState.merchant,
                        description = currentState.description.takeIf { it.isNotBlank() },
                        date = currentState.date,
                        accountId = account.id
                    )
                    
                    if (updateResult.isSuccess && originalTransaction != null) {
                        val updatedTransaction = manageTransactionsUseCase.getTransaction(currentState.transactionId)
                        if (updatedTransaction != null) {
                            undoRedoUseCase.recordOperation(
                                TransactionOperation.Update(originalTransaction, updatedTransaction)
                            )
                        }
                    }
                    
                    updateResult
                } else {
                    // Create new transaction
                    val createResult = manageTransactionsUseCase.createTransaction(
                        amount = amount,
                        type = currentState.type,
                        category = category,
                        merchant = currentState.merchant,
                        description = currentState.description.takeIf { it.isNotBlank() },
                        date = currentState.date,
                        accountId = account.id
                    )
                    
                    if (createResult.isSuccess) {
                        val transactionId = createResult.getOrThrow()
                        val transaction = manageTransactionsUseCase.getTransaction(transactionId)
                        if (transaction != null) {
                            undoRedoUseCase.recordOperation(
                                TransactionOperation.Create(transaction, transactionId)
                            )
                        }
                    }
                    
                    createResult.map { Unit }
                }
                
                if (result.isSuccess) {
                    _uiState.value = currentState.copy(isLoading = false)
                    // Navigation back should be handled by the UI
                } else {
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        error = "Failed to save transaction: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    isLoading = false,
                    error = "Failed to save transaction: ${e.message}"
                )
            }
        }
    }
    
    private fun validateAmount(amount: String): String? {
        return when {
            amount.isBlank() -> "Amount is required"
            else -> {
                try {
                    val decimal = BigDecimal(amount)
                    if (decimal <= BigDecimal.ZERO) "Amount must be positive" else null
                } catch (e: NumberFormatException) {
                    "Invalid amount format"
                }
            }
        }
    }
    
    private fun validateDate(date: LocalDateTime): String? {
        return if (date.isAfter(LocalDateTime.now())) {
            "Date cannot be in the future"
        } else null
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}