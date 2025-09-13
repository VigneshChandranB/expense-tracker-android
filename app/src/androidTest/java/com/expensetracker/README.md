# Integration and End-to-End Testing Suite

This directory contains comprehensive integration and end-to-end tests for the Android Expense Tracker application, covering all modules and multi-account scenarios as specified in task 21.

## Test Structure

### 1. Integration Tests (`integration/`)
- **ComprehensiveIntegrationTest.kt**: Tests integration across all modules including multi-account scenarios, SMS processing, and data persistence

### 2. End-to-End Tests (`e2e/`)
- **EndToEndUserJourneyTest.kt**: Complete user journey tests from onboarding to daily usage, including multi-account transfers and multi-bank SMS processing

### 3. UI Tests (`ui/`)
- **CriticalUserFlowsTest.kt**: Automated UI testing for critical user flows including account management, transaction operations, and settings

### 4. Regression Tests (`regression/`)
- **RegressionTestSuite.kt**: Regression testing for core functionality and account operations to prevent feature breakage

### 5. Test Suite (`suite/`)
- **IntegrationTestSuite.kt**: Master test suite that runs all integration and end-to-end tests

### 6. Configuration (`config/`)
- **TestConfiguration.kt**: Test setup utilities, test data creation, and configuration management

## Test Coverage

### Core Functionality Tests
- ✅ Transaction CRUD operations
- ✅ Account management (create, edit, deactivate, reactivate)
- ✅ Account balance calculations
- ✅ Category assignment and management
- ✅ SMS processing accuracy across multiple banks
- ✅ Data export integrity (CSV and PDF)
- ✅ Notification triggers and alerts
- ✅ Search and filter functionality
- ✅ Analytics calculations

### Multi-Account Scenarios
- ✅ Multiple account creation and management
- ✅ Account transfers with linked transactions
- ✅ Multi-account dashboard view
- ✅ Account-specific filtering and operations
- ✅ Cross-account analytics and reporting
- ✅ Account balance synchronization

### Multi-Bank SMS Processing
- ✅ HDFC Bank SMS pattern recognition
- ✅ ICICI Bank SMS pattern recognition
- ✅ SBI Bank SMS pattern recognition
- ✅ AXIS Bank SMS pattern recognition
- ✅ Amount extraction accuracy
- ✅ Merchant name extraction
- ✅ Transaction type detection
- ✅ Account mapping from SMS

### User Journey Tests
- ✅ Complete onboarding flow
- ✅ Privacy explanation and SMS permission setup
- ✅ Multi-account creation during onboarding
- ✅ Feature introduction and tutorials
- ✅ Daily usage workflows
- ✅ Settings configuration
- ✅ Data export and backup

### Critical User Flows
- ✅ Account management workflows
- ✅ Transaction management with validation
- ✅ Transfer operations between accounts
- ✅ Category management
- ✅ Search and filtering
- ✅ Notification settings
- ✅ Data export functionality

## Running Tests

### Run All Integration Tests
```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.expensetracker.suite.IntegrationTestSuite
```

### Run Specific Test Categories

#### Integration Tests Only
```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.expensetracker.integration.ComprehensiveIntegrationTest
```

#### End-to-End Tests Only
```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.expensetracker.e2e.EndToEndUserJourneyTest
```

#### UI Tests Only
```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.expensetracker.ui.CriticalUserFlowsTest
```

#### Regression Tests Only
```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.expensetracker.regression.RegressionTestSuite
```

### Run Tests with Coverage
```bash
./gradlew connectedAndroidTest jacocoTestReport
```

## Test Requirements Mapping

### Requirement 1 (SMS Processing)
- ✅ `smsProcessingIntegration_multiBank()` - Tests SMS processing across multiple banks
- ✅ `smsProcessingAndCategorizationJourney()` - Tests SMS processing with categorization
- ✅ `smsProcessingAccuracy_regression()` - Regression tests for SMS accuracy

### Requirement 2 (Categorization)
- ✅ `categoryAssignment_regression()` - Tests category assignment and changes
- ✅ `categoryManagementFlow()` - Tests category management UI
- ✅ `smsProcessingAndCategorizationJourney()` - Tests automatic categorization

