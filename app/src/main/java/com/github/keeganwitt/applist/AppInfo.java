package com.github.keeganwitt.applist;

import android.content.pm.ApplicationInfo;

import java.util.Date;

public class AppInfo {
    private ApplicationInfo applicationInfo;
    private AppInfoField appInfoField;
    private Date lastUsed;

    public AppInfo(ApplicationInfo applicationInfo) {
        this.applicationInfo = applicationInfo;
    }

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

    public void setAppInfoField(AppInfoField appInfoField) {
        this.appInfoField = appInfoField;
    }

    public Date getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(Date lastUsed) {
        this.lastUsed = lastUsed;
    }
}
