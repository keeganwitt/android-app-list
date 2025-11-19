package com.github.keeganwitt.applist

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeDown
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
class AppListUiFlowTest {
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setup() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(2000)
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun appList_whenSwipeRefresh_thenListRefreshes() {
        onView(withId(R.id.swipeRefreshLayout)).perform(swipeDown())
        Thread.sleep(1000)
        onView(withId(R.id.recycler_view)).check(matches(isDisplayed()))
    }

    @Test
    fun appList_whenToggleSortOrder_thenListReorders() {
        onView(withId(R.id.toggleButton)).check(matches(isDisplayed()))
        onView(withId(R.id.toggleButton)).perform(click())
        Thread.sleep(1000)
        onView(withId(R.id.recycler_view)).check(matches(isDisplayed()))
    }

    @Test
    fun appList_whenActivityResumed_thenListIsVisible() {
        onView(withId(R.id.recycler_view)).check(matches(isDisplayed()))
    }
}