### Requirement 3 (SMS Permissions)
- ✅ `completeNewUserJourney()` - Tests SMS permission setup flow
- ✅ SMS permission handling in onboarding tests

### Requirement 4 (Manual Transactions)
- ✅ `transactionManagementFlow()` - Tests manual transaction operations
- ✅ `coreTransactionOperations_regression()` - Regression tests for transaction CRUD

### Requirement 5 (Analytics)
- ✅ `analyticsCalculation_regression()` - Tests analytics calculations
- ✅ Dashboard integration tests with analytics

### Requirement 6 (Data Export)
- ✅ `dataExportIntegrity_regression()` - Tests export functionality
- ✅ `dataExportAndBackupJourney()` - End-to-end export workflow
- ✅ `dataExportFlow()` - UI tests for export functionality

### Requirement 7 (Notifications)
- ✅ `notificationTriggers_regression()` - Tests notification triggers
- ✅ `notificationAndAlertWorkflow()` - End-to-end notification workflow
- ✅ `notificationSettingsFlow()` - UI tests for notification settings

### Requirement 8 (Security)
- ✅ Security integration tests (covered in existing security tests)
- ✅ Data encryption validation

### Requirement 9 (UI/UX)
- ✅ All UI tests validate Material Design compliance
- ✅ Accessibility testing integration

### Requirement 10 (Multi-Account)
- ✅ `fullAppIntegration_multiAccountScenario()` - Complete multi-account integration
- ✅ `multiAccountTransferWorkflow()` - Multi-account transfer testing
- ✅ `multiAccountOperations_regression()` - Regression tests for multi-account operations
- ✅ `accountManagementFlow()` - Account management UI tests

### Requirement 11 (Performance)
- ✅ Performance validation in integration tests
- ✅ Large data handling tests

## Test Data Management

### Test Accounts
- HDFC Bank Checking Account (Primary Checking)
- ICICI Bank Credit Card (Credit Card)
- SBI Bank Savings Account (Savings Account)
- AXIS Bank Checking Account (Secondary Checking)

### Test Transactions
- Various transaction types (income, expense, transfers)
- Multiple categories and merchants
- Different date ranges for analytics testing

### Test SMS Patterns
- Bank-specific SMS patterns for all major banks
- Amount extraction patterns
- Merchant name extraction patterns
- Date and transaction type patterns

## Continuous Integration

### Pre-commit Tests
Run critical user flow tests before committing:
```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.expensetracker.ui.CriticalUserFlowsTest
```

### Release Testing
Run complete integration test suite before release:
```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.expensetracker.suite.IntegrationTestSuite
```

### Regression Testing
Run regression tests after any core functionality changes:
```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.expensetracker.regression.RegressionTestSuite
```

## Test Environment Setup

### Prerequisites
- Android device or emulator with API level 30+
- SMS permission granted for testing
- Sufficient storage for test data
- Network connectivity for some integration tests

### Test Configuration
- Tests use in-memory database for isolation
- Mock data is created for each test
- Tests clean up after execution
- Parallel test execution is supported

## Troubleshooting

### Common Issues
1. **Test timeouts**: Increase timeout values in test configuration
2. **Permission issues**: Ensure SMS permissions are granted on test device
3. **Database conflicts**: Tests use isolated databases, but ensure proper cleanup
4. **UI timing issues**: Use `waitForIdle()` and proper synchronization

### Debug Mode
Enable debug logging for tests:
```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.debug=true
```

## Maintenance

### Adding New Tests
1. Follow existing test structure and naming conventions
2. Add tests to appropriate category (integration, e2e, ui, regression)
3. Update test suite if adding new test classes
4. Document test coverage for new features

### Updating Tests
1. Update tests when requirements change
2. Maintain backward compatibility where possible
3. Update documentation to reflect changes
4. Run full test suite after updates

This comprehensive testing suite ensures the Android Expense Tracker application meets all requirements and maintains high quality across all features and user scenarios.