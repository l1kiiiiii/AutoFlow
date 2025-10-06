# AutoFlow - Quick Start Guide

## 🚀 5-Minute Quick Start

This guide will get you up and running with AutoFlow in 5 minutes!

---

## For End Users (Using the App)

### Step 1: Install (1 minute)
1. Download the APK from GitHub releases
2. Enable "Install from unknown sources" in Settings
3. Install the APK
4. Open AutoFlow

### Step 2: Grant Permissions (1 minute)
When prompted, grant these permissions:
- ✅ **Notifications** (required for alerts)
- ✅ **Alarms** (required for time triggers)
- ✅ **Location** (optional, for location triggers)
- ✅ **Bluetooth** (optional, for BLE triggers)

### Step 3: Disable Battery Optimization (1 minute)
For reliable automation:
1. Settings → Battery → Battery Optimization
2. Find AutoFlow → Select "Don't optimize"

### Step 4: Create Your First Workflow (2 minutes)
1. Tap the **➕** button at bottom
2. Name it: "Test Reminder"
3. Trigger: Select **Time** → Choose **5 min**
4. Action: Select **Send Notification**
   - Title: "Test"
   - Message: "It works!"
   - Priority: Normal
5. Tap **Save Task**

### Step 5: Wait & Enjoy!
In 5 minutes, you'll get a notification. That's it! 🎉

---

## For Developers (Building the App)

### Prerequisites
- Android Studio (latest)
- JDK 21
- Android SDK 36

### Step 1: Clone (30 seconds)
```bash
git clone https://github.com/l1kiiiiii/AutoFlow.git
cd AutoFlow
git checkout l1kiii-1
```

### Step 2: Open (1 minute)
1. Open Android Studio
2. File → Open
3. Select the AutoFlow folder
4. Wait for Gradle sync

### Step 3: Build (2-3 minutes)
```bash
chmod +x gradlew
./gradlew build
```

Or in Android Studio: Build → Make Project

### Step 4: Run (1 minute)
1. Connect Android device or start emulator
2. Click Run ▶️
3. Select device
4. Wait for installation

### Step 5: Test!
App should launch on your device. Create a test workflow to verify!

---

## Common Issues & Fixes

### App Won't Install
- ✅ Enable "Unknown sources" in Settings
- ✅ Check Android version (need 12+)
- ✅ Free up storage space

### Workflows Don't Execute
- ✅ Ensure permissions granted
- ✅ Disable battery optimization
- ✅ Check workflow is enabled (toggle on)

### Build Fails
- ✅ Update Android Studio to latest
- ✅ Install JDK 21
- ✅ Run `./gradlew clean build`
- ✅ Invalidate caches: File → Invalidate Caches → Restart

### Location Triggers Don't Work
- ✅ Enable GPS/Location
- ✅ Grant background location permission
- ✅ Use larger radius (200-500m)

---

## What's Next?

### For Users
Read **USER_GUIDE.md** for:
- Detailed trigger explanations
- Advanced action types
- Script writing
- Troubleshooting

### For Developers
Read **README.md** and **ANALYSIS.md** for:
- Architecture details
- Code structure
- Contributing guidelines
- Feature roadmap

---

## Essential Commands

### Build Commands
```bash
# Clean build
./gradlew clean build

# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease

# Run tests
./gradlew test

# Install on device
./gradlew installDebug
```

### Git Commands
```bash
# Checkout branch
git checkout l1kiii-1

# Pull latest
git pull origin l1kiii-1

# Check status
git status
```

---

## File Structure Overview

```
AutoFlow/
├── app/                    # Main application code
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/       # Java/Kotlin source
│   │   │   ├── res/        # Resources (layouts, etc.)
│   │   │   └── AndroidManifest.xml
│   │   └── test/           # Tests
│   └── build.gradle.kts    # App build config
├── gradle/                 # Gradle wrapper
├── README.md              # Main documentation
├── ANALYSIS.md            # Technical analysis
├── REQUIREMENTS.md        # System requirements
├── USER_GUIDE.md          # User manual
├── SUMMARY.md             # Executive summary
├── QUICKSTART.md          # This file!
├── build.gradle.kts       # Root build config
└── settings.gradle.kts    # Project settings
```

---

## Key Features at a Glance

