# AutoFlow - Android Automation App

## 📱 Overview

**AutoFlow** is a sophisticated Android automation application built with modern Android development practices. It allows users to create powerful automation workflows by defining triggers (events) and actions (responses). The app enables users to automate everyday tasks based on time, location, WiFi connection, Bluetooth devices, and more.

### Purpose
AutoFlow empowers users to:
- Automate repetitive tasks on their Android device
- Create context-aware automations (location-based, time-based, etc.)
- Improve productivity by eliminating manual actions
- Customize device behavior based on various conditions

---

## ✨ Features

### Core Functionality

#### 1. **Workflow Management**
- ✅ Create, read, update, and delete automation workflows
- ✅ Enable/disable workflows with a simple toggle
- ✅ Persistent storage using Room Database
- ✅ Real-time workflow status monitoring
- ✅ Swipe-to-delete gesture support

#### 2. **Trigger Types**
AutoFlow supports multiple trigger types to initiate automations:

- **⏰ Time-based Triggers**: Schedule actions at specific times
  - Quick time options (5 min, 15 min, 30 min, 1 hour, 2 hours, 1 day)
  - Custom date and time selection
  - Managed with AlarmManager for precise scheduling

- **📍 Location-based Triggers**: Activate workflows when entering/exiting locations
  - Manual coordinate entry
  - Interactive Google Maps selection
  - Configurable radius (20m - 1000m)
  - Entry, exit, or both event detection
  - Geofencing integration

- **📶 WiFi Triggers**: Respond to WiFi state changes
  - Detect connection to specific networks
  - WiFi on/off state changes
  - Network entry/exit detection

- **🔵 Bluetooth (BLE) Triggers**: Detect Bluetooth device connections
  - Connect to specific BLE devices
  - Device address/MAC address filtering
  - Automatic device scanning

#### 3. **Action Types**
When triggers activate, AutoFlow can perform various actions:

- **🔔 Send Notifications**: Display custom notifications
  - Configurable title and message
  - Priority levels (Low, Normal, High, Max)
  - Multiple notification channels

- **🔌 Toggle Device Settings**:
  - WiFi on/off
  - Bluetooth on/off
  - Sound modes (Ring, Vibrate, Silent, DND)

- **📜 Run Custom Scripts**: Execute JavaScript code
  - Rhino JavaScript engine integration
  - Built-in utility functions:
    - `log(message)` - Console logging
    - `notify(title, message)` - Send notifications
    - `httpGet(url)` - Make HTTP requests
    - `androidContext` - Access Android context
  - Script validation and error handling

- **📱 Block Apps**: Accessibility-based app blocking
- **🔊 Audio Control**: Set ringer modes, DND states

#### 4. **User Interface**

**Modern Material 3 Design**:
- ✅ Dark/Light theme support
- ✅ Smooth animations and transitions
- ✅ Intuitive navigation with bottom bar
- ✅ Card-based workflow display
- ✅ Icon-based visual workflow identification

**Navigation Structure**:
- **Home**: Dashboard showing all workflows with status
- **Create Task**: Wizard-style workflow creation
- **Profile**: User profile management
- **Settings**: App configuration and preferences

**Key UI Features**:
- Real-time workflow status indicators
- Color-coded trigger type icons
- Workflow cards with toggle switches
- Dropdown menu for edit/delete actions
- Floating Action Button (FAB) for quick creation
- Empty state guidance for new users

#### 5. **Advanced Features**

- **Device Admin**: Optional device policy controller support
- **Accessibility Service**: For advanced app blocking features
- **Background Execution**: WorkManager for reliable background tasks
- **Geofencing**: Location monitoring with Google Play Services
- **Notification Channels**: Organized notification system
- **Permission Management**: Runtime permission handling
- **Alarm Scheduling**: Exact alarm scheduling (Android 12+)

---

## 🏗️ Architecture & Technologies

