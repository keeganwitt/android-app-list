package com.github.keeganwitt.applist;

public enum AppInfoField {
    APK_SIZE("APK Size"),
    APP_SIZE("App Size"),
    CACHE_SIZE("Cache Size"),
    DATA_SIZE("Data Size"),
    ENABLED("Enabled"),
    EXTERNAL_CACHE_SIZE("External Cache Size"),
    FIRST_INSTALLED("First Installed"),
    LAST_UPDATED("Last Updated"),
    MIN_SDK("Min SDK"),
    PACKAGE_MANAGER("Package Manager"),
    PERMISSIONS("Permissions"),
    TARGET_SDK("Target SDK"),
    TOTAL_SIZE("Total Size"),
    VERSION("Version");

    private final String displayName;

    AppInfoField(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
