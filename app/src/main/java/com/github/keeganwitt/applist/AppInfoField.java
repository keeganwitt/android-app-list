package com.github.keeganwitt.applist;

public enum AppInfoField {
    APP_NAME("App Name"),
    APK_SIZE("APK Size"),
    APP_SIZE("App Size"),
    CACHE_SIZE("Cache Size"),
    DATA_SIZE("Data Size"),
    ENABLED("Enabled"),
    EXTERNAL_CACHE_SIZE("External Cache Size"),
    FIRST_INSTALLED("First Installed"),
    GRANTED_PERMISSIONS("Granted Permissions"),
    LAST_UPDATED("Last Updated"),
    LAST_USED("Last Used"),
    MIN_SDK("Min SDK"),
    PACKAGE_MANAGER("Package Manager"),
    REQUESTED_PERMISSIONS("Requested Permissions"),
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
