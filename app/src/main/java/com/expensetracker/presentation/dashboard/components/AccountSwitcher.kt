package com.expensetracker.presentation.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expensetracker.domain.model.Account

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSwitcher(
    accounts: List<Account>,
    selectedAccount: Account?,
    onAccountSelected: (Account?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "Accounts",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            // All Accounts option
            item {
                FilterChip(
                    onClick = { onAccountSelected(null) },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("All Accounts")
                        }
                    },
                    selected = selectedAccount == null,
                    shape = RoundedCornerShape(20.dp)
                )
            }
            
            // Individual accounts
            items(accounts.filter { it.isActive }) { account ->
                FilterChip(
                    onClick = { onAccountSelected(account) },
                    label = {
                        Column {
                            Text(
                                text = account.nickname,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = account.bankName,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    },
                    selected = selectedAccount?.id == account.id,
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }
    }
}