### Architecture Pattern
**MVVM (Model-View-ViewModel)** with Repository pattern:

```
┌─────────────────────────────────────────────────┐
│              UI Layer (Jetpack Compose)          │
│  Dashboard, HomeScreen, TaskCreationScreen, etc. │
└─────────────────────────────────────────────────┘
                        ↕
┌─────────────────────────────────────────────────┐
│            ViewModel Layer                       │
│          WorkflowViewModel                       │
└─────────────────────────────────────────────────┘
                        ↕
┌─────────────────────────────────────────────────┐
│            Repository Layer                      │
│          WorkflowRepository                      │
└─────────────────────────────────────────────────┘
                        ↕
┌─────────────────────────────────────────────────┐
│         Data Layer (Room Database)               │
│    WorkflowEntity, WorkflowDao, AppDatabase      │
└─────────────────────────────────────────────────┘
```

### Technology Stack

#### **Core Android**
- **Language**: Kotlin + Java (hybrid codebase)
  - Kotlin: ~45% (UI, utilities, managers)
  - Java: ~55% (data layer, models, workers)
- **Minimum SDK**: 31 (Android 12)
- **Target SDK**: 36 (Android 14+)
- **Build Tool**: Gradle with Kotlin DSL

#### **UI Framework**
- **Jetpack Compose**: Modern declarative UI
- **Material 3**: Latest Material Design components
- **Navigation Compose**: Type-safe navigation
- **Compose BOM**: Version management for Compose libraries

#### **Data & State Management**
- **Room**: SQLite ORM for local data persistence
- **LiveData**: Observable data holder for UI updates
- **ViewModel**: UI-related data lifecycle management
- **Repository Pattern**: Clean separation of data sources

#### **Background Processing**
- **WorkManager**: Reliable background task execution
- **AlarmManager**: Precise scheduled task execution
- **Receivers**: Broadcast receivers for system events

#### **Location & Maps**
- **Google Play Services Location**: Location APIs
- **Google Maps Compose**: Interactive map integration
- **Geofencing API**: Location-based triggers

#### **Integrations**
- **Rhino Android**: JavaScript engine for script execution
- **OkHttp**: HTTP client for network requests
- **Kotlin Coroutines**: Asynchronous programming

#### **Testing**
- **JUnit**: Unit testing framework
- **Mockito**: Mocking framework
- **Espresso**: UI testing
- **Compose UI Test**: Jetpack Compose testing

---

## 📋 Requirements

### Development Requirements

#### **System Requirements**
- Android Studio Hedgehog (2023.1.1) or later
- JDK 21
- Gradle 8.0+
- Android SDK 36
- Minimum 8 GB RAM (16 GB recommended)

#### **Dependencies**
All dependencies are managed through Gradle. Key libraries include:

```kotlin
// Compose & UI
androidx.compose.ui
androidx.compose.material3
androidx.navigation.compose

// Data & Architecture
androidx.room.runtime
androidx.lifecycle.livedata
androidx.lifecycle.viewmodel
androidx.work.runtime

// Location & Maps
com.google.android.gms:play-services-location
com.google.android.gms:play-services-maps
com.google.maps.android:maps-compose

// Script Engine
io.github.detekt:detekt-rhino-android

// Network
com.squareup.okhttp3:okhttp

// Coroutines
org.jetbrains.kotlinx:kotlinx-coroutines-play-services
```

### Runtime Requirements

#### **Android Device Requirements**
- Android 12 (API 31) or higher
- GPS/Location services
- Bluetooth 4.0+ (for BLE triggers)
- WiFi capability
- Minimum 100 MB free storage

#### **Required Permissions**
AutoFlow requests the following permissions:

**Location**:
- `ACCESS_FINE_LOCATION`
- `ACCESS_COARSE_LOCATION`
- `ACCESS_BACKGROUND_LOCATION`

