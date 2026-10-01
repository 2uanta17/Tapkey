# ModernTiles Proguard Rules

# Keep Quick Settings TileService implementations and lifecycle methods
-keep class * extends android.service.quicksettings.TileService {
    public *;
}

# Keep AccessibilityService implementations and methods
-keep class * extends android.accessibilityservice.AccessibilityService {
    public *;
}

# Strip Log.d and Log.v in production/release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
