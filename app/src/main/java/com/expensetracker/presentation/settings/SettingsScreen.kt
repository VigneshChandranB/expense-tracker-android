package com.expensetracker.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.R

/**
 * Main settings screen with navigation to different settings categories
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToAppSettings: () -> Unit,
    onNavigateToNotificationSettings: () -> Unit,
    onNavigateToDataManagement: () -> Unit,
    onNavigateToPrivacySettings: () -> Unit,
    onNavigateToAccountManagement: () -> Unit,
    onNavigateToBackupRestore: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Settings Categories
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(getSettingsCategories()) { category ->
                SettingsCategoryCard(
                    category = category,
                    onClick = {
                        when (category.id) {
                            "app_settings" -> onNavigateToAppSettings()
                            "notifications" -> onNavigateToNotificationSettings()
                            "data_management" -> onNavigateToDataManagement()
                            "privacy" -> onNavigateToPrivacySettings()
                            "accounts" -> onNavigateToAccountManagement()
                            "backup_restore" -> onNavigateToBackupRestore()
                        }
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // App Info
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_info),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Version: ${uiState.appVersion}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Build: ${uiState.buildNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsCategoryCard(
    category: SettingsCategory,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = category.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class SettingsCategory(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

private fun getSettingsCategories(): List<SettingsCategory> {
    return listOf(
        SettingsCategory(
            id = "app_settings",
            title = "App Settings",
            description = "Theme, language, and general preferences",
            icon = Icons.Default.Settings
        ),
        SettingsCategory(
            id = "notifications",
            title = "Notifications",
            description = "Manage alerts and notification preferences",
            icon = Icons.Default.Notifications
        ),
        SettingsCategory(
            id = "accounts",
            title = "Account Management",
            description = "Add, edit, and manage your bank accounts",
            icon = Icons.Default.AccountBalance
        ),
        SettingsCategory(
            id = "data_management",
            title = "Data Management",
            description = "SMS permissions and data deletion options",
            icon = Icons.Default.Storage
        ),
        SettingsCategory(
            id = "privacy",
            title = "Privacy & Security",
            description = "Security settings and privacy controls",
            icon = Icons.Default.Security
        ),
        SettingsCategory(
            id = "backup_restore",
            title = "Backup & Restore",
            description = "Backup your data and restore from backups",
            icon = Icons.Default.Backup
        )
    )
}