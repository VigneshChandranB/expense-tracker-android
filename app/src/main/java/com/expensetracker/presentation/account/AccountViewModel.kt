package com.expensetracker.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.domain.model.AccountType
import com.expensetracker.domain.usecase.ManageAccountsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

/**
 * ViewModel for account management functionality
 */
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val manageAccountsUseCase: ManageAccountsUseCase
) : ViewModel() {

    private val _listUiState = MutableStateFlow(AccountListUiState())
    val listUiState: StateFlow<AccountListUiState> = _listUiState.asStateFlow()

    private val _formUiState = MutableStateFlow(AccountFormUiState())
    val formUiState: StateFlow<AccountFormUiState> = _formUiState.asStateFlow()

    init {
        loadAccounts()
        observeAccounts()
    }

    fun onListEvent(event: AccountUiEvent) {
        when (event) {
            is AccountUiEvent.LoadAccounts -> loadAccounts()
            is AccountUiEvent.ToggleActiveFilter -> toggleActiveFilter()
            is AccountUiEvent.SelectAccount -> selectAccount(event.accountId)
            is AccountUiEvent.DeactivateAccount -> deactivateAccount(event.accountId)
            is AccountUiEvent.ReactivateAccount -> reactivateAccount(event.accountId)
            is AccountUiEvent.DeleteAccount -> deleteAccount(event.accountId)
            is AccountUiEvent.CreateNewAccount -> createNewAccount()
            is AccountUiEvent.EditAccount -> editAccount(event.accountId)
            is AccountUiEvent.ClearError -> clearListError()
        }
    }

    fun onFormEvent(event: AccountFormEvent) {
        when (event) {
            is AccountFormEvent.BankNameChanged -> updateBankName(event.bankName)
            is AccountFormEvent.AccountTypeChanged -> updateAccountType(event.accountType)
            is AccountFormEvent.AccountNumberChanged -> updateAccountNumber(event.accountNumber)
            is AccountFormEvent.NicknameChanged -> updateNickname(event.nickname)
            is AccountFormEvent.BalanceChanged -> updateBalance(event.balance)
            is AccountFormEvent.SaveAccount -> saveAccount()
            is AccountFormEvent.CancelEdit -> cancelEdit()
            is AccountFormEvent.ClearError -> clearFormError()
        }
    }

    private fun loadAccounts() {
        _listUiState.value = _listUiState.value.copy(isLoading = true, error = null)
        
        viewModelScope.launch {
            try {
                val accounts = if (_listUiState.value.showActiveOnly) {
                    manageAccountsUseCase.getActiveAccounts()
                } else {
                    manageAccountsUseCase.getAllAccounts()
                }
                
                _listUiState.value = _listUiState.value.copy(
                    accounts = accounts,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _listUiState.value = _listUiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load accounts"
                )
            }
        }
    }

    private fun observeAccounts() {
        manageAccountsUseCase.observeActiveAccounts()
            .catch { e ->
                _listUiState.value = _listUiState.value.copy(
                    error = e.message ?: "Failed to observe accounts"
                )
            }
            .onEach { accounts ->
                if (_listUiState.value.showActiveOnly) {
                    _listUiState.value = _listUiState.value.copy(accounts = accounts)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun toggleActiveFilter() {
        val newShowActiveOnly = !_listUiState.value.showActiveOnly
        _listUiState.value = _listUiState.value.copy(showActiveOnly = newShowActiveOnly)
        loadAccounts()
    }

    private fun selectAccount(accountId: Long) {
        // Navigate to account detail - implementation depends on navigation setup
    }

    private fun deactivateAccount(accountId: Long) {
        viewModelScope.launch {
            manageAccountsUseCase.deactivateAccount(accountId)
                .onSuccess {
                    loadAccounts()
                }
                .onFailure { e ->
                    _listUiState.value = _listUiState.value.copy(
                        error = e.message ?: "Failed to deactivate account"
                    )
                }
        }
    }

    private fun reactivateAccount(accountId: Long) {
        viewModelScope.launch {
            manageAccountsUseCase.reactivateAccount(accountId)
                .onSuccess {
                    loadAccounts()
                }
                .onFailure { e ->
                    _listUiState.value = _listUiState.value.copy(
                        error = e.message ?: "Failed to reactivate account"
                    )
                }
        }
    }

    private fun deleteAccount(accountId: Long) {
        viewModelScope.launch {
            manageAccountsUseCase.deleteAccount(accountId)
                .onSuccess {
                    loadAccounts()
                }
                .onFailure { e ->
                    _listUiState.value = _listUiState.value.copy(
                        error = e.message ?: "Failed to delete account"
                    )
                }
        }
    }

    private fun createNewAccount() {
        _formUiState.value = AccountFormUiState(isEditing = false)
    }

    private fun editAccount(accountId: Long) {
        viewModelScope.launch {
            val account = manageAccountsUseCase.getAccount(accountId)
            if (account != null) {
                _formUiState.value = AccountFormUiState(
                    isEditing = true,
                    accountId = accountId,
                    bankName = account.bankName,
                    accountType = account.accountType,
                    accountNumber = account.accountNumber,
                    nickname = account.nickname,
                    currentBalance = account.currentBalance.toString(),
                    isActive = account.isActive
                )
                validateForm()
            }
        }
    }

    private fun updateBankName(bankName: String) {
        _formUiState.value = _formUiState.value.copy(bankName = bankName)
        validateForm()
    }

    private fun updateAccountType(accountType: AccountType) {
        _formUiState.value = _formUiState.value.copy(accountType = accountType)
        validateForm()
    }

    private fun updateAccountNumber(accountNumber: String) {
        _formUiState.value = _formUiState.value.copy(accountNumber = accountNumber)
        validateForm()
    }

    private fun updateNickname(nickname: String) {
        _formUiState.value = _formUiState.value.copy(nickname = nickname)
        validateForm()
    }

    private fun updateBalance(balance: String) {
        _formUiState.value = _formUiState.value.copy(currentBalance = balance)
        validateForm()
    }

    private fun validateForm() {
        val state = _formUiState.value
        val errors = AccountValidationErrors(
            bankNameError = if (state.bankName.isBlank()) "Bank name is required" else null,
            accountNumberError = if (state.accountNumber.isBlank()) "Account number is required" else null,
            nicknameError = if (state.nickname.isBlank()) "Account nickname is required" else null,
            balanceError = try {
                BigDecimal(state.currentBalance)
                null
            } catch (e: NumberFormatException) {
                "Invalid balance amount"
            }
        )

        val isSaveEnabled = errors.bankNameError == null &&
                errors.accountNumberError == null &&
                errors.nicknameError == null &&
                errors.balanceError == null

        _formUiState.value = state.copy(
            validationErrors = errors,
            isSaveEnabled = isSaveEnabled
        )
    }

    private fun saveAccount() {
        if (!_formUiState.value.isSaveEnabled) return

        _formUiState.value = _formUiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            val state = _formUiState.value
            val balance = try {
                BigDecimal(state.currentBalance)
            } catch (e: NumberFormatException) {
                _formUiState.value = state.copy(
                    isLoading = false,
                    error = "Invalid balance amount"
                )
                return@launch
            }

            val result = if (state.isEditing && state.accountId != null) {
                manageAccountsUseCase.updateAccount(
                    accountId = state.accountId,
                    bankName = state.bankName,
                    accountType = state.accountType,
                    accountNumber = state.accountNumber,
                    nickname = state.nickname,
                    currentBalance = balance
                )
            } else {
                manageAccountsUseCase.createAccount(
                    bankName = state.bankName,
                    accountType = state.accountType,
                    accountNumber = state.accountNumber,
                    nickname = state.nickname,
                    initialBalance = balance
                ).map { Unit }
            }

            result
                .onSuccess {
                    _formUiState.value = AccountFormUiState() // Reset form
                    loadAccounts()
                }
                .onFailure { e ->
                    _formUiState.value = state.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to save account"
                    )
                }
        }
    }

    private fun cancelEdit() {
        _formUiState.value = AccountFormUiState()
    }

    private fun clearListError() {
        _listUiState.value = _listUiState.value.copy(error = null)
    }

    private fun clearFormError() {
        _formUiState.value = _formUiState.value.copy(error = null)
    }
}