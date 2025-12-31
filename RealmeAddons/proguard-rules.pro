# Copyright (C) 2025 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0

# Keep all classes in our package
-keep class org.lineageos.settings.** { *; }

# Keep classes with constructors used by Android framework
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

# Keep preference classes
-keep public class * extends androidx.preference.Preference {
    public <init>(android.content.Context, android.util.AttributeSet);
}

# Keep ViewModel classes
-keep public class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}

# Keep service classes
-keep public class * extends android.app.Service
-keep public class * extends android.service.quicksettings.TileService

# Keep broadcast receivers
-keep public class * extends android.content.BroadcastReceiver

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# DataStore
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}
