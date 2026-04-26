package com.github.keeganwitt.applist.utils

import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Process
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAppOpsManager

@RunWith(AndroidJUnit4::class)
@Config(application = com.github.keeganwitt.applist.TestAppListApplication::class)
class PermissionUtilsTest {
    private lateinit var context: Context
    private lateinit var shadowAppOps: ShadowAppOpsManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        shadowAppOps = Shadows.shadowOf(context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager)
    }

    @Test
    fun `hasUsageStatsPermission returns true when mode is allowed`() {
        shadowAppOps.setMode(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName, AppOpsManager.MODE_ALLOWED)
        assertTrue(PermissionUtils.hasUsageStatsPermission(context))
    }

    @Test
    fun `hasUsageStatsPermission returns false when mode is not allowed`() {
        shadowAppOps.setMode(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName, AppOpsManager.MODE_IGNORED)
        assertFalse(PermissionUtils.hasUsageStatsPermission(context))
    }

    @Test
    @Suppress("DEPRECATION")
    fun `requestUsageStatsPermission starts activity when intent resolves`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val shadowActivity = Shadows.shadowOf(activity)
        val shadowPackageManager = Shadows.shadowOf(activity.packageManager)

        val intentToResolve =
            Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                data = Uri.fromParts("package", activity.packageName, null)
            }
        val resolveInfo =
            ResolveInfo().apply {
                activityInfo =
                    android.content.pm.ActivityInfo().apply {
                        packageName = "com.android.settings"
                        name = "Settings"
                    }
            }
        @Suppress("DEPRECATION")
        shadowPackageManager.addResolveInfoForIntent(intentToResolve, resolveInfo)

        PermissionUtils.requestUsageStatsPermission(activity)

        val startedIntent = shadowActivity.nextStartedActivity
        assertNotNull(startedIntent)
        assertTrue(startedIntent?.action == android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
        assertTrue(startedIntent?.data == Uri.fromParts("package", activity.packageName, null))
    }

    @Test
    @Suppress("DEPRECATION")
    fun `requestUsageStatsPermission starts fallback activity when primary intent does not resolve`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val shadowActivity = Shadows.shadowOf(activity)
        val shadowPackageManager = Shadows.shadowOf(activity.packageManager)

        // Only fallback intent resolves
        val fallbackIntent = Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
        val resolveInfo =
            ResolveInfo().apply {
                activityInfo =
                    android.content.pm.ActivityInfo().apply {
                        packageName = "com.android.settings"
                        name = "Settings"
                    }
            }
        @Suppress("DEPRECATION")
        shadowPackageManager.addResolveInfoForIntent(fallbackIntent, resolveInfo)

        PermissionUtils.requestUsageStatsPermission(activity)

        val startedIntent = shadowActivity.nextStartedActivity
        assertNotNull(startedIntent)
        assertTrue(startedIntent?.action == android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
        assertTrue(startedIntent?.data == null)
    }

    @Test
    fun `showUsageStatsPermissionDialog shows dialog and handles confirm`() {
        var confirmed = false
        val activity = Robolectric.buildActivity(androidx.appcompat.app.AppCompatActivity::class.java).setup().get()

        PermissionUtils.showUsageStatsPermissionDialog(
            activity,
            onConfirm = { confirmed = true },
            onCancel = {},
        )
        Shadows.shadowOf(activity.mainLooper).idle()

        val dialog =
            org.robolectric.shadows.ShadowDialog
                .getLatestDialog() as androidx.appcompat.app.AlertDialog
        assertNotNull(dialog)
        assertTrue(dialog.isShowing)

        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick()
        Shadows.shadowOf(activity.mainLooper).idle()
        assertTrue(confirmed)
    }

    @Test
    fun `showUsageStatsPermissionDialog calls onCancel when negative button clicked`() {
        var cancelled = false
        val activity = Robolectric.buildActivity(androidx.appcompat.app.AppCompatActivity::class.java).setup().get()

        PermissionUtils.showUsageStatsPermissionDialog(
            activity,
            onConfirm = {},
            onCancel = { cancelled = true },
        )
        Shadows.shadowOf(activity.mainLooper).idle()

        val dialog =
            org.robolectric.shadows.ShadowDialog
                .getLatestDialog() as androidx.appcompat.app.AlertDialog
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).performClick()
        Shadows.shadowOf(activity.mainLooper).idle()
        assertTrue(cancelled)
    }

    @Test
    fun `showUsageStatsPermissionDialog calls onCancel when dialog is cancelled`() {
        var cancelled = false
        val activity = Robolectric.buildActivity(androidx.appcompat.app.AppCompatActivity::class.java).setup().get()

        PermissionUtils.showUsageStatsPermissionDialog(
            activity,
            onConfirm = {},
            onCancel = { cancelled = true },
        )
        Shadows.shadowOf(activity.mainLooper).idle()

        val dialog =
            org.robolectric.shadows.ShadowDialog
                .getLatestDialog() as androidx.appcompat.app.AlertDialog
        dialog.cancel()
        Shadows.shadowOf(activity.mainLooper).idle()
        assertTrue(cancelled)
    }
}
