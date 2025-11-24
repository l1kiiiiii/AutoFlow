# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ============================================================
# Rhino JavaScript Engine
# ============================================================

# Keep Rhino runtime classes (exclude tools/debugger)
-keep class org.mozilla.javascript.** { *; }

# Don't warn about missing desktop Java classes (not on Android)
-dontwarn java.beans.**
-dontwarn java.awt.**
-dontwarn javax.swing.**
-dontwarn org.mozilla.javascript.xml.**
-dontwarn org.mozilla.javascript.xmlimpl.**
-dontwarn org.mozilla.javascript.tools.**

# ============================================================
# AutoFlow Script Security
# ============================================================

# Keep script security package
-keep class com.example.autoflow.script.** { *; }

# Keep Action model
-keep class com.example.autoflow.model.Action { *; }

# Keep all model classes
-keep class com.example.autoflow.model.** { *; }

# Keep data entities
-keep class com.example.autoflow.data.** { *; }

# ============================================================
# General Android
# ============================================================

-keepattributes *Annotation*
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
