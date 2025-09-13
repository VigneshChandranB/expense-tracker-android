package com.expensetracker.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * Accessible bottom navigation bar with proper semantics
 */
@Composable
fun AccessibleBottomNavigation(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    NavigationBar(
        modifier = modifier.semantics {
            contentDescription = "Main navigation"
            role = Role.TabList
        }
    ) {
        NavigationItem.values().forEach { item ->
            val isSelected = currentRoute == item.route
            
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null // Content description is on the item itself
                    )
                },
                label = {
                    Text(item.title)
                },
                selected = isSelected,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            // Pop up to the start destination to avoid building up a large stack
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    }
                },
                modifier = Modifier.semantics {
                    contentDescription = "${item.title} tab"
                    role = Role.Tab
                    if (isSelected) {
                        stateDescription = "Selected"
                    }
                }
            )
        }
    }
}

/**
 * Navigation items with accessibility information
 */
enum class NavigationItem(
    val title: String,
    val icon: ImageVector,
    val route: String,
    val contentDescription: String
) {
    DASHBOARD(
        title = "Dashboard",
        icon = Icons.Default.Dashboard,
        route = "dashboard",
        contentDescription = "Dashboard - View account overview and recent transactions"
    ),
    TRANSACTIONS(
        title = "Transactions",
        icon = Icons.Default.Receipt,
        route = "transactions",
        contentDescription = "Transactions - View and manage all transactions"
    ),
    ACCOUNTS(
        title = "Accounts",
        icon = Icons.Default.AccountBalance,
        route = "accounts",
        contentDescription = "Accounts - Manage bank accounts and balances"
    ),
    ANALYTICS(
        title = "Analytics",
        icon = Icons.Default.Analytics,
        route = "analytics",
        contentDescription = "Analytics - View spending insights and reports"
    ),
    SETTINGS(
        title = "Settings",
        icon = Icons.Default.Settings,
        route = "settings",
        contentDescription = "Settings - Configure app preferences and permissions"
    )
}

/**
 * Accessible screen transitions with proper announcements
 */
@Composable
fun AccessibleScreenTransition(
    targetState: String,
    content: @Composable AnimatedVisibilityScope.(String) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        transitionSpec = {
            slideInHorizontally(
                initialOffsetX = { width -> width },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300)) with
                    slideOutHorizontally(
                        targetOffsetX = { width -> -width },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
        },
        modifier = Modifier.semantics {
            liveRegion = LiveRegionMode.Polite
        }
    ) { screen ->
        content(screen)
    }
}

/**
 * Accessible top app bar with proper navigation semantics
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibleTopAppBar(
    title: String,
    onNavigationClick: (() -> Unit)? = null,
    navigationIcon: ImageVector? = null,
    navigationContentDescription: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                modifier = Modifier.semantics {
                    heading()
                    contentDescription = "$title screen"
                }
            )
        },
        navigationIcon = {
            if (onNavigationClick != null && navigationIcon != null) {
                IconButton(
                    onClick = onNavigationClick,
                    modifier = Modifier.semantics {
                        contentDescription = navigationContentDescription ?: "Navigate back"
                        role = Role.Button
                    }
                ) {
                    Icon(
                        imageVector = navigationIcon,
                        contentDescription = null
                    )
                }
            }
        },
        actions = actions,
        modifier = modifier.semantics {
            contentDescription = "Top app bar"
        }
    )
}

/**
 * Accessible floating action button with proper semantics
 */
@Composable
fun AccessibleFloatingActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    text: String? = null
) {
    if (expanded && text != null) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            icon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null
                )
            },
            text = {
                Text(text)
            },
            modifier = modifier.semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            }
        )
    } else {
        FloatingActionButton(
            onClick = onClick,
            modifier = modifier.semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
        }
    }
}

/**
 * Accessible dialog with proper focus management
 */
@Composable
fun AccessibleAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    text: String,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = title,
                modifier = Modifier.semantics {
                    heading()
                }
            )
        },
        text = {
            Text(text)
        },
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        modifier = modifier.semantics {
            contentDescription = "Dialog: $title"
            role = Role.Dialog
        }
    )
}