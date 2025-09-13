package com.expensetracker.presentation.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.domain.model.Account
import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.usecase.ManageAccountsUseCase
import com.expensetracker.domain.usecase.ManageTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for transaction detail screen
 */
@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val manageTransactionsUseCase: ManageTransactionsUseCase,
    private val manageAccountsUseCase: ManageAccountsUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TransactionDetailUiState())
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()
    
    private var currentTransactionId: Long? = null
    
    fun loadTransaction(transactionId: Long) {
        currentTransactionId = transactionId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val transaction = manageTransactionsUseCase.getTransaction(transactionId)
                if (transaction != null) {
                    val account = manageAccountsUseCase.getAccount(transaction.accountId)
                    val transferAccount = transaction.transferAccountId?.let { 
                        manageAccountsUseCase.getAccount(it) 
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        transaction = transaction,
                        account = account,
                        transferAccount = transferAccount,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        transaction = null,
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
    
    fun deleteTransaction() {
        currentTransactionId?.let { transactionId ->
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                try {
                    val result = manageTransactionsUseCase.deleteTransaction(transactionId)
                    if (result.isSuccess) {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to delete transaction: ${result.exceptionOrNull()?.message}"
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
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * UI state for transaction detail screen
 */
data class TransactionDetailUiState(
    val transaction: Transaction? = null,
    val account: Account? = null,
    val transferAccount: Account? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)