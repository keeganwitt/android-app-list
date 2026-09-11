package com.github.keeganwitt.applist

import androidx.preference.ListPreference
import androidx.preference.PreferenceDialogFragmentCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsActivityRecreationTest {
    @Test
    fun `recreation with theme dialog open restores existing fragments`() {
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val settingsFragment =
                    activity.supportFragmentManager.findFragmentById(R.id.settings_container)
                        as SettingsActivity.SettingsFragment
                settingsFragment.findPreference<ListPreference>(AppSettings.KEY_THEME_MODE)?.performClick()
                activity.supportFragmentManager.executePendingTransactions()
            }

            scenario.recreate()
            scenario.recreate()

            scenario.onActivity { activity ->
                val fragments = activity.supportFragmentManager.fragments
                assertEquals(1, fragments.filterIsInstance<SettingsActivity.SettingsFragment>().size)
                assertEquals(1, fragments.filterIsInstance<PreferenceDialogFragmentCompat>().size)
            }
        }
    }
}
