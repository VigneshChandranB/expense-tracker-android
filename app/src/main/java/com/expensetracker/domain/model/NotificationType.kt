package com.expensetracker.domain.model

/**
 * Types of notifications supported by the expense tracker
 */
enum class NotificationType {
    BILL_DUE_REMINDER,
    SPENDING_LIMIT_ALERT,
    LOW_BALANCE_WARNING,
    UNUSUAL_SPENDING_ALERT,
    BUDGET_EXCEEDED,
    LARGE_TRANSACTION_ALERT
}

/**
 * Priority levels for notifications
 */
enum class NotificationPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}