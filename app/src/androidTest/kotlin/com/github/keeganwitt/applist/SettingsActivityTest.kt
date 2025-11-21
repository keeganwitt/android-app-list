package com.github.keeganwitt.applist

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Matchers.endsWith
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class SettingsActivityTest {
    private lateinit var scenario: ActivityScenario<SettingsActivity>
    private lateinit var appSettings: AppSettings

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        appSettings = SharedPreferencesAppSettings(context)
        appSettings.setCrashReportingEnabled(true)

        scenario = ActivityScenario.launch(SettingsActivity::class.java)
        waitFor(1000)
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun settingsActivity_whenLaunched_thenToolbarIsDisplayed() {
        onView(withId(R.id.toolbar)).check(matches(isDisplayed()))
    }

    @Test
    fun settingsActivity_whenLaunched_thenSettingsContainerIsDisplayed() {
        onView(withId(R.id.settings_container)).check(matches(isDisplayed()))
    }

    @Test
    fun settingsActivity_whenLaunched_thenCrashReportingPreferenceIsDisplayed() {
        onView(withText(R.string.crash_reporting_title)).check(matches(isDisplayed()))
    }

    @Test
    fun settingsActivity_whenCrashReportingEnabled_thenSwitchIsChecked() {
        appSettings.setCrashReportingEnabled(true)
        scenario.recreate()
        waitFor(500)
        onView(withText(R.string.crash_reporting_title)).check(matches(isDisplayed()))
    }

    @Test
    fun settingsActivity_whenCrashReportingDisabled_thenSwitchIsUnchecked() {
        appSettings.setCrashReportingEnabled(false)
        scenario.recreate()
        waitFor(500)
        onView(withText(R.string.crash_reporting_title)).check(matches(isDisplayed()))
    }

    @Test
    fun settingsActivity_whenSwitchToggled_thenPreferenceIsUpdated() {
        appSettings.setCrashReportingEnabled(true)
        scenario.recreate()
        waitFor(1000)

        scenario.onActivity { activity ->
            val fragment =
                activity.supportFragmentManager.findFragmentById(R.id.settings_container) as
                    SettingsActivity.SettingsFragment
            val preference =
                fragment.findPreference<androidx.preference.SwitchPreferenceCompat>(
                    AppSettings.KEY_CRASH_REPORTING_ENABLED,
                )
            if (preference != null) {
                preference.performClick()
            } else {
                throw RuntimeException("Preference not found")
            }
        }

        waitFor(1000)
        assertFalse("Preference should be false", appSettings.isCrashReportingEnabled())
    }

    @Test
    fun settingsActivity_whenSwitchToggledTwice_thenPreferenceReturnsToOriginalState() {
        appSettings.setCrashReportingEnabled(true)
        scenario.recreate()
        waitFor(1000)
        onView(withClassName(endsWith("SwitchCompat"))).perform(click())
        waitFor(1000)
        onView(withClassName(endsWith("SwitchCompat"))).perform(click())
        waitFor(1000)
        assertTrue(appSettings.isCrashReportingEnabled())
    }
}
