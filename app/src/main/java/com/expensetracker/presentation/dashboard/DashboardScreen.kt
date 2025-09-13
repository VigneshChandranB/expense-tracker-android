package com.expensetracker.presentation.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.domain.model.Account
import com.expensetracker.domain.model.Transaction
import com.expensetracker.presentation.accessibility.accessibleSemantics
import com.expensetracker.presentation.components.AccessibleErrorDisplay
import com.expensetracker.presentation.components.AccessibleLoadingIndicator
import com.expensetracker.presentation.dashboard.components.*
import com.expensetracker.presentation.navigation.AccessibleTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToTransactions: () -> Unit,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onTransactionClick: (Transaction) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .accessibleSemantics(
                contentDescription = "Dashboard screen showing account overview and financial summary"
            )
    ) {
        // Top App Bar
        AccessibleTopAppBar(
            title = "Dashboard",
            actions = {
                IconButton(
                    onClick = { viewModel.refreshData() },
                    modifier = Modifier.semantics {
                        contentDescription = "Refresh dashboard data"
                        role = Role.Button
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null
                    )
                }
            }
        )
        
        if (uiState.isLoading) {
            AccessibleLoadingIndicator(
                message = "Loading dashboard data",
                modifier = Modifier.fillMaxSize()
            )
        } else if (uiState.error != null) {
            AccessibleErrorDisplay(
                error = uiState.error!!,
                onRetry = { viewModel.refreshData() },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics {
                        contentDescription = "Dashboard content list"
                    },
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                // Account Switcher
                item {
                    AccountSwitcher(
                        accounts = uiState.accounts,
                        selectedAccount = uiState.selectedAccount,
                        onAccountSelected = { account ->
                            viewModel.selectAccount(account)
                        },
                        modifier = Modifier.semantics {
                            contentDescription = "Account selector"
                            heading()
                        }
                    )
                }
                
                // Account Balance Cards or Total Portfolio
                item {
                    if (uiState.selectedAccount == null) {
                        // Show total portfolio view
                        TotalPortfolioCard(
                            totalBalance = uiState.totalBalance,
                            accountCount = uiState.accounts.size
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Show account balance cards in a horizontal scroll
                        if (uiState.accounts.isNotEmpty()) {
                            Text(
                                text = "Account Balances",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                items(uiState.accounts.filter { it.isActive }) { account ->
                                    AccountBalanceCard(
                                        account = account,
                                        isSelected = false,
                                        onClick = { viewModel.selectAccount(account) },
                                        modifier = Modifier.width(280.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Show selected account details
                        AccountBalanceCard(
                            account = uiState.selectedAccount!!,
                            isSelected = true,
                            onClick = { /* Already selected */ }
                        )
                    }
                }
                
                // Financial Alerts
                if (uiState.alerts.isNotEmpty()) {
                    item {
                        FinancialAlertsCard(
                            alerts = uiState.alerts,
                            onAlertDismiss = { /* Handle alert dismissal */ }
                        )
                    }
                }
                
                // Spending Summary
                item {
                    SpendingSummaryCard(
                        monthlyReport = uiState.monthlyReport
                    )
                }
                
                // Category Breakdown Chart
                item {
                    CategoryBreakdownChart(
                        categoryBreakdown = uiState.categoryBreakdown
                    )
                }
                
                // Recent Transactions
                item {
                    RecentTransactionsList(
                        transactions = uiState.recentTransactions,
                        onTransactionClick = onTransactionClick,
                        onViewAllClick = onNavigateToTransactions
                    )
                }
                
                // Quick Actions
                item {
                    QuickActionsCard(
                        onAddTransaction = onNavigateToAddTransaction,
                        onManageAccounts = onNavigateToAccounts,
                        onViewTransactions = onNavigateToTransactions
                    )
                }
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickActionsCard(
    onAddTransaction: () -> Unit,
    onManageAccounts: () -> Unit,
    onViewTransactions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Quick actions section"
                heading()
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics {
                    heading()
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onAddTransaction,
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            contentDescription = "Add new transaction"
                            role = Role.Button
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Transaction")
                }
                
                OutlinedButton(
                    onClick = onManageAccounts,
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            contentDescription = "Manage bank accounts"
                            role = Role.Button
                        }
                ) {
                    Text("Manage Accounts")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedButton(
                onClick = onViewTransactions,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "View all transactions"
                        role = Role.Button
                    }
            ) {
                Text("View All Transactions")
            }
        }
    }
}