package com.github.keeganwitt.applist;

import android.content.pm.ApplicationInfo;

import java.util.Date;

public class AppInfo {
    @SuppressWarnings({"FieldMayBeFinal", "CanBeFinal"})
    private ApplicationInfo applicationInfo;
    private AppInfoField appInfoField;
    private Date lastUsed;

    public AppInfo(ApplicationInfo applicationInfo, AppInfoField appInfoField) {
        this.applicationInfo = applicationInfo;
        this.appInfoField = appInfoField;
    }

    public ApplicationInfo getApplicationInfo() {
        return applicationInfo;
    }

    public AppInfoField getAppInfoField() {
        return appInfoField;
    }

    @SuppressWarnings("unused")
    public Date getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(Date lastUsed) {
        this.lastUsed = lastUsed;
    }
}
