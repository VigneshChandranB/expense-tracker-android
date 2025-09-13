package com.expensetracker.presentation.account

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.expensetracker.domain.model.Account
import com.expensetracker.domain.model.AccountType
import com.expensetracker.domain.usecase.ManageAccountsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Unit tests for AccountViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var manageAccountsUseCase: ManageAccountsUseCase
    private lateinit var viewModel: AccountViewModel

    private val sampleAccount = Account(
        id = 1L,
        bankName = "Test Bank",
        accountType = AccountType.CHECKING,
        accountNumber = "1234567890",
        nickname = "My Checking",
        currentBalance = BigDecimal("1000.00"),
        isActive = true,
        createdAt = LocalDateTime.now()
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        manageAccountsUseCase = mockk()
        
        // Setup default mock responses
        coEvery { manageAccountsUseCase.getActiveAccounts() } returns listOf(sampleAccount)
        coEvery { manageAccountsUseCase.getAllAccounts() } returns listOf(sampleAccount)
        coEvery { manageAccountsUseCase.observeActiveAccounts() } returns flowOf(listOf(sampleAccount))
        
        viewModel = AccountViewModel(manageAccountsUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should load active accounts`() = runTest {
        // Then
        val state = viewModel.listUiState.value
        assertEquals(listOf(sampleAccount), state.accounts)
        assertTrue(state.showActiveOnly)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `onListEvent LoadAccounts should load accounts`() = runTest {
        // Given
        val accounts = listOf(sampleAccount)
        coEvery { manageAccountsUseCase.getActiveAccounts() } returns accounts

        // When
        viewModel.onListEvent(AccountUiEvent.LoadAccounts)

        // Then
        val state = viewModel.listUiState.value
        assertEquals(accounts, state.accounts)
        coVerify { manageAccountsUseCase.getActiveAccounts() }
    }

    @Test
    fun `onListEvent ToggleActiveFilter should toggle filter and reload`() = runTest {
        // Given
        val allAccounts = listOf(sampleAccount, sampleAccount.copy(id = 2L, isActive = false))
        coEvery { manageAccountsUseCase.getAllAccounts() } returns allAccounts

        // When
        viewModel.onListEvent(AccountUiEvent.ToggleActiveFilter)

        // Then
        val state = viewModel.listUiState.value
        assertFalse(state.showActiveOnly)
        assertEquals(allAccounts, state.accounts)
        coVerify { manageAccountsUseCase.getAllAccounts() }
    }

    @Test
    fun `onListEvent DeactivateAccount should deactivate account successfully`() = runTest {
        // Given
        coEvery { manageAccountsUseCase.deactivateAccount(1L) } returns Result.success(Unit)

        // When
        viewModel.onListEvent(AccountUiEvent.DeactivateAccount(1L))

        // Then
        coVerify { manageAccountsUseCase.deactivateAccount(1L) }
        coVerify { manageAccountsUseCase.getActiveAccounts() } // Should reload
    }

    @Test
    fun `onListEvent DeactivateAccount should handle failure`() = runTest {
        // Given
        val errorMessage = "Failed to deactivate"
        coEvery { manageAccountsUseCase.deactivateAccount(1L) } returns Result.failure(Exception(errorMessage))

        // When
        viewModel.onListEvent(AccountUiEvent.DeactivateAccount(1L))

        // Then
        val state = viewModel.listUiState.value
        assertEquals(errorMessage, state.error)
    }

    @Test
    fun `onListEvent ReactivateAccount should reactivate account successfully`() = runTest {
        // Given
        coEvery { manageAccountsUseCase.reactivateAccount(1L) } returns Result.success(Unit)

        // When
        viewModel.onListEvent(AccountUiEvent.ReactivateAccount(1L))

        // Then
        coVerify { manageAccountsUseCase.reactivateAccount(1L) }
        coVerify { manageAccountsUseCase.getActiveAccounts() } // Should reload
    }

    @Test
    fun `onListEvent DeleteAccount should delete account successfully`() = runTest {
        // Given
        coEvery { manageAccountsUseCase.deleteAccount(1L) } returns Result.success(Unit)

        // When
        viewModel.onListEvent(AccountUiEvent.DeleteAccount(1L))

        // Then
        coVerify { manageAccountsUseCase.deleteAccount(1L) }
        coVerify { manageAccountsUseCase.getActiveAccounts() } // Should reload
    }

    @Test
    fun `onListEvent CreateNewAccount should reset form state`() = runTest {
        // When
        viewModel.onListEvent(AccountUiEvent.CreateNewAccount)

        // Then
        val formState = viewModel.formUiState.value
        assertFalse(formState.isEditing)
        assertEquals("", formState.bankName)
        assertEquals("", formState.accountNumber)
        assertEquals("", formState.nickname)
    }

    @Test
    fun `onListEvent EditAccount should populate form with account data`() = runTest {
        // Given
        coEvery { manageAccountsUseCase.getAccount(1L) } returns sampleAccount

        // When
        viewModel.onListEvent(AccountUiEvent.EditAccount(1L))

        // Then
        val formState = viewModel.formUiState.value
        assertTrue(formState.isEditing)
        assertEquals(1L, formState.accountId)
        assertEquals(sampleAccount.bankName, formState.bankName)
        assertEquals(sampleAccount.accountNumber, formState.accountNumber)
        assertEquals(sampleAccount.nickname, formState.nickname)
        assertEquals(sampleAccount.currentBalance.toString(), formState.currentBalance)
    }

    @Test
    fun `onFormEvent BankNameChanged should update bank name and validate`() = runTest {
        // When
        viewModel.onFormEvent(AccountFormEvent.BankNameChanged("New Bank"))

        // Then
        val formState = viewModel.formUiState.value
        assertEquals("New Bank", formState.bankName)
        assertNull(formState.validationErrors.bankNameError)
    }

    @Test
    fun `onFormEvent BankNameChanged with empty value should show validation error`() = runTest {
        // When
        viewModel.onFormEvent(AccountFormEvent.BankNameChanged(""))

        // Then
        val formState = viewModel.formUiState.value
        assertEquals("", formState.bankName)
        assertEquals("Bank name is required", formState.validationErrors.bankNameError)
        assertFalse(formState.isSaveEnabled)
    }

    @Test
    fun `onFormEvent AccountNumberChanged should update account number and validate`() = runTest {
        // When
        viewModel.onFormEvent(AccountFormEvent.AccountNumberChanged("9876543210"))

        // Then
        val formState = viewModel.formUiState.value
        assertEquals("9876543210", formState.accountNumber)
        assertNull(formState.validationErrors.accountNumberError)
    }

    @Test
    fun `onFormEvent NicknameChanged should update nickname and validate`() = runTest {
        // When
        viewModel.onFormEvent(AccountFormEvent.NicknameChanged("My New Account"))

        // Then
        val formState = viewModel.formUiState.value
        assertEquals("My New Account", formState.nickname)
        assertNull(formState.validationErrors.nicknameError)
    }

    @Test
    fun `onFormEvent BalanceChanged should update balance and validate`() = runTest {
        // When
        viewModel.onFormEvent(AccountFormEvent.BalanceChanged("2500.50"))

        // Then
        val formState = viewModel.formUiState.value
        assertEquals("2500.50", formState.currentBalance)
        assertNull(formState.validationErrors.balanceError)
    }

    @Test
    fun `onFormEvent BalanceChanged with invalid value should show validation error`() = runTest {
        // When
        viewModel.onFormEvent(AccountFormEvent.BalanceChanged("invalid"))

        // Then
        val formState = viewModel.formUiState.value
        assertEquals("invalid", formState.currentBalance)
        assertEquals("Invalid balance amount", formState.validationErrors.balanceError)
        assertFalse(formState.isSaveEnabled)
    }

    @Test
    fun `onFormEvent SaveAccount should create new account successfully`() = runTest {
        // Given
        setupValidFormState()
        coEvery { manageAccountsUseCase.createAccount(any(), any(), any(), any(), any()) } returns Result.success(1L)

        // When
        viewModel.onFormEvent(AccountFormEvent.SaveAccount)

        // Then
        coVerify { 
            manageAccountsUseCase.createAccount(
                bankName = "Test Bank",
                accountType = AccountType.CHECKING,
                accountNumber = "1234567890",
                nickname = "Test Account",
                initialBalance = BigDecimal("1000.00")
            )
        }
        
        // Form should be reset after successful save
        val formState = viewModel.formUiState.value
        assertEquals("", formState.bankName)
        assertEquals("", formState.accountNumber)
        assertEquals("", formState.nickname)
    }

    @Test
    fun `onFormEvent SaveAccount should update existing account successfully`() = runTest {
        // Given
        setupValidFormStateForEdit()
        coEvery { manageAccountsUseCase.updateAccount(any(), any(), any(), any(), any(), any()) } returns Result.success(Unit)

        // When
        viewModel.onFormEvent(AccountFormEvent.SaveAccount)

        // Then
        coVerify { 
            manageAccountsUseCase.updateAccount(
                accountId = 1L,
                bankName = "Updated Bank",
                accountType = AccountType.SAVINGS,
                accountNumber = "9876543210",
                nickname = "Updated Account",
                currentBalance = BigDecimal("2000.00")
            )
        }
    }

    @Test
    fun `onFormEvent SaveAccount should handle creation failure`() = runTest {
        // Given
        setupValidFormState()
        val errorMessage = "Account number already exists"
        coEvery { manageAccountsUseCase.createAccount(any(), any(), any(), any(), any()) } returns Result.failure(Exception(errorMessage))

        // When
        viewModel.onFormEvent(AccountFormEvent.SaveAccount)

        // Then
        val formState = viewModel.formUiState.value
        assertEquals(errorMessage, formState.error)
        assertFalse(formState.isLoading)
    }

    @Test
    fun `onFormEvent CancelEdit should reset form state`() = runTest {
        // Given
        setupValidFormState()

        // When
        viewModel.onFormEvent(AccountFormEvent.CancelEdit)

        // Then
        val formState = viewModel.formUiState.value
        assertEquals("", formState.bankName)
        assertEquals("", formState.accountNumber)
        assertEquals("", formState.nickname)
        assertFalse(formState.isEditing)
    }

    @Test
    fun `form validation should enable save when all fields are valid`() = runTest {
        // When
        setupValidFormState()

        // Then
        val formState = viewModel.formUiState.value
        assertTrue(formState.isSaveEnabled)
        assertNull(formState.validationErrors.bankNameError)
        assertNull(formState.validationErrors.accountNumberError)
        assertNull(formState.validationErrors.nicknameError)
        assertNull(formState.validationErrors.balanceError)
    }

    private fun setupValidFormState() {
        viewModel.onFormEvent(AccountFormEvent.BankNameChanged("Test Bank"))
        viewModel.onFormEvent(AccountFormEvent.AccountNumberChanged("1234567890"))
        viewModel.onFormEvent(AccountFormEvent.NicknameChanged("Test Account"))
        viewModel.onFormEvent(AccountFormEvent.BalanceChanged("1000.00"))
    }

    private fun setupValidFormStateForEdit() {
        viewModel.onListEvent(AccountUiEvent.CreateNewAccount)
        // Simulate editing mode
        val formState = viewModel.formUiState.value.copy(
            isEditing = true,
            accountId = 1L
        )
        // Use reflection or create a test-specific method to set the state
        viewModel.onFormEvent(AccountFormEvent.BankNameChanged("Updated Bank"))
        viewModel.onFormEvent(AccountFormEvent.AccountTypeChanged(AccountType.SAVINGS))
        viewModel.onFormEvent(AccountFormEvent.AccountNumberChanged("9876543210"))
        viewModel.onFormEvent(AccountFormEvent.NicknameChanged("Updated Account"))
        viewModel.onFormEvent(AccountFormEvent.BalanceChanged("2000.00"))
    }
}