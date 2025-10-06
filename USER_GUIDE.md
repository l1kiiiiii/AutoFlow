# AutoFlow - User Guide

## 📖 Table of Contents
1. [Introduction](#introduction)
2. [Getting Started](#getting-started)
3. [Understanding AutoFlow](#understanding-autoflow)
4. [Creating Your First Workflow](#creating-your-first-workflow)
5. [Trigger Types Explained](#trigger-types-explained)
6. [Action Types Explained](#action-types-explained)
7. [Managing Workflows](#managing-workflows)
8. [Advanced Features](#advanced-features)
9. [Tips & Best Practices](#tips--best-practices)
10. [Troubleshooting](#troubleshooting)
11. [FAQ](#faq)

---

## Introduction

Welcome to **AutoFlow** - your personal automation assistant for Android! AutoFlow lets you automate repetitive tasks by creating smart workflows. Think of it as teaching your phone to do things automatically based on certain conditions.

### What Can AutoFlow Do?

- 🔔 Send you notifications at specific times or locations
- 📶 Turn WiFi on/off based on your location
- 🔵 Detect when you connect to Bluetooth devices
- ⏰ Schedule actions for specific times
- 📜 Run custom scripts for advanced automation

### Who Is This For?

- **Busy professionals** who want to save time
- **Tech enthusiasts** who love customization
- **Forgetful folks** who need reminders
- **Privacy-conscious users** who prefer on-device automation
- **Anyone** who wants a smarter phone experience

---

## Getting Started

### Installation

1. **Download the APK** from the GitHub releases page or build from source
2. **Enable installation from unknown sources** (if not from Play Store)
   - Go to Settings → Security → Unknown Sources → Enable
3. **Install the APK** by tapping on the downloaded file
4. **Open AutoFlow** from your app drawer

### First Launch Setup

#### Step 1: Grant Permissions

When you first open AutoFlow, you'll be asked to grant several permissions. Here's why each is needed:

- **📍 Location**: For location-based triggers (e.g., "Turn off WiFi when I leave home")
- **🔔 Notifications**: To send you alerts and show automation results
- **⏰ Alarms & Reminders**: For time-based triggers (e.g., "Send notification at 8 AM")
- **🔵 Bluetooth**: To detect Bluetooth device connections
- **📶 WiFi**: To toggle WiFi on/off as an action

**Important**: You can deny permissions for features you don't plan to use. However, the app works best with all permissions granted.

#### Step 2: Battery Optimization

For reliable automation, disable battery optimization for AutoFlow:

1. Go to **Settings → Battery → Battery Optimization**
2. Find **AutoFlow** in the list
3. Select **Don't optimize**

This ensures your automations run even when your phone is in sleep mode.

#### Step 3: Explore the Interface

The app has four main sections accessible from the bottom navigation bar:

- **🏠 Home**: View and manage all your workflows
- **➕ Create**: Create new automation workflows
- **👤 Profile**: Manage your profile and preferences
- **⚙️ Settings**: Configure app settings

---

## Understanding AutoFlow

### The Basic Concept

AutoFlow uses a simple **"If This, Then That"** logic:

```
IF [Trigger Happens] → THEN [Perform Action]
```

**Example**:
- IF "I arrive at work" (Location Trigger)
- THEN "Set phone to silent" (Action)

### Key Terms

- **Workflow**: A complete automation rule (Trigger + Action)
- **Trigger**: An event that starts the automation (e.g., time, location)
- **Action**: What happens when the trigger activates (e.g., notification, toggle WiFi)
- **Enable/Disable**: Turn workflows on or off without deleting them

---

## Creating Your First Workflow

Let's create a simple workflow: **"Send me a reminder in 5 minutes"**

### Step-by-Step Instructions

#### 1. Navigate to Create Tab

Tap the **➕ Create** button in the bottom navigation bar.

#### 2. Name Your Workflow

In the **"Workflow Name"** field, type: `Quick Reminder`

#### 3. Select Trigger Type

Under **"Select Trigger Type"**, tap the dropdown and select **Time**.

#### 4. Set Trigger Details

You'll see quick time options. Tap **5 min** to set the trigger for 5 minutes from now.

Alternatively, you can:
- Select a specific date and time
- Use other quick options (15 min, 30 min, 1 hour, etc.)

#### 5. Select Action Type

Under **"Select Action Type"**, tap the dropdown and select **Send Notification**.

#### 6. Configure Action Details

Fill in the notification details:
- **Title**: `Reminder`
- **Message**: `This is your 5-minute reminder!`
- **Priority**: Select **Normal**

#### 7. Save Your Workflow

Scroll down and tap the **Save Task** button.

#### 8. Success!

You'll be redirected to the home screen where you'll see your new workflow. After 5 minutes, you'll receive the notification!

---

## Trigger Types Explained

AutoFlow supports multiple trigger types. Here's a detailed explanation of each:

### ⏰ Time Triggers

**What it does**: Activates at a specific date and time.

**Use Cases**:
- Daily reminders
- Weekly task notifications
- One-time scheduled actions
- Morning/evening routines

**How to Configure**:
1. Select **Time** as trigger type
2. Choose from quick options or set custom date/time
3. The workflow will execute once at the specified time

**Quick Time Options**:
- **5 min**: 5 minutes from now
- **15 min**: 15 minutes from now
- **30 min**: 30 minutes from now
- **1 hour**: 1 hour from now
- **2 hours**: 2 hours from now
- **1 day**: 24 hours from now

**Tips**:
- Use for one-time reminders or scheduled tasks
- For recurring tasks, you'll need to recreate the workflow (recurring feature coming soon)
- Make sure "Alarms & reminders" permission is granted

**Example Workflows**:
- Morning motivation: "At 7 AM, send notification: 'Good morning! Start your day strong!'"
- Medication reminder: "At 2 PM, send notification: 'Time to take your medicine'"
- End of workday: "At 5 PM, turn on WiFi and send notification: 'Time to head home!'"

---

### 📍 Location Triggers

**What it does**: Activates when you enter or exit a specific location.

**Use Cases**:
- Work/home automation
- Location-based reminders
- Context-aware settings
- Place-specific notifications

**How to Configure**:
1. Select **Location** as trigger type
2. Choose how to set location:
   - **Use Current Location**: Uses your GPS coordinates
   - **Enter Manually**: Type latitude and longitude
   - **Select on Map**: Pick a location on Google Maps
3. Set the radius (20m to 1000m)
4. Choose entry/exit behavior

**Location Options**:
- **Radius**: How close you need to be (e.g., 100 meters)
- **Entry**: Trigger when you enter the area
- **Exit**: Trigger when you leave the area
- **Both**: Trigger on both entry and exit

**Tips**:
- Use larger radius (200-500m) for reliable detection
- Smaller radius drains battery faster
- Ensure "Location" permission is granted
- Works in background (battery optimization must be disabled)

**Example Workflows**:
- Arriving at work: "When I enter work area, set phone to vibrate"
- Leaving home: "When I exit home area, turn off WiFi"
- Arriving home: "When I enter home area, turn on WiFi"
- Leaving gym: "When I exit gym area, send notification: 'Great workout!'"

---

### 📶 WiFi Triggers

**What it does**: Activates when WiFi state changes or you connect to a specific network.

**Use Cases**:
- Network-based automation
- Home/office detection
- Data saving
- Security reminders

**How to Configure**:
1. Select **WiFi** as trigger type
2. Configure WiFi settings:
   - **SSID**: Specific WiFi network name (optional)
   - **State**: Connected/Disconnected

**Tips**:
- Leave SSID empty to trigger on any WiFi connection/disconnection
- Specify SSID for home/office specific automation
- Ensure "WiFi" permission is granted

**Example Workflows**:
- Home WiFi connected: "When connected to 'Home WiFi', turn off mobile data"
- Office WiFi connected: "When connected to 'Office WiFi', set phone to silent"
- WiFi disconnected: "When WiFi disconnects, send notification: 'Remember to turn on mobile data'"

---

### 🔵 Bluetooth (BLE) Triggers

**What it does**: Activates when a specific Bluetooth device connects.

**Use Cases**:
- Car integration
- Wearable device detection
- Headphone automation
- Smart home triggers

**How to Configure**:
1. Select **BLE** (Bluetooth) as trigger type
2. Enter the Bluetooth device details:
   - **Device Name**: Name of your device
   - **MAC Address**: Device's unique identifier (XX:XX:XX:XX:XX:XX)

**Finding Device MAC Address**:
1. Go to Android Settings → Bluetooth
2. Tap the ⓘ icon next to your paired device
3. Look for "Device address" or "MAC address"

**Tips**:
- BLE scanning can drain battery
- Ensure Bluetooth is enabled
- Make sure "Bluetooth" permission is granted
- Works best with devices that support BLE (Bluetooth 4.0+)

**Example Workflows**:
- Car Bluetooth: "When connected to car Bluetooth, open Google Maps"
- Headphones: "When connected to headphones, increase media volume"
- Smartwatch: "When connected to smartwatch, enable fitness tracking mode"

---

## Action Types Explained

When a trigger activates, AutoFlow performs one or more actions. Here are the available actions:

### 🔔 Send Notification

**What it does**: Displays a notification on your device.

**Configuration**:
- **Title**: Notification headline (e.g., "Reminder")
- **Message**: Notification content (e.g., "Time for lunch!")
- **Priority**: How urgent the notification is

**Priority Levels**:
- **Low**: Silent notification, no sound or vibration
- **Normal**: Standard notification with sound
- **High**: Makes sound and appears as heads-up notification
- **Max**: Most urgent, interrupts user

**Tips**:
- Keep titles short (under 50 characters)
- Make messages clear and actionable
- Use High/Max priority sparingly to avoid notification fatigue

**Example Uses**:
- Reminders and alerts
- Workflow execution confirmations
- Status updates
- Motivational messages

---

### 📶 Toggle WiFi

**What it does**: Turns WiFi on or off.

**Configuration**:
- **State**: On or Off

**Use Cases**:
- Save battery by turning off WiFi when not needed
- Automatically enable WiFi when arriving home
- Security: Disable WiFi in untrusted locations

**Tips**:
- Some Android versions restrict WiFi toggling
- Ensure proper permissions are granted
- Consider using Airplane mode for complete connectivity control

**Example Uses**:
- "When leaving home, turn WiFi off"
- "When arriving at office, turn WiFi on"
- "At night (10 PM), turn WiFi off to save battery"

---

### 🔊 Set Sound Mode

**What it does**: Changes your phone's ringer mode.

**Configuration**:
- **Mode**: Ring, Vibrate, Silent, DND

**Sound Modes**:
- **Ring**: Normal mode with ringtone
- **Vibrate**: Vibration only, no sound
- **Silent**: No sound or vibration
- **DND** (Do Not Disturb): Various DND modes

**DND Options**:
- **Total Silence**: No interruptions
- **Priority Only**: Only priority contacts/apps
- **Alarms Only**: Only alarms can make sound

**Tips**:
- DND mode requires "Do Not Disturb access" permission
- Use for automatic silence during meetings or sleep
- Consider creating multiple workflows for different times

**Example Uses**:
- "When arriving at work, set to vibrate"
- "At night (11 PM), enable DND mode"
- "When leaving work, set to ring mode"
- "During weekends, keep vibrate mode"

---

### 📜 Run Script

**What it does**: Executes custom JavaScript code for advanced automation.

**Configuration**:
- **Script Code**: JavaScript code to execute

**Available Functions**:
```javascript
// Log messages to console (for debugging)
log("Hello, World!");

// Send a notification
notify("Title", "Message");

// Make HTTP GET request
httpGet("https://api.example.com/data");

// Access Android context (advanced)
androidContext.getSystemService("...");
```

**Tips**:
- Test scripts before using in important workflows
- Use `log()` for debugging
- Be careful with network requests (data usage)
- Scripts run with app permissions

**Example Scripts**:

**Simple Logging**:
```javascript
log("Workflow executed at: " + new Date().toString());
```

**Weather Notification** (requires API):
```javascript
var weather = httpGet("https://api.weather.com/current");
notify("Weather Update", weather);
```

**Complex Logic**:
```javascript
var hour = new Date().getHours();
if (hour < 12) {
    notify("Good Morning", "Have a great day!");
} else if (hour < 18) {
    notify("Good Afternoon", "Keep up the good work!");
} else {
    notify("Good Evening", "Time to relax!");
}
```

**Caution**: Script feature is powerful but can be complex. Start with simple scripts and gradually increase complexity.

---

## Managing Workflows

### Viewing Workflows

On the **Home** screen, you'll see all your workflows displayed as cards. Each card shows:
- **Workflow Name**: The name you gave it
- **Icon**: Visual indicator of trigger type
- **Description**: Brief summary of trigger and action
- **Toggle Switch**: Enable/disable the workflow
- **Status**: Whether it's enabled or disabled

### Enabling/Disabling Workflows

To temporarily stop a workflow without deleting it:

1. Locate the workflow on the Home screen
2. Tap the **toggle switch** on the right side
3. Green = Enabled, Gray = Disabled

**When Disabled**:
- The workflow won't execute
- Triggers are not monitored
- No battery or resource usage

### Editing Workflows

To modify an existing workflow:

1. Locate the workflow on the Home screen
2. Tap the **three-dot menu** (⋮) on the card
3. Select **Edit**
4. Make your changes
5. Tap **Save Task**

**What You Can Edit**:
- Workflow name
- Trigger type and configuration
- Action type and configuration

### Deleting Workflows

To permanently remove a workflow:

1. Locate the workflow on the Home screen
2. Tap the **three-dot menu** (⋮) on the card
3. Select **Delete**
4. Confirm deletion in the popup dialog

**Important**: Deletion is permanent and cannot be undone. Consider disabling instead if you might want to use the workflow later.

### Organizing Workflows

**Current Limitations**:
- No folders or categories yet
- No search functionality
- No sorting options

**Coming Soon**:
- Workflow categories/tags
- Search and filter
- Sort by name, date, or status
- Favorite/pin workflows

---

## Advanced Features

### Script Execution

For users comfortable with JavaScript, the script action offers unlimited automation possibilities:

**Capabilities**:
- Make HTTP API calls
- Implement complex logic (if/else, loops)
- Access date/time functions
- Integrate with web services

**Security Note**: Scripts run with the same permissions as AutoFlow. Be cautious with scripts from untrusted sources.

### Geofencing

Location triggers use Android's Geofencing API for battery-efficient location monitoring:

**How It Works**:
- Define a circular area (center + radius)
- Android monitors your location
- Triggers when you cross the boundary
- Minimal battery impact (compared to constant GPS)

**Best Practices**:
- Use reasonable radius (100-500m)
- Don't create too many geofences (limit: 100)
- Test in real-world conditions

### Background Execution

AutoFlow uses multiple Android APIs to ensure reliable background execution:

- **AlarmManager**: For precise time-based triggers
- **WorkManager**: For reliable background tasks
- **Geofencing API**: For location monitoring
- **BLE Scanner**: For Bluetooth device detection

**Battery Impact**:
- Time triggers: Minimal (only when alarm fires)
- Location triggers: Low to medium (depends on radius)
- BLE triggers: Medium (requires periodic scanning)
- WiFi triggers: Minimal (uses system callbacks)

---

## Tips & Best Practices

### For Beginners

1. **Start Simple**: Create basic time-based notifications first
2. **Test Thoroughly**: Create test workflows to understand behavior
3. **One at a Time**: Don't create too many workflows initially
4. **Read Descriptions**: Card descriptions help you remember what each workflow does
5. **Use Descriptive Names**: "Morning Alarm" is better than "Workflow 1"

### For Power Users

1. **Combine Triggers**: Use scripts to implement complex trigger logic
2. **Chain Actions**: Use scripts to perform multiple actions
3. **API Integration**: Connect to web services via HTTP requests
4. **Error Handling**: Add try-catch in scripts for reliability
5. **Performance**: Disable unused workflows to save battery

### Battery Optimization

1. **Location Triggers**: Use coarse location when possible
2. **BLE Scanning**: Limit number of BLE workflows
3. **Update Intervals**: Increase intervals for non-critical workflows
4. **Disable Unused**: Turn off workflows you don't actively use
5. **Test Battery Usage**: Monitor battery stats (Settings → Battery)

### Privacy & Security

1. **Permissions**: Only grant permissions you need
2. **Scripts**: Review scripts before running
3. **Location Data**: Stored locally on device
4. **No Cloud**: All data stays on your device
5. **Review Regularly**: Check active workflows periodically

---

## Troubleshooting

### Workflow Not Executing

**Problem**: Workflow is enabled but doesn't trigger.

**Solutions**:
1. **Check Permissions**: Ensure all required permissions are granted
2. **Battery Optimization**: Disable battery optimization for AutoFlow
3. **Check Time**: For time triggers, verify the time hasn't passed
4. **Location Accuracy**: For location triggers, ensure GPS is enabled
5. **Bluetooth**: For BLE triggers, ensure Bluetooth is enabled
6. **Review Logs**: Check Android system logs for errors

### Notifications Not Appearing

**Problem**: Notification action doesn't show notifications.

**Solutions**:
1. **Notification Permission**: Ensure POST_NOTIFICATIONS is granted (Android 13+)
2. **DND Mode**: Check if Do Not Disturb is enabled
3. **Notification Settings**: Verify AutoFlow notifications aren't blocked
4. **Channel Settings**: Check notification channel settings in system settings
5. **Priority**: Try using High priority for important notifications

### Battery Drain

**Problem**: AutoFlow is using too much battery.

**Solutions**:
1. **Disable Unused Workflows**: Turn off workflows you don't need
2. **Reduce Location Accuracy**: Use coarse location instead of fine
3. **Increase Radius**: Larger radius = fewer location updates
4. **Limit BLE Scans**: Reduce number of BLE workflows
5. **Check Background Apps**: Ensure other apps aren't interfering

### Location Triggers Not Working

**Problem**: Location-based workflows don't trigger.

**Solutions**:
1. **Enable GPS**: Turn on location services
2. **High Accuracy Mode**: Use "High accuracy" in location settings
3. **Increase Radius**: Try a larger detection radius
4. **Background Location**: Ensure background location permission is granted
5. **Test Location**: Verify GPS is working in Google Maps
6. **Battery Optimization**: Disable for AutoFlow

### BLE Device Not Detected

**Problem**: Bluetooth trigger doesn't detect device.

**Solutions**:
1. **Enable Bluetooth**: Turn on Bluetooth
2. **Pair Device**: Ensure device is paired in Android settings
3. **Correct MAC Address**: Verify MAC address is correct
4. **BLE Support**: Ensure device supports Bluetooth 4.0+
5. **Distance**: Keep device close during testing
6. **Permissions**: Check Bluetooth permissions

### Script Errors

**Problem**: Script action fails or produces errors.

**Solutions**:
1. **Syntax Check**: Verify JavaScript syntax is correct
2. **Test Incrementally**: Add code gradually and test
3. **Use log()**: Add logging to debug
4. **Check Permissions**: Ensure script has needed permissions
5. **Network Access**: Verify internet connection for HTTP requests
6. **Exception Handling**: Add try-catch blocks

### App Crashes

**Problem**: AutoFlow crashes or force closes.

**Solutions**:
1. **Restart App**: Close and reopen AutoFlow
2. **Clear Cache**: Settings → Apps → AutoFlow → Clear Cache
3. **Update Android**: Ensure Android is up to date
4. **Reinstall**: Uninstall and reinstall AutoFlow
5. **Report Bug**: Create an issue on GitHub with logs

---

## FAQ

### General Questions

**Q: Is AutoFlow free?**
A: Yes, AutoFlow is free and open-source.

**Q: Does AutoFlow collect my data?**
A: No, all data stays on your device. No data is sent to external servers (except when you use script HTTP requests).

**Q: Does AutoFlow require internet?**
A: No, except for map tile downloads and script HTTP requests.

**Q: Can I use AutoFlow without Google Play Services?**
A: Partially. Maps won't work, but other features will.

**Q: What's the minimum Android version?**
A: Android 12 (API 31) or higher.

### Workflow Questions

**Q: How many workflows can I create?**
A: Theoretically unlimited, but performance may degrade with many active workflows.

**Q: Can I have recurring workflows?**
A: Not directly yet. You need to recreate time-based workflows after they execute. Recurring feature is planned.

**Q: Can I export/import workflows?**
A: Not yet. This feature is planned for future releases.

**Q: Can I share workflows with friends?**
A: Not yet. Workflow sharing is planned for future releases.

**Q: Can workflows run when phone is locked?**
A: Yes, if battery optimization is disabled for AutoFlow.

### Trigger Questions

**Q: How accurate are location triggers?**
A: Typically 20-100 meters accuracy, depending on GPS conditions.

**Q: Do triggers work in airplane mode?**
A: Only time triggers work in airplane mode. Location, WiFi, and BLE triggers require respective radios to be enabled.

**Q: Can I combine multiple triggers?**
A: Not directly in UI. Use scripts for complex trigger logic (AND/OR conditions).

**Q: How often are triggers checked?**
A: Varies by type. Time triggers are precise, location triggers use geofencing (efficient), BLE triggers scan periodically.

### Action Questions

**Q: Can I perform multiple actions?**
A: Not directly in UI. Use scripts to perform multiple actions sequentially.

**Q: Can actions open apps?**
A: Not yet. App launching action is planned for future releases.

**Q: Can actions control volume?**
A: Partially. Sound mode action can change ringer mode. Full volume control is planned.

**Q: Can actions send SMS?**
A: Not yet. SMS action is planned but requires careful permission handling.

### Technical Questions

**Q: Does AutoFlow use root access?**
A: No, AutoFlow doesn't require root.

**Q: What's the battery impact?**
A: Varies based on active workflows. Time triggers: minimal. Location triggers: low to medium. BLE triggers: medium.

**Q: Does AutoFlow work with tasker?**
A: No direct integration yet. This could be added in future.

**Q: Can I see execution history?**
A: Not yet. Execution log/history is planned for future releases.

**Q: Is there a desktop version?**
A: No, AutoFlow is Android-only.

### Troubleshooting Questions

**Q: Why isn't my workflow executing?**
A: Check permissions, battery optimization, and that the workflow is enabled.

**Q: Why is AutoFlow using so much battery?**
A: Likely due to location or BLE triggers. Try disabling unnecessary workflows.

**Q: The app crashes on my device. What should I do?**
A: Try clearing cache, reinstalling, or report the bug on GitHub.

**Q: Location triggers are unreliable. Why?**
A: GPS accuracy varies. Try increasing radius, enabling high-accuracy mode, and disabling battery optimization.

---

## Getting Help

### Support Channels

1. **GitHub Issues**: Report bugs and request features
   - https://github.com/l1kiiiiii/AutoFlow/issues

2. **GitHub Discussions**: Ask questions and get help
   - https://github.com/l1kiiiiii/AutoFlow/discussions

3. **Documentation**: Read technical documentation
   - README.md
   - ANALYSIS.md
   - REQUIREMENTS.md

### Before Asking for Help

1. Check this user guide
2. Search existing GitHub issues
3. Verify permissions are granted
4. Test with a simple workflow
5. Check Android system logs

### Reporting Bugs

When reporting bugs, include:
- Android version
- Device model
- AutoFlow version
- Steps to reproduce
- Error messages or logs
- Screenshots (if applicable)

---

## Conclusion

Thank you for using AutoFlow! This guide should help you get started with automation. Remember:

- **Start simple** and gradually create more complex workflows
- **Test thoroughly** before relying on critical automations
- **Monitor battery** usage and adjust as needed
- **Report bugs** to help improve the app
- **Share feedback** to guide future development

Happy automating! 🚀

---

**Last Updated**: 2024
**Version**: 1.0
**For**: AutoFlow (l1kiii-1 branch)

**Questions or Feedback**: Create an issue on GitHub or start a discussion!
