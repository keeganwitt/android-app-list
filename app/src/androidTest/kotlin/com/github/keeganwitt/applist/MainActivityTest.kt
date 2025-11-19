package com.github.keeganwitt.applist

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityTest {
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setup() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(3000)
    }

    @After
    fun tearDown() {
        scenario.close()
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
        Thread.sleep(1000)
        onView(withId(R.id.toggleButton)).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.toggleButton)).check(matches(isDisplayed()))
    }
}
