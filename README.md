# Expense Tracker Android App

A modern Android expense tracking application that automatically reads SMS messages from banks to track transactions, built with Jetpack Compose and following Clean Architecture principles.

## Project Structure

The project follows Clean Architecture with the following package structure:

```
app/src/main/java/com/expensetracker/
├── ExpenseTrackerApplication.kt          # Application class with Hilt
├── presentation/                         # UI Layer (Jetpack Compose)
│   ├── MainActivity.kt                   # Main activity
│   └── theme/                           # Material Design 3 theming
├── domain/                              # Business Logic Layer
│   ├── model/                           # Domain entities
│   ├── repository/                      # Repository interfaces
│   └── usecase/                         # Use cases
├── data/                               # Data Layer
│   ├── local/                          # Local data sources
│   │   ├── database/                   # Room database
│   │   ├── entities/                   # Room entities
│   │   ├── dao/                        # Data Access Objects
│   │   └── converters/                 # Type converters
│   └── sms/                           # SMS processing
└── di/                                # Dependency Injection (Hilt modules)
```

## Technologies Used

- **Kotlin** - Primary programming language
- **Jetpack Compose** - Modern UI toolkit
- **Room Database** - Local data persistence
- **Hilt** - Dependency injection
- **Coroutines** - Asynchronous programming
- **Material Design 3** - UI design system
- **Clean Architecture** - Architectural pattern

## Build Configuration

- **Minimum SDK**: 30 (Android 11)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34

## Key Features (Planned)

- Automatic SMS parsing for transaction detection
- Multi-account support
- Intelligent transaction categorization
- Visual spending insights and analytics
- Data export functionality
- Secure local data storage
- Material Design 3 UI

## Getting Started

1. Clone the repository
2. Open in Android Studio
3. Sync project with Gradle files
4. Run the app on an Android device or emulator

## Permissions Required

- `RECEIVE_SMS` - To automatically detect transaction SMS
- `READ_SMS` - To parse existing transaction messages
- `POST_NOTIFICATIONS` - For spending alerts and reminders
- `FOREGROUND_SERVICE` - For background SMS monitoring

## Security & Privacy

- All data is stored locally on the device
- SMS content is processed locally, never transmitted
- Database encryption using Android Keystore
- No network communication for SMS data

## Development Status

This project is currently in development. The basic project structure and dependencies have been set up following Clean Architecture principles.