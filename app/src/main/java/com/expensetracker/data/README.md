# Data Layer Implementation

This document describes the core data models and database schema implementation for the Android Expense Tracker app.

## Overview

The data layer follows Clean Architecture principles with a clear separation between domain models and data entities. It uses Room database for local storage with proper foreign key relationships and migration strategies.

## Domain Models

### Core Models
- **Transaction**: Represents financial transactions with support for transfers between accounts
- **Account**: Represents bank accounts with different types (Savings, Checking, Credit Card, etc.)
- **Category**: Represents transaction categories with hierarchical support
- **SmsPattern**: Represents patterns for parsing bank SMS messages

### Enums
- **TransactionType**: INCOME, EXPENSE, TRANSFER_OUT, TRANSFER_IN
- **AccountType**: SAVINGS, CHECKING, CREDIT_CARD, INVESTMENT, CASH
- **TransactionSource**: SMS_AUTO, MANUAL, IMPORTED

## Database Schema

### Entities
All entities are properly mapped with Room annotations and include:

1. **TransactionEntity**: Core transaction data with foreign key relationships
2. **AccountEntity**: Account information with unique constraints
3. **CategoryEntity**: Category data with hierarchical support
4. **SmsPatternEntity**: SMS parsing patterns for different banks

### Foreign Key Relationships
- Transactions → Accounts (CASCADE delete)
- Transactions → Categories (SET_DEFAULT on delete)
- Transactions → Transfer Accounts (SET_NULL on delete)
- Categories → Parent Categories (CASCADE delete)

### Indices
Optimized indices for:
- Account lookups
- Category filtering
- Date range queries
- Transfer relationships
- SMS pattern matching

## Data Access Objects (DAOs)

### TransactionDao
- CRUD operations for transactions
- Date range queries
- Category and account filtering
- Transfer relationship management
- Merchant and source filtering
- Balance calculations

### AccountDao
- Account management operations
- Balance calculations and updates
- Account type and bank filtering
- Status management (active/inactive)
- Search functionality

### CategoryDao
- Category management
- Hierarchical category support
- Default vs custom categories
- Search and filtering

### SmsPatternDao
- SMS pattern management
- Bank-specific pattern retrieval
- Active/inactive pattern management

## Database Migrations

### Version 1 → 2
- Added foreign key constraint for transferAccountId
- Ensured default "Uncategorized" category exists
- Updated indices for better performance

### Default Data Population
- 10 default categories including "Uncategorized"
- SMS patterns for major Indian banks (HDFC, ICICI, SBI, Axis)
- Proper color coding and icons for categories

## Type Converters

Custom converters for:
- BigDecimal ↔ String (precision preservation)
- LocalDateTime ↔ Long (timestamp storage)

## Testing

### Unit Tests
- Entity data class validation
- Domain model creation and validation
- Enum value verification

### Integration Tests
- Database operations
- DAO functionality
- Foreign key constraints
- Migration testing
- Default data population

### Key Test Scenarios
- Transaction CRUD operations
- Transfer transaction linking
- Account balance calculations
- Foreign key cascade behavior
- Category hierarchy management
- SMS pattern matching

## Usage Examples

### Creating a Transaction
```kotlin
val transaction = Transaction(
    amount = BigDecimal("100.50"),
    type = TransactionType.EXPENSE,
    category = foodCategory,
    merchant = "McDonald's",
    description = "Lunch",
    date = LocalDateTime.now(),
    source = TransactionSource.SMS_AUTO,
    accountId = 1L
)
```

### Creating a Transfer
```kotlin
val transferOut = Transaction(
    amount = BigDecimal("500.00"),
    type = TransactionType.TRANSFER_OUT,
    category = transferCategory,
    merchant = "Transfer",
    accountId = fromAccountId,
    transferAccountId = toAccountId,
    source = TransactionSource.MANUAL
)
```

### Account Management
```kotlin
val account = Account(
    bankName = "HDFC Bank",
    accountType = AccountType.SAVINGS,
    accountNumber = "1234567890",
    nickname = "Primary Savings",
    currentBalance = BigDecimal("10000.50"),
    isActive = true,
    createdAt = LocalDateTime.now()
)
```

## Security Considerations

- All sensitive data stored locally with Room encryption support
- BigDecimal used for monetary values to prevent precision loss
- Foreign key constraints ensure data integrity
- Proper indexing for performance optimization

## Performance Optimizations

- Strategic database indices for common queries
- Efficient foreign key relationships
- Batch operations support
- Flow-based reactive queries for UI updates
- Proper database migration strategies

This implementation provides a solid foundation for the expense tracking functionality while maintaining data integrity, performance, and extensibility.