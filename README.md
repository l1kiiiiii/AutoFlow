
# AutoFlow - README Outline

## Overview
- Android app using Jetpack Compose for task automation.
- Supports triggers based on time, location, wifi, and Bluetooth.
- Executes actions such as notifications, toggling settings, and running scripts.

## Features Implemented
### Task Management
- Create, update, delete, and enable/disable automation tasks.
- Persistent storage with Room Database.
- Data layer follows MVVM architecture with Repository pattern.

### Trigger Types
- Time: schedule tasks at specified timestamps.
- Location: support for manual entry and map-based selection using Google Maps Compose.
- WiFi: react to WiFi state changes.
- Bluetooth: detect connection to specific BLE devices.

### Action Types
- Notifications with configurable title, message, and priority.
- Toggle device settings like WiFi.
- Run JavaScript-based custom scripts.

### User Interface
- Home screen showing active tasks with status and brief info.
- Task details dialog on tap for quick task overview.
- Task creation and editing screen with dynamic inputs based on trigger and action types.
- Bottom navigation for easy access to Home, Create Task, Profile, and Settings.
- Smooth navigation implemented using Jetpack Navigation Compose.

### Notifications
- Integrated notifications with channels using NotificationHelper.
- Scheduling handled with AlarmManager and permission management.

### Location Selection
- Users can select current location 

## Architecture & Technologies
- Jetpack Compose UI framework.
- Room ORM for data persistence.
- ViewModel and LiveData for state management.
- Navigation Compose for routing.
- Google Maps SDK for maps integration.
- Background task execution with AlarmManager and WorkManager.