**Bluetooth**:
- `BLUETOOTH` (API ≤30)
- `BLUETOOTH_ADMIN` (API ≤30)
- `BLUETOOTH_CONNECT` (API 31+)
- `BLUETOOTH_SCAN` (API 31+)

**Notifications**:
- `POST_NOTIFICATIONS` (API 33+)
- `ACCESS_NOTIFICATION_POLICY`

**Alarms**:
- `SCHEDULE_EXACT_ALARM` (API 31+)
- `RECEIVE_BOOT_COMPLETED`

**Connectivity**:
- `INTERNET`
- `CHANGE_WIFI_STATE`
- `ACCESS_WIFI_STATE`

**Other**:
- `READ_PHONE_STATE`
- `SEND_SMS` / `READ_SMS`
- `MODIFY_AUDIO_SETTINGS`
- `VIBRATE`
- `BIND_DEVICE_ADMIN` (optional)
- `BIND_ACCESSIBILITY_SERVICE` (optional)

---

## 🚀 How to Build and Run

### Building the Project

1. **Clone the Repository**
   ```bash
   git clone https://github.com/l1kiiiiii/AutoFlow.git
   cd AutoFlow
   ```

2. **Checkout the l1kiii-1 Branch**
   ```bash
   git checkout l1kiii-1
   ```

3. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned directory
   - Wait for Gradle sync to complete

4. **Configure API Keys** (if needed)
   - Create a `local.properties` file in the project root
   - Add your Google Maps API key:
     ```
     MAPS_API_KEY=your_api_key_here
     ```

5. **Build the Project**
   ```bash
   ./gradlew build
   ```

   Or use Android Studio:
   - Build → Make Project (Ctrl+F9)

6. **Run on Device/Emulator**
   ```bash
   ./gradlew installDebug
   ```

   Or in Android Studio:
   - Run → Run 'app' (Shift+F10)
   - Select a device/emulator

### Building Release APK

```bash
./gradlew assembleRelease
```

The APK will be generated at:
`app/build/outputs/apk/release/app-release.apk`

---

## 📖 User Guide - How to Navigate

### First Launch

1. **Grant Permissions**: The app will request necessary permissions
2. **Home Screen**: You'll see an empty dashboard with a message to create your first workflow

### Creating Your First Automation

#### Step 1: Create a New Workflow

1. Tap the **"+"** Floating Action Button or the **Create** tab
2. Enter a **Workflow Name** (e.g., "Silent at Work")

#### Step 2: Configure Trigger

3. Select **Trigger Type**:
   - **Time**: Choose a specific time
   - **Location**: Select or enter coordinates
   - **WiFi**: Choose network connection event
   - **Bluetooth**: Select BLE device

4. Configure trigger details:
   - For **Time**: Pick date and time, or use quick options
   - For **Location**: Use map or enter coordinates, set radius
   - For **WiFi**: Specify SSID or state change
   - For **Bluetooth**: Enter device address

#### Step 3: Configure Action

5. Select **Action Type**:
   - **Send Notification**: Create custom notification
   - **Toggle WiFi**: Turn WiFi on/off
   - **Run Script**: Write JavaScript code
   - **Set Sound Mode**: Change ringer mode

6. Configure action details based on type

#### Step 4: Save

7. Tap **"Save Task"** button
8. Your workflow appears on the home screen

### Managing Workflows

**On the Home Screen**:

- **View All Workflows**: Scroll through the list
- **Enable/Disable**: Use the toggle switch on each card
- **Edit**: Tap the three-dot menu → Edit
- **Delete**: Tap the three-dot menu → Delete (with confirmation)
- **View Details**: Tap on a workflow card

### Navigation Bar

- **Home** 🏠: View all workflows and their status
- **Create** ➕: Create new automation workflows
- **Profile** 👤: Manage user profile and preferences
- **Settings** ⚙️: Configure app settings and permissions

### Tips for Best Experience

