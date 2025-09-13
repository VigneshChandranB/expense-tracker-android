# SMS Parsing and Transaction Extraction Engine

This module implements a comprehensive SMS parsing and transaction extraction engine for the Android Expense Tracker app. It automatically reads and parses SMS messages from banks and financial institutions to extract transaction details.

## Architecture Overview

The SMS parsing engine follows a modular architecture with the following key components:

### Core Components

1. **SmsTransactionExtractor** - Main interface and implementation for extracting transactions from SMS messages
2. **BankPatternRegistry** - Manages SMS patterns for different banks and financial institutions
3. **AccountMappingService** - Maps SMS account identifiers to user accounts
4. **TransactionExtractionResult** - Result wrapper containing extracted transaction and confidence metrics

## Key Features

### 1. Multi-Bank Support

The engine supports SMS patterns from major Indian banks and financial services:

- **HDFC Bank** - Debit/Credit cards and accounts
- **ICICI Bank** - Various account types and credit cards
- **State Bank of India (SBI)** - Savings and current accounts
- **Axis Bank** - Credit cards and bank accounts
- **Kotak Mahindra Bank** - All account types
- **Paytm Payments Bank** - Wallet transactions
- **PhonePe** - UPI transactions
- **Google Pay** - UPI and wallet transactions

### 2. Intelligent Field Extraction

The engine extracts the following fields from SMS messages:

- **Amount** - Transaction amount with currency normalization
- **Transaction Type** - Debit, Credit, Transfer (In/Out)
- **Merchant Name** - Vendor or recipient information
- **Date/Time** - Transaction timestamp with multiple format support
- **Account Identifier** - Masked account numbers or card endings

### 3. Confidence Scoring System

Each extraction is assigned a confidence score (0.0 to 1.0) based on:

- **Field Completeness** (30% weight) - Amount extraction success
- **Type Detection** (20% weight) - Transaction type identification
- **Merchant Extraction** (15% weight) - Merchant name quality
- **Date Parsing** (10% weight) - Date format recognition
- **Account Mapping** (10% weight) - Account identifier match
- **Pattern Matching** (10% weight) - SMS pattern confidence
- **Sender Trust** (5% weight) - Known bank sender verification

### 4. Robust Error Handling

The engine handles various error scenarios:

- **Malformed SMS** - Invalid or incomplete transaction data
- **Unknown Senders** - SMS from unrecognized sources
- **Pattern Mismatches** - SMS that don't match any bank pattern
- **Field Validation Errors** - Invalid amounts or data formats

### 5. Account Mapping

The system maintains mappings between SMS account identifiers and user accounts:

- **Multiple Accounts per Bank** - Support for users with multiple accounts
- **Cross-Bank Support** - Same identifier across different banks
- **Dynamic Mapping** - Runtime account association
- **Mapping Management** - CRUD operations for account mappings

## Usage Examples

### Basic Transaction Extraction

```kotlin
val extractor = RegexSmsTransactionExtractor(bankPatternRegistry, accountMappingService)

val smsMessage = SmsMessage(
    id = 1,
    sender = "VK-HDFCBK",
    body = "Rs.1500.00 debited from A/c no XXXX1234 at AMAZON on 15-01-2024 14:30:25",
    timestamp = Date()
)

val result = extractor.extractTransaction(smsMessage)

if (result.isSuccessful) {
    val transaction = result.transaction!!
    println("Amount: ${transaction.amount}")
    println("Type: ${transaction.type}")
    println("Merchant: ${transaction.merchant}")
    println("Confidence: ${result.confidenceScore}")
}
```

### Registering Custom Bank Patterns

```kotlin
val customPattern = SmsPattern(
    bankName = "Custom Bank",
    senderPattern = ".*CUSTOMBANK.*",
    amountPattern = "Rs\\.([\\d,]+(?:\\.\\d{2})?)",
    merchantPattern = "at\\s+([A-Za-z0-9\\s]+?)(?:\\s+on|\\.|$)",
    datePattern = "(\\d{2}-\\d{2}-\\d{4}\\s+\\d{2}:\\d{2}:\\d{2})",
    typePattern = "(debited|credited)",
    accountPattern = "A/c\\s+([X\\d]+)",
    isActive = true
)

extractor.registerBankPattern(customPattern)
```

### Account Mapping Management

```kotlin
// Create account mapping
accountMappingService.createMapping(
    accountId = 1L,
    bankName = "HDFC Bank",
    accountIdentifier = "XXXX1234"
)

// Find account by identifier
val accountId = accountMappingService.findAccountByIdentifier("HDFC Bank", "XXXX1234")
```

## SMS Pattern Format

Each bank pattern consists of regex patterns for different fields:

