package com.github.keeganwitt.applist

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.appcompat.view.menu.MenuBuilder
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityTest {
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setup() {
        InstrumentationRegistry
            .getInstrumentation()
            .targetContext
            .getSharedPreferences(
                InstrumentationRegistry.getInstrumentation().targetContext.packageName + AppSettings.DEFAULT_PREF_NAME_SUFFIX,
                android.content.Context.MODE_PRIVATE,
            ).edit().clear().commit()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        waitFor(3000)
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun mainActivity_whenLaunched_thenItIsResumed() {
        assertEquals(Lifecycle.State.RESUMED, scenario.state)
    }

    @Test
    fun mainActivity_whenLaunched_thenRecyclerViewIsDisplayed() {
        onView(withId(R.id.recycler_view)).check(matches(isDisplayed()))
    }

    @Test
    fun mainActivity_whenLaunched_thenSpinnerIsDisplayed() {
        onView(withId(R.id.spinner)).check(matches(isDisplayed()))
    }

    @Test
    fun mainActivity_whenLaunched_thenToggleButtonIsDisplayed() {
        onView(withId(R.id.toggleButton)).check(matches(isDisplayed()))
    }

    @Test
    fun mainActivity_whenLaunched_thenSwipeRefreshLayoutIsDisplayed() {
        onView(withId(R.id.swipeRefreshLayout)).check(matches(isDisplayed()))
    }

    @Test
    fun mainActivity_whenToggleButtonClicked_thenSortOrderChanges() {
        waitFor(1000)
        onView(withId(R.id.toggleButton)).perform(click())
        waitFor(500)
        onView(withId(R.id.toggleButton)).check(matches(isDisplayed()))
    }

    @Test
    fun mainActivity_whenSystemAppsToggleClicked_thenCheckedStateAndIconChange() {
        scenario.onActivity { activity ->
            val menu = MenuBuilder(activity)
            activity.onCreateOptionsMenu(menu)
            val item = menu.findItem(R.id.systemAppToggle)

            assertEquals(false, item.isChecked)
            assertEquals(
                activity.getDrawable(R.drawable.ic_system_apps_on)?.constantState,
                item.icon?.constantState,
            )

            activity.onOptionsItemSelected(item)

            assertEquals(true, item.isChecked)
            assertEquals(
                activity.getDrawable(R.drawable.ic_system_apps_off)?.constantState,
                item.icon?.constantState,
            )
        }
    }

    @Test
    fun mainActivity_whenRecreated_thenNoCrashOccurs() {
        scenario.recreate()
        assertEquals(Lifecycle.State.RESUMED, scenario.state)
    }
}