1. **Start Simple**: Create a basic time-based notification first
2. **Test Thoroughly**: Enable test mode to verify triggers work
3. **Grant All Permissions**: For full functionality
4. **Check Notifications**: App sends confirmation when workflows execute
5. **Battery Optimization**: Disable battery optimization for AutoFlow for reliable background execution

---

## 📊 Professional Assessment

### Code Quality: **8/10**

**Strengths** ✅:
- Clean architecture with clear separation of concerns
- Consistent naming conventions
- Comprehensive error handling
- Well-structured package organization
- Good use of modern Android components
- Proper null safety handling
- Documented constants and utility classes

**Areas for Improvement** 🔧:
- Mixed Java/Kotlin codebase (should migrate fully to Kotlin)
- Some classes could be broken into smaller components
- Missing comprehensive unit test coverage
- Could benefit from more inline documentation
- Some duplicate code in UI components

### Efficiency: **7.5/10**

**Strengths** ✅:
- Efficient database operations with Room
- Background processing with WorkManager
- Proper use of coroutines for async operations
- Optimized UI rendering with Compose
- Resource management for location and BLE services

**Areas for Improvement** 🔧:
- Could implement caching for frequently accessed data
- Some potential memory leaks in manager classes
- Background service optimization needed
- Better battery efficiency with job batching
- Network request optimization

### Design & UX: **8.5/10**

**Strengths** ✅:
- Modern Material 3 design
- Intuitive workflow creation wizard
- Clear visual hierarchy
- Good use of colors and icons
- Smooth animations and transitions
- Responsive layout design
- Empty states with helpful guidance

**Areas for Improvement** 🔧:
- Could add onboarding tutorial
- More accessibility features needed
- Better error messages for users
- Progress indicators for long operations
- Landscape mode optimization

### Professionalism: **8/10**

**Strengths** ✅:
- Production-ready code structure
- Proper error handling
- Security-conscious permission handling
- Follows Android best practices
- Scalable architecture
- Version control usage

**Areas for Improvement** 🔧:
- Missing comprehensive documentation
- No CI/CD pipeline
- Limited test coverage
- Could use code quality tools (Detekt, Lint)
- Missing contribution guidelines

### Overall Score: **8/10**

**Summary**: AutoFlow is a well-architected, feature-rich Android automation app with solid fundamentals. The codebase demonstrates good understanding of Android development patterns and modern practices. With some refinements in testing, documentation, and code consolidation, it could easily become a professional-grade application.

---

## 🎯 Feasible Features for Implementation

### High Priority (Easy to Implement)

1. **Enhanced Notifications**
   - Notification history log
   - Custom notification sounds
   - Notification actions (quick reply, dismiss, etc.)
   - Grouped notifications

2. **Workflow Templates**
   - Pre-built automation templates
   - Import/export workflows
   - Share workflows with others
   - Template marketplace

3. **UI Enhancements**
   - Dark mode improvements
   - Workflow search and filter
   - Sort by name, date, status
   - Workflow categories/tags
   - Onboarding tutorial

4. **Quick Actions**
   - Home screen widgets
   - Quick settings tiles
   - Workflow shortcuts
   - Voice assistant integration

5. **Testing & Debugging**
   - Test trigger button
   - Execution history log
   - Debug mode with detailed logs
   - Dry run mode

### Medium Priority (Moderate Effort)

6. **Advanced Triggers**
   - Battery level triggers
   - Charging state triggers
   - Network type (4G/5G/WiFi)
   - Calendar event triggers
   - Weather-based triggers
   - NFC tag triggers

7. **Advanced Actions**
   - Launch specific apps
   - Send SMS/Email
   - Open URLs
   - Control media playback
   - Take photos/screenshots
   - Flashlight control

8. **Conditional Logic**
   - Multiple triggers (AND/OR conditions)
   - If-else action chains
   - Delay between actions
   - Repeat actions
   - Action sequences

9. **Cloud Integration**
   - Cloud backup of workflows
   - Sync across devices
   - IFTTT integration
   - Zapier integration
   - Google Assistant integration

10. **Analytics & Insights**
    - Workflow execution statistics
    - Battery usage analysis
    - Most used workflows
    - Trigger success rate
    - Time-based usage patterns

### Low Priority (Complex Implementation)

11. **AI/ML Features**
    - Smart trigger suggestions based on usage
    - Predictive automation
    - Anomaly detection
    - Natural language workflow creation

12. **Advanced Scripting**
    - Python script support
    - Shell script execution
    - Custom function library
    - Script marketplace
    - Visual flow builder

13. **Home Automation**
    - Smart home device control
    - IoT device integration
    - MQTT support
    - Home Assistant integration

14. **Security Features**
    - Workflow password protection
    - Biometric authentication
    - Encrypted workflow storage
    - Secure credential management

15. **Multi-device Support**
    - Tablet-optimized UI
    - Wear OS companion app
    - Android TV support
    - Cross-device trigger sharing

### Quality of Life Improvements

16. **Performance**
    - Reduce app size
    - Optimize battery usage
    - Faster workflow execution
    - Background service optimization

17. **Accessibility**
    - Screen reader support
    - High contrast themes
    - Large text support
    - Voice control

18. **Localization**
    - Multi-language support
    - RTL language support
    - Cultural date/time formats

19. **Documentation**
    - In-app help system
    - Video tutorials
    - FAQ section
    - Community forum

20. **Developer Features**
    - API for third-party integration
    - Plugin system
    - Custom trigger/action SDK
    - Webhook support

---

## 🗂️ Project Statistics

- **Total Lines of Code**: ~9,361 lines
- **Languages**: Kotlin (45%), Java (55%)
- **Files**: 47 source files
- **Screens**: 6 main screens (Dashboard, Home, TaskCreation, Profile, Settings, BlockActivity)
- **Database Tables**: 1 (workflows)
- **Supported Triggers**: 4 types (Time, Location, WiFi, BLE)
- **Supported Actions**: 5+ types
- **Permissions**: 20+ required permissions

---

## 🔧 Development Roadmap

### Phase 1: Stabilization (Current)
- ✅ Core workflow CRUD operations
- ✅ Basic trigger types
- ✅ Basic action types
- ✅ Material 3 UI
- 🔄 Bug fixes and stability improvements

### Phase 2: Enhancement (Next)
- 📋 Comprehensive test coverage
- 📋 Full Kotlin migration
- 📋 Improved documentation
- 📋 Performance optimization
- 📋 Advanced trigger conditions

### Phase 3: Expansion
- 📋 Cloud sync
- 📋 Workflow templates
- 📋 Advanced actions
- 📋 Analytics dashboard
- 📋 Third-party integrations

### Phase 4: Innovation
- 📋 AI-powered suggestions
- 📋 Voice control
- 📋 Smart home integration
- 📋 Multi-device support

---

## 🤝 Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

This project is currently unlicensed. Please contact the repository owner for licensing information.

---

## 👥 Authors

- **l1kiiiiii** - Initial development and maintenance

---

## 🙏 Acknowledgments

- Android Jetpack team for excellent libraries
- Material Design team for beautiful components
- Open source community for invaluable tools and libraries

---

## 📞 Support

For issues, questions, or suggestions:
- Open an issue on GitHub
- Check the FAQ (coming soon)
- Contact: [Create an issue on GitHub]

---

## 🔮 Future Vision

AutoFlow aims to become the **most powerful and user-friendly automation app** for Android, enabling users to create complex automations without writing code, while also providing advanced scripting capabilities for power users. The goal is to save users time, improve productivity, and make their Android experience truly personalized and automated.

---

**Last Updated**: 2024
**Version**: 1.0 (l1kiii-1 branch)
**Status**: Active Development 🚀
