package com.github.keeganwitt.applist.utils

import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
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
}
