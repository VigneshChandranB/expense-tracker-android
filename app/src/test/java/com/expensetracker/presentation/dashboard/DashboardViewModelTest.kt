package com.expensetracker.presentation.dashboard

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.expensetracker.domain.model.*
import com.expensetracker.domain.repository.AccountRepository
import com.expensetracker.domain.repository.TransactionRepository
import com.expensetracker.domain.usecase.AnalyzeCategoryBreakdownUseCase
import com.expensetracker.domain.usecase.AnalyzeSpendingTrendsUseCase
import com.expensetracker.domain.usecase.GenerateMonthlyReportUseCase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var accountRepository: AccountRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var generateMonthlyReportUseCase: GenerateMonthlyReportUseCase
    private lateinit var analyzeCategoryBreakdownUseCase: AnalyzeCategoryBreakdownUseCase
    private lateinit var analyzeSpendingTrendsUseCase: AnalyzeSpendingTrendsUseCase
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        accountRepository = mockk()
        transactionRepository = mockk()
        generateMonthlyReportUseCase = mockk()
        analyzeCategoryBreakdownUseCase = mockk()
        analyzeSpendingTrendsUseCase = mockk()

        // Setup default mock responses
        coEvery { accountRepository.getAllAccounts() } returns createSampleAccounts()
        coEvery { transactionRepository.getRecentTransactions(any()) } returns createSampleTransactions()
        coEvery { transactionRepository.getTransactionsByAccount(any(), any()) } returns createSampleTransactions()
        coEvery { generateMonthlyReportUseCase(any(), any()) } returns createSampleMonthlyReport()
        coEvery { analyzeCategoryBreakdownUseCase(any(), any()) } returns createSampleCategoryBreakdown()
        coEvery { analyzeSpendingTrendsUseCase(any()) } returns createSampleSpendingTrends()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be loading`() = runTest {
        createViewModel()
        
        val initialState = viewModel.uiState.first()
        assert(initialState.isLoading)
        assert(initialState.accounts.isEmpty())
        assert(initialState.selectedAccount == null)
    }

    @Test
    fun `loadDashboardData should populate all data successfully`() = runTest {
        createViewModel()
        
        // Wait for loading to complete
        advanceUntilIdle()
        
        val state = viewModel.uiState.value
        
        assert(!state.isLoading)
        assert(state.error == null)
        assert(state.accounts.isNotEmpty())
        assert(state.totalBalance > BigDecimal.ZERO)
        assert(state.monthlyReport != null)
        assert(state.categoryBreakdown != null)
        assert(state.spendingTrends != null)
        assert(state.recentTransactions.isNotEmpty())
    }

    @Test
    fun `selectAccount should update selected account and reload data`() = runTest {
        createViewModel()
        advanceUntilIdle()
        
        val account = createSampleAccounts().first()
        viewModel.selectAccount(account)
        advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assert(state.selectedAccount == account)
        assert(state.totalBalance == account.currentBalance)
        
        // Verify that account-specific data was loaded
        coVerify { transactionRepository.getTransactionsByAccount(account.id, 5) }
        coVerify { generateMonthlyReportUseCase(any(), account.id) }
        coVerify { analyzeCategoryBreakdownUseCase(any(), account.id) }
        coVerify { analyzeSpendingTrendsUseCase(account.id) }
    }

    @Test
    fun `selectAccount with null should show all accounts view`() = runTest {
        createViewModel()
        advanceUntilIdle()
        
        // First select an account
        val account = createSampleAccounts().first()
        viewModel.selectAccount(account)
        advanceUntilIdle()
        
        // Then select null (all accounts)
        viewModel.selectAccount(null)
        advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assert(state.selectedAccount == null)
        
        // Total balance should be sum of all accounts
        val expectedTotal = createSampleAccounts().sumOf { it.currentBalance }
        assert(state.totalBalance == expectedTotal)
        
        // Verify that all-accounts data was loaded
        coVerify { transactionRepository.getRecentTransactions(5) }
        coVerify { generateMonthlyReportUseCase(any(), null) }
        coVerify { analyzeCategoryBreakdownUseCase(any(), null) }
        coVerify { analyzeSpendingTrendsUseCase(null) }
    }

    @Test
    fun `refreshData should reload all dashboard data`() = runTest {
        createViewModel()
        advanceUntilIdle()
        
        // Clear previous invocations
        clearMocks(accountRepository, transactionRepository, generateMonthlyReportUseCase)
        
        viewModel.refreshData()
        advanceUntilIdle()
        
        // Verify all data sources were called again
        coVerify { accountRepository.getAllAccounts() }
        coVerify { transactionRepository.getRecentTransactions(any()) }
        coVerify { generateMonthlyReportUseCase(any(), any()) }
        coVerify { analyzeCategoryBreakdownUseCase(any(), any()) }
        coVerify { analyzeSpendingTrendsUseCase(any()) }
    }

    @Test
    fun `error in data loading should set error state`() = runTest {
        val errorMessage = "Network error"
        coEvery { accountRepository.getAllAccounts() } throws Exception(errorMessage)
        
        createViewModel()
        advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assert(!state.isLoading)
        assert(state.error != null)
        assert(state.error!!.contains(errorMessage))
    }

    @Test
    fun `generateAlerts should create low balance alerts`() = runTest {
        val lowBalanceAccount = Account(
            id = 1L,
            bankName = "Test Bank",
            accountType = AccountType.SAVINGS,
            accountNumber = "123456",
            nickname = "Low Balance Account",
            currentBalance = BigDecimal("500.00"), // Below 1000 threshold
            isActive = true,
            createdAt = LocalDateTime.now()
        )
        
        coEvery { accountRepository.getAllAccounts() } returns listOf(lowBalanceAccount)
        
        createViewModel()
        advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assert(state.alerts.isNotEmpty())
        
        val lowBalanceAlert = state.alerts.find { it.type == AlertType.LOW_BALANCE }
        assert(lowBalanceAlert != null)
        assert(lowBalanceAlert!!.accountId == lowBalanceAccount.id)
    }

    @Test
    fun `generateAlerts should create spending increase alerts`() = runTest {
        val monthlyReport = createSampleMonthlyReport().copy(
            comparisonToPreviousMonth = MonthComparison(
                incomeChange = BigDecimal.ZERO,
                expenseChange = BigDecimal("10000.00"),
                incomeChangePercentage = 0f,
                expenseChangePercentage = 25f, // Above 20% threshold
                significantChanges = emptyList()
            )
        )
        
        coEvery { generateMonthlyReportUseCase(any(), any()) } returns monthlyReport
        
        createViewModel()
        advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assert(state.alerts.isNotEmpty())
        
        val spendingAlert = state.alerts.find { it.type == AlertType.UNUSUAL_SPENDING }
        assert(spendingAlert != null)
        assert(spendingAlert!!.message.contains("25%"))
    }

    private fun createViewModel() {
        viewModel = DashboardViewModel(
            accountRepository = accountRepository,
            transactionRepository = transactionRepository,
            generateMonthlyReportUseCase = generateMonthlyReportUseCase,
            analyzeCategoryBreakdownUseCase = analyzeCategoryBreakdownUseCase,
            analyzeSpendingTrendsUseCase = analyzeSpendingTrendsUseCase
        )
    }

    private fun createSampleAccounts(): List<Account> {
        return listOf(
            Account(
                id = 1L,
                bankName = "HDFC Bank",
                accountType = AccountType.SAVINGS,
                accountNumber = "1234567890",
                nickname = "My Savings",
                currentBalance = BigDecimal("25000.00"),
                isActive = true,
                createdAt = LocalDateTime.now()
            ),
            Account(
                id = 2L,
                bankName = "ICICI Bank",
                accountType = AccountType.CHECKING,
                accountNumber = "0987654321",
                nickname = "My Checking",
                currentBalance = BigDecimal("15000.00"),
                isActive = true,
                createdAt = LocalDateTime.now()
            )
        )
    }

    private fun createSampleTransactions(): List<Transaction> {
        return listOf(
            Transaction(
                id = 1L,
                amount = BigDecimal("2500.00"),
                type = TransactionType.EXPENSE,
                category = Category(1L, "Shopping", "shopping", "#2196F3", true),
                merchant = "Amazon",
                description = null,
                date = LocalDateTime.now(),
                source = TransactionSource.MANUAL,
                accountId = 1L
            )
        )
    }

    private fun createSampleMonthlyReport(): MonthlyReport {
        return MonthlyReport(
            month = YearMonth.now(),
            totalIncome = BigDecimal("80000.00"),
            totalExpenses = BigDecimal("45000.00"),
            netAmount = BigDecimal("35000.00"),
            categoryBreakdown = emptyMap(),
            topMerchants = emptyList(),
            comparisonToPreviousMonth = MonthComparison(
                incomeChange = BigDecimal("5000.00"),
                expenseChange = BigDecimal("2000.00"),
                incomeChangePercentage = 6.67f,
                expenseChangePercentage = 4.65f,
                significantChanges = emptyList()
            ),
            accountBreakdown = emptyMap()
        )
    }

    private fun createSampleCategoryBreakdown(): CategoryBreakdown {
        return CategoryBreakdown(
            period = DateRange.currentMonth(),
            totalAmount = BigDecimal("37500.00"),
            categorySpending = emptyList(),
            topCategories = emptyList(),
            unusedCategories = emptyList()
        )
    }

    private fun createSampleSpendingTrends(): SpendingTrends {
        return SpendingTrends(
            monthlyTrends = emptyList(),
            categoryTrends = emptyMap(),
            overallTrend = OverallTrend(
                direction = TrendDirection.INCREASING,
                averageMonthlyChange = 5.2f,
                consistency = 0.8f,
                volatility = 0.3f,
                savingsRate = 0.25f,
                savingsRateTrend = TrendDirection.STABLE
            ),
            seasonalPatterns = emptyList(),
            predictions = null
        )
    }
}