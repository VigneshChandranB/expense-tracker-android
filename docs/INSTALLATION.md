# Expense Tracker - Installation Guide

## System Requirements

- **Android Version**: Android 11 (API level 30) or higher
- **RAM**: Minimum 2GB, Recommended 4GB+
- **Storage**: 50MB free space for app installation
- **Permissions**: SMS access (optional but recommended for automatic transaction detection)

## Installation Methods

### Method 1: APK Installation (Sideloading)

1. **Enable Unknown Sources**
   - Go to Settings > Security > Unknown Sources
   - Enable "Allow installation of apps from unknown sources"
   - Or for newer Android versions: Settings > Apps > Special app access > Install unknown apps

2. **Download and Install**
   - Download the `expense-tracker-v1.0.0.apk` file
   - Tap on the APK file to begin installation
   - Follow the on-screen prompts
   - Grant necessary permissions when requested

3. **Launch the App**
   - Find "Expense Tracker" in your app drawer
   - Tap to launch and begin setup

### Method 2: Android App Bundle (AAB) - Play Store Distribution

The AAB file (`app-release.aab`) is intended for distribution through Google Play Store and cannot be directly installed on devices.

## First-Time Setup

### 1. Welcome and Privacy Setup

When you first launch the app, you'll be guided through the setup process:

1. **Welcome Screen**: Introduction to the app's features
2. **Privacy Explanation**: Clear explanation of how SMS data is handled
3. **SMS Permission Setup**: Optional but recommended for automatic transaction detection

### 2. Account Configuration

#### Adding Your First Account

1. **Account Details**
   - Bank Name: Select or enter your bank name
   - Account Type: Choose from Savings, Checking, Credit Card, Investment, or Cash
   - Account Nickname: Give your account a memorable name (e.g., "Main Checking", "HDFC Credit Card")
   - Account Number: Enter the last 4 digits for identification (optional)

2. **Initial Balance** (Optional)
   - Enter your current account balance
   - This helps with accurate tracking from day one

#### Multi-Account Setup

The app supports multiple accounts from different banks:

1. **Adding Additional Accounts**
   - Go to Settings > Account Management
   - Tap "Add New Account"
   - Repeat the account setup process

2. **Account Types Supported**
   - **Savings Accounts**: Regular savings accounts
   - **Checking Accounts**: Current/checking accounts
   - **Credit Cards**: Credit card accounts (expenses show as positive amounts)
   - **Investment Accounts**: Investment and trading accounts
   - **Cash**: Physical cash tracking

3. **Account Switching**
   - Use the account selector on the dashboard
   - View consolidated or individual account data
   - Transfer money between your accounts

### 3. SMS Permission Configuration

#### Granting SMS Access

1. **Why SMS Access?**
   - Automatically detects bank transaction SMS
   - Extracts transaction details (amount, merchant, date)
   - Saves time on manual entry

2. **Granting Permission**
   - Tap "Grant SMS Permission" during setup
   - Select "Allow" when prompted by Android
   - The app only reads SMS locally on your device

3. **Bank SMS Pattern Setup**
   - The app comes with pre-configured patterns for major Indian banks
   - Supported banks include: HDFC, ICICI, SBI, Axis, Kotak, and more
   - Custom patterns can be added for other banks

#### Privacy and Security

- **Local Processing**: All SMS data is processed locally on your device
- **No Data Upload**: SMS content never leaves your device
- **Encrypted Storage**: All data is encrypted using Android Keystore
- **Permission Control**: You can revoke SMS access anytime in settings

### 4. Category Setup

#### Default Categories

The app comes with pre-configured categories:
- Food & Dining
- Shopping
- Transportation
- Bills & Utilities
- Entertainment
- Healthcare
- Investment
- Income

#### Custom Categories

1. **Adding Categories**
   - Go to Settings > Category Management
   - Tap "Add Category"
   - Choose name, icon, and color

2. **Category Rules**
   - Set up automatic categorization rules
   - Based on merchant names or keywords
   - The app learns from your manual corrections

### 5. Notification Setup

#### Configuring Alerts

1. **Bill Reminders**
   - Set up recurring bill due dates
   - Get notifications 3 days before due date

2. **Spending Limits**
   - Set monthly spending limits per category
   - Receive alerts when approaching limits

3. **Account Alerts**
   - Low balance warnings
   - Unusual spending pattern detection
   - Large transaction notifications

## Multi-Bank SMS Setup

### Supported Banks

The app automatically recognizes SMS from these banks:

#### Major Indian Banks
- **HDFC Bank**: All account types
- **ICICI Bank**: Savings, Credit Cards
- **State Bank of India (SBI)**: All account types
- **Axis Bank**: Savings, Credit Cards
- **Kotak Mahindra Bank**: All account types
- **Punjab National Bank (PNB)**: Savings accounts
- **Bank of Baroda**: Savings accounts
- **Canara Bank**: Savings accounts

#### Credit Card Companies
- **American Express**: Credit cards
- **Citibank**: Credit cards
- **Standard Chartered**: Credit cards

### Account Mapping

1. **Automatic Detection**
   - The app tries to match SMS to accounts based on account numbers
   - Uses last 4 digits or account identifiers in SMS

2. **Manual Mapping**
   - If automatic detection fails, you can manually map SMS patterns
   - Go to Settings > SMS Patterns > Add Bank Pattern
   - Define regex patterns for your specific bank format

3. **Multiple Accounts Same Bank**
   - Use account nicknames to distinguish accounts
   - The app uses account numbers in SMS to route transactions correctly

## Troubleshooting

### Common Issues

#### SMS Not Being Detected

1. **Check Permissions**
   - Ensure SMS permission is granted
   - Check if the app has notification access

2. **Bank Not Supported**
   - Add custom SMS pattern in settings
   - Contact support with sample SMS format (remove sensitive data)

3. **Wrong Account Assignment**
   - Check account number mapping in settings
   - Manually assign transactions if needed

#### App Performance

1. **Slow Performance**
   - Clear app cache in Android settings
   - Restart the app
   - Ensure sufficient device storage

2. **Battery Optimization**
   - Disable battery optimization for the app
   - Allow background activity for SMS monitoring

#### Data Issues

1. **Missing Transactions**
   - Check if SMS permission is still granted
   - Verify SMS patterns for your bank
   - Add transactions manually if needed

2. **Incorrect Categorization**
   - Manually correct categories to train the system
   - Review and update categorization rules

### Getting Help

1. **In-App Help**
   - Settings > Help & Support
   - FAQ section with common questions

2. **Contact Support**
   - Email: support@expensetracker.com
   - Include device model, Android version, and issue description

3. **Community**
   - User forums and community support
   - Share tips and bank SMS patterns

## Security Best Practices

1. **Device Security**
   - Use screen lock (PIN, pattern, fingerprint)
   - Keep your device updated
   - Don't root your device if possible

2. **App Security**
   - Regularly update the app
   - Review permissions periodically
   - Use backup features for data protection

3. **Data Protection**
   - Enable automatic backups
   - Store backup files securely
   - Don't share sensitive financial data

## Advanced Features

### Data Export

1. **Export Formats**
   - CSV for spreadsheet analysis
   - PDF for formatted reports

2. **Export Options**
   - Date range selection
   - Account-specific exports
   - Category-wise breakdowns

### Analytics and Insights

1. **Spending Analysis**
   - Monthly spending trends
   - Category-wise breakdowns
   - Account comparisons

2. **Budget Tracking**
   - Set spending limits
   - Track progress against budgets
   - Receive overspending alerts

### Backup and Restore

1. **Automatic Backups**
   - Enable in Settings > Backup & Restore
   - Encrypted local backups
   - Cloud storage integration (optional)

2. **Manual Backup**
   - Export all data to secure location
   - Include account settings and categories
   - Test restore process periodically

This installation guide ensures you can successfully set up and use the Expense Tracker app with multiple bank accounts for comprehensive financial management.