package com.expensetracker.presentation.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.domain.usecase.ManageAccountsUseCase
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
 * ViewModel for transfer form
 */
@HiltViewModel
class TransferFormViewModel @Inject constructor(
    private val manageTransactionsUseCase: ManageTransactionsUseCase,
    private val manageAccountsUseCase: ManageAccountsUseCase,
    private val undoRedoUseCase: TransactionUndoRedoUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TransferFormUiState())
    val uiState: StateFlow<TransferFormUiState> = _uiState.asStateFlow()
    
    init {
        loadAccounts()
    }
    
    fun onEvent(event: TransferFormEvent) {
        when (event) {
            is TransferFormEvent.UpdateAmount -> updateAmount(event.amount)
            is TransferFormEvent.UpdateFromAccount -> updateFromAccount(event.account)
            is TransferFormEvent.UpdateToAccount -> updateToAccount(event.account)
            is TransferFormEvent.UpdateDescription -> updateDescription(event.description)
            is TransferFormEvent.UpdateDate -> updateDate(event.date)
            is TransferFormEvent.SaveTransfer -> saveTransfer()
            is TransferFormEvent.LoadAccounts -> loadAccounts()
        }
    }
    
    private fun loadAccounts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                manageAccountsUseCase.observeAllAccounts().collect { accounts ->
                    _uiState.value = _uiState.value.copy(
                        accounts = accounts.filter { it.isActive },
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load accounts: ${e.message}"
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
    
    private fun updateFromAccount(account: com.expensetracker.domain.model.Account) {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            fromAccount = account,
            validationErrors = currentState.validationErrors.copy(
                fromAccount = null,
                sameAccount = validateDifferentAccounts(account, currentState.toAccount)
            )
        )
    }
    
    private fun updateToAccount(account: com.expensetracker.domain.model.Account) {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            toAccount = account,
            validationErrors = currentState.validationErrors.copy(
                toAccount = null,
                sameAccount = validateDifferentAccounts(currentState.fromAccount, account)
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
    
    private fun saveTransfer() {
        val currentState = _uiState.value
        
        // Validate all fields
        val validationErrors = TransferValidationErrors(
            amount = validateAmount(currentState.amount),
            fromAccount = if (currentState.fromAccount == null) "Source account is required" else null,
            toAccount = if (currentState.toAccount == null) "Destination account is required" else null,
            date = validateDate(currentState.date),
            sameAccount = validateDifferentAccounts(currentState.fromAccount, currentState.toAccount)
        )
        
        if (!validationErrors.isEmpty()) {
            _uiState.value = currentState.copy(validationErrors = validationErrors)
            return
        }
        
        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, error = null)
            
            try {
                val amount = BigDecimal(currentState.amount)
                val fromAccount = currentState.fromAccount!!
                val toAccount = currentState.toAccount!!
                
                val result = manageTransactionsUseCase.createTransfer(
                    fromAccountId = fromAccount.id,
                    toAccountId = toAccount.id,
                    amount = amount,
                    description = currentState.description.takeIf { it.isNotBlank() },
                    date = currentState.date
                )
                
                if (result.isSuccess) {
                    val (fromTransactionId, toTransactionId) = result.getOrThrow()
                    
                    // Get the created transactions for undo functionality
                    val fromTransaction = manageTransactionsUseCase.getTransaction(fromTransactionId)
                    val toTransaction = manageTransactionsUseCase.getTransaction(toTransactionId)
                    
                    if (fromTransaction != null && toTransaction != null) {
                        undoRedoUseCase.recordOperation(
                            TransactionOperation.Transfer(
                                fromTransaction = fromTransaction,
                                toTransaction = toTransaction,
                                fromTransactionId = fromTransactionId,
                                toTransactionId = toTransactionId
                            )
                        )
                    }
                    
                    // Reset form
                    _uiState.value = TransferFormUiState(
                        accounts = currentState.accounts,
                        isLoading = false
                    )
                } else {
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        error = "Failed to create transfer: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    isLoading = false,
                    error = "Failed to create transfer: ${e.message}"
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
    
    private fun validateDifferentAccounts(
        fromAccount: com.expensetracker.domain.model.Account?,
        toAccount: com.expensetracker.domain.model.Account?
    ): String? {
        return if (fromAccount != null && toAccount != null && fromAccount.id == toAccount.id) {
            "Source and destination accounts must be different"
        } else null
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}