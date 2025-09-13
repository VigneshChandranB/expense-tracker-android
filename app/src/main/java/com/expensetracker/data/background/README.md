# Background Services Implementation

This module implements comprehensive background service management for SMS monitoring with advanced error recovery, battery optimization handling, and lifecycle management.

## Components

### 1. SmsMonitoringService
Enhanced foreground service for continuous SMS monitoring with the following features:

#### Key Features:
- **Foreground Service**: Runs as a foreground service to avoid being killed by the system
- **Error Recovery**: Implements exponential backoff for error recovery with configurable retry limits
- **Health Monitoring**: Continuous health checks every 30 seconds to detect and recover from issues
- **Battery Optimization**: Handles wake locks and battery optimization scenarios
- **Permission Monitoring**: Reactive permission state monitoring with automatic service adjustment
- **Lifecycle Management**: Proper service lifecycle with START_STICKY for automatic restart

#### Error Recovery Strategy:
1. **Exponential Backoff**: Retry delays increase exponentially (1s, 2s, 4s, 8s, etc.)
2. **Maximum Retries**: Stops after 5 failed attempts to prevent infinite loops
3. **Health Reset**: Error count resets after 5 minutes of stable operation
4. **Graceful Degradation**: Service stops gracefully after maximum retries with user notification

#### Battery Optimization Handling:
- **Wake Lock Management**: Acquires partial wake locks during SMS processing
- **Timeout Protection**: Wake locks have 10-minute timeout to prevent battery drain
- **Battery Status Monitoring**: Detects and reports battery optimization status
- **User Guidance**: Provides notifications about battery optimization impact

### 2. SmsReceiver
Enhanced broadcast receiver for incoming SMS messages with efficient processing:

#### Key Features:
- **Smart Filtering**: Pre-filters SMS messages to avoid unnecessary processing
- **Async Processing**: Non-blocking SMS processing with coroutines
- **Wake Lock Management**: Ensures SMS processing completes even when device sleeps
- **Error Handling**: Graceful error handling without crashing the receiver
- **Processing Timeout**: 15-second timeout to prevent hanging operations

#### SMS Filtering Logic:
The receiver uses intelligent pre-filtering to identify potential transaction SMS:
- **Transaction Keywords**: debited, credited, withdrawn, deposited, paid, received, etc.
- **Currency Indicators**: Rs, ₹, INR, numeric amounts
- **Sender Validation**: Bank names, short codes, financial institution patterns

### 3. BackgroundServiceCoordinator
Central coordinator for managing all background services:

#### Key Features:
- **Service Orchestration**: Manages lifecycle of all background services
- **Health Monitoring**: Continuous health checks every 30 seconds
- **Permission Integration**: Reacts to permission changes automatically
- **Recovery Logic**: Detects and recovers unhealthy services
- **Zombie Detection**: Stops services that shouldn't be running
- **Missing Service Detection**: Starts services that should be running but aren't

#### Health Check Logic:
1. **Basic Health**: Service running state matches expected state
2. **Permission Health**: Permissions align with service requirements
3. **Battery Health**: Battery optimization doesn't conflict with service needs
4. **Recovery Actions**: Automatic restart of unhealthy services

### 4. BatteryOptimizationHelper
Comprehensive battery optimization management:

#### Key Features:
- **Device Detection**: Identifies manufacturer-specific battery optimization
- **Impact Assessment**: Evaluates battery optimization impact on SMS monitoring
- **User Guidance**: Provides device-specific setup instructions
- **Intent Creation**: Creates intents to open battery optimization settings

#### Supported Manufacturers:
- **Xiaomi (MIUI)**: Detailed steps for battery saver and autostart settings
- **Huawei/Honor**: Launch management and background activity settings
- **Oppo**: Battery optimization and background app refresh settings
- **Vivo**: Background app refresh and high background activity settings
- **OnePlus**: Battery optimization and app-specific settings
- **Generic Android**: Standard Android battery optimization settings

## Usage

### Starting Background Services
```kotlin
// Initialize all background services
backgroundServiceCoordinator.initializeBackgroundServices()

// Start SMS monitoring specifically
backgroundServiceCoordinator.startSmsMonitoring()
```

### Monitoring Service Health
```kotlin
// Check if services are healthy
val isHealthy = backgroundServiceCoordinator.areServicesHealthy()

// Get detailed service status
val status = backgroundServiceCoordinator.getServiceStatus()
```

### Battery Optimization Management
```kotlin
// Check battery optimization status
val status = batteryOptimizationHelper.getBatteryOptimizationStatus()

// Get device-specific guidance
val guidance = batteryOptimizationHelper.getDeviceSpecificGuidance()

// Create intent to open battery settings
val intent = batteryOptimizationHelper.createBatteryOptimizationIntent()
```

## Testing

### Unit Tests
- **BackgroundServiceCoordinatorTest**: Tests service coordination logic
- **BatteryOptimizationHelperTest**: Tests battery optimization detection and guidance
- **SmsServiceManagerTest**: Tests SMS service lifecycle management
- **EnhancedSmsReceiverTest**: Tests SMS filtering and processing logic

### Integration Tests
- **SmsMonitoringServiceIntegrationTest**: Tests actual service lifecycle
- **Background service health monitoring**: Tests recovery scenarios
- **Battery optimization handling**: Tests various optimization states

## Configuration

### Service Constants
```kotlin
// Health check interval (30 seconds)
private const val HEALTH_CHECK_INTERVAL = 30_000L

// Error recovery settings
private const val MAX_ERROR_COUNT = 5
private const val INITIAL_BACKOFF_DELAY = 1_000L
private const val MAX_BACKOFF_DELAY = 60_000L

// SMS processing timeout (15 seconds)
private const val SMS_PROCESSING_TIMEOUT = 15_000L
```

### Notification Channels
- **Channel ID**: "sms_monitoring_channel"
- **Importance**: LOW (to avoid disturbing users)
- **Features**: No badge, lights, or vibration

## Error Handling

### Service Errors
1. **Logging**: All errors are logged for debugging
2. **User Notification**: Critical errors shown in notification
3. **Recovery Attempts**: Automatic recovery with exponential backoff
4. **Graceful Shutdown**: Service stops after maximum retry attempts

### Permission Errors
1. **Permission Monitoring**: Reactive permission state monitoring
2. **Graceful Degradation**: Service adapts to permission changes
3. **User Guidance**: Clear notifications about permission requirements

### Battery Optimization Issues
1. **Detection**: Automatic detection of battery optimization status
2. **Impact Assessment**: Evaluation of optimization impact on service reliability
3. **User Guidance**: Device-specific instructions for optimization settings
4. **Fallback Options**: Service continues with reduced reliability when optimized

## Performance Considerations

### Memory Management
- **Scoped Coroutines**: All async operations use properly scoped coroutines
- **Wake Lock Timeouts**: Wake locks have timeouts to prevent battery drain
- **Efficient Filtering**: SMS pre-filtering reduces unnecessary processing

### Battery Optimization
- **Minimal Wake Lock Usage**: Wake locks only during active processing
- **Efficient Health Checks**: Health checks use minimal resources
- **Smart Service Management**: Services only run when needed

### Network and Storage
- **Local Processing**: All SMS processing is local (no network calls)
- **Minimal Storage**: Only essential state information is stored
- **Efficient Database Access**: Optimized database operations for SMS data

## Security Considerations

### Permission Management
- **Runtime Permissions**: Proper runtime permission handling
- **Permission Monitoring**: Continuous permission state monitoring
- **Graceful Degradation**: Service continues without SMS access when permissions denied

### Data Privacy
- **Local Processing**: All SMS data processed locally
- **No Network Transmission**: SMS content never sent over network
- **Secure Storage**: Sensitive data encrypted using Android Keystore

### Service Security
- **Foreground Service**: Prevents unauthorized background execution
- **Intent Validation**: All service intents are validated
- **Error Isolation**: Errors in one component don't affect others