| Feature | Description | Difficulty |
|---------|-------------|------------|
| Time Triggers | Schedule actions | ⭐ Easy |
| Location Triggers | Geofencing | ⭐⭐ Medium |
| WiFi Triggers | Network detection | ⭐ Easy |
| BLE Triggers | Bluetooth devices | ⭐⭐ Medium |
| Notifications | Send alerts | ⭐ Easy |
| WiFi Toggle | Turn WiFi on/off | ⭐ Easy |
| Sound Mode | Change ringer | ⭐ Easy |
| Scripts | Run JavaScript | ⭐⭐⭐ Advanced |

---

## Example Workflows

### 1. Morning Reminder
- **Trigger**: Time → 7:00 AM
- **Action**: Notification → "Good morning! Start your day!"
- **Use**: Daily motivation

### 2. Silent at Work
- **Trigger**: Location → Enter work area (500m radius)
- **Action**: Set Sound Mode → Vibrate
- **Use**: Automatic silence during work hours

### 3. WiFi at Home
- **Trigger**: Location → Enter home area (200m radius)
- **Action**: Toggle WiFi → On
- **Use**: Save mobile data at home

### 4. Car Connected
- **Trigger**: BLE → Car Bluetooth MAC address
- **Action**: Notification → "Connected to car"
- **Use**: Trigger when entering car

### 5. Bedtime Routine
- **Trigger**: Time → 10:00 PM
- **Action**: Multiple via script:
  - Turn on DND
  - Turn off WiFi
  - Send notification → "Goodnight!"

---

## Documentation Quick Links

| Document | Purpose | Read Time |
|----------|---------|-----------|
| QUICKSTART.md | This file | 5 min ⭐ |
| README.md | Complete overview | 20 min |
| USER_GUIDE.md | How to use app | 30 min |
| ANALYSIS.md | Technical deep dive | 40 min |
| REQUIREMENTS.md | Setup & dependencies | 25 min |
| SUMMARY.md | Executive summary | 10 min |

**Start here**: QUICKSTART.md (you are here!)
**Then read**: README.md → USER_GUIDE.md

---

## Getting Help

### Documentation
1. Check this QUICKSTART.md
2. Read USER_GUIDE.md for detailed help
3. Check README.md for technical info

### Online
1. GitHub Issues: Report bugs
2. GitHub Discussions: Ask questions
3. Stack Overflow: Tag with 'autoflow-android'

### Common Questions

**Q: Does it work offline?**
A: Yes! Except scripts with HTTP requests.

**Q: Does it drain battery?**
A: Depends on triggers. Time triggers: minimal. Location/BLE: moderate.

**Q: Is it safe?**
A: Yes! Open source, no cloud, privacy-focused.

**Q: Can I share workflows?**
A: Not yet, but feature is planned.

**Q: Does it need root?**
A: No! Works on unrooted devices.

---

## Tips for Success

### Start Simple
✅ Create a time-based notification first
✅ Test with short intervals (5 minutes)
✅ Verify permissions are granted

### Then Progress
✅ Try location triggers near home
✅ Experiment with WiFi triggers
✅ Explore action types

### Advanced Features
✅ Learn JavaScript for scripts
✅ Combine multiple actions
✅ Create complex workflows

### Optimize
✅ Disable unused workflows
✅ Monitor battery usage
✅ Adjust trigger frequencies

---

## Success Checklist

After following this guide, you should:

**Users**:
- [ ] App installed and opens
- [ ] Permissions granted
- [ ] Battery optimization disabled
- [ ] First workflow created
- [ ] Test notification received
- [ ] Understand basic navigation

**Developers**:
- [ ] Repository cloned
- [ ] Project opens in Android Studio
- [ ] Gradle sync successful
- [ ] App builds without errors
- [ ] App runs on device/emulator
- [ ] Code structure understood

---

## Next Steps

### Week 1
- Create 2-3 simple workflows
- Test different trigger types
- Explore all action types
- Read USER_GUIDE.md

### Week 2
- Create location-based workflows
- Try BLE triggers (if you have devices)
- Experiment with scripts
- Share feedback on GitHub

### Month 1
- Build your automation suite
- Optimize for battery
- Contribute to project
- Help others in discussions

---

## Version Info

- **App Version**: 1.0
- **Branch**: l1kiii-1
- **Min Android**: 12 (API 31)
- **Target Android**: 14 (API 36)
- **Last Updated**: 2024

---

## Contact & Support

- **GitHub**: https://github.com/l1kiiiiii/AutoFlow
- **Issues**: Report bugs and request features
- **Discussions**: Ask questions and share ideas

---

**You're all set! Start automating! 🚀**

For detailed information, read:
- **README.md** (Overview)
- **USER_GUIDE.md** (How to use)
- **ANALYSIS.md** (Technical details)

Happy Automating! ⚡