```kotlin
data class SmsPattern(
    val id: Long = 0,
    val bankName: String,                    // Bank or service name
    val senderPattern: String,               // SMS sender regex
    val amountPattern: String,               // Amount extraction regex
    val merchantPattern: String,             // Merchant name regex
    val datePattern: String,                 // Date/time regex
    val typePattern: String,                 // Transaction type regex
    val accountPattern: String? = null,      // Account identifier regex
    val isActive: Boolean = true             // Pattern active status
)
```

### Example Patterns

#### HDFC Bank Pattern
```kotlin
SmsPattern(
    bankName = "HDFC Bank",
    senderPattern = ".*HDFC.*|.*VK-HDFCBK.*",
    amountPattern = "Rs\\.([\\d,]+(?:\\.\\d{2})?)",
    merchantPattern = "(?:at|to|from)\\s+([A-Za-z0-9\\s&.-]+?)(?:\\s+on|\\s+dt|\\.|,|$)",
    datePattern = "(\\d{2}-\\d{2}-\\d{4}\\s+\\d{2}:\\d{2}:\\d{2})",
    typePattern = "(debited|credited|debit|credit)",
    accountPattern = "(?:A/c|account)\\s+(?:no\\.?)?\\s*([X\\d]+)"
)
```

#### UPI Pattern (PhonePe)
```kotlin
SmsPattern(
    bankName = "PhonePe",
    senderPattern = ".*PHONEPE.*",
    amountPattern = "Rs\\.([\\d,]+(?:\\.\\d{2})?)",
    merchantPattern = "(?:to|from)\\s+([A-Za-z0-9\\s&.-]+?)(?:\\s+via|\\s+on|\\.|$)",
    datePattern = "(\\d{2}-\\d{2}-\\d{4})",
    typePattern = "(paid|received)"
)
```

## Testing

The module includes comprehensive test coverage:

### Unit Tests
- **SmsTransactionExtractorTest** - Core extraction logic
- **BankPatternRegistryTest** - Pattern management
- **AccountMappingServiceTest** - Account mapping functionality

### Integration Tests
- **SmsParsingIntegrationTest** - End-to-end parsing scenarios with real SMS formats

### Test Coverage Areas
- Multiple bank SMS formats
- Various transaction types (debit, credit, transfer, UPI)
- Error handling scenarios
- Confidence scoring validation
- Pattern registration and management
- Account mapping operations

## Performance Considerations

### Optimization Features
- **Fast Pattern Matching** - Efficient regex compilation and caching
- **Processing Time Tracking** - Performance monitoring for each extraction
- **Memory Management** - Minimal object allocation during parsing
- **Confidence Bonuses** - Performance-based confidence adjustments

### Benchmarks
- **SMS Processing** - Target: <100ms per message
- **Pattern Matching** - Optimized regex patterns for speed
- **Memory Usage** - Minimal heap allocation during extraction

## Security and Privacy

### Data Protection
- **Local Processing** - All SMS parsing happens on-device
- **No Network Transmission** - SMS content never leaves the device
- **Encrypted Storage** - Sensitive data encrypted using Android Keystore
- **Permission Management** - Explicit user consent for SMS access

### Privacy Features
- **Selective Processing** - Only processes transaction-related SMS
- **Data Minimization** - Extracts only necessary transaction fields
- **User Control** - Users can disable SMS processing anytime
- **Transparent Operations** - Clear logging of all processing activities

## Extension Points

### Adding New Banks
1. Create SMS pattern with appropriate regex patterns
2. Register pattern using `BankPatternRegistry`
3. Add test cases for the new bank format
4. Update documentation with pattern examples

### Custom Field Extraction
1. Extend `SmsPattern` with new field patterns
2. Update `extractFields()` method in extractor
3. Modify confidence scoring to include new fields
4. Add validation for new field types

### Enhanced Confidence Scoring
1. Implement custom `ConfidenceFactors` calculation
2. Add new scoring criteria (e.g., merchant database lookup)
3. Adjust weights based on field importance
4. Include machine learning models for pattern confidence

## Dependencies

- **Kotlin Coroutines** - Asynchronous processing
- **Hilt/Dagger** - Dependency injection
- **Room Database** - Pattern and mapping persistence
- **JUnit 4** - Unit testing framework
- **MockK** - Mocking framework for tests

## Future Enhancements

### Planned Features
- **Machine Learning Integration** - AI-powered merchant categorization
- **Dynamic Pattern Learning** - Automatic pattern discovery from new SMS formats
- **Multi-Language Support** - SMS parsing in regional languages
- **Advanced Analytics** - Transaction pattern analysis and insights
- **Cloud Sync** - Optional cloud backup of patterns and mappings

### Performance Improvements
- **Parallel Processing** - Multi-threaded SMS processing
- **Pattern Caching** - Intelligent pattern caching strategies
- **Batch Processing** - Efficient bulk SMS processing
- **Memory Optimization** - Further memory usage improvements