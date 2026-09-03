package com.github.keeganwitt.applist

import android.content.res.Configuration
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBackUnconditionally
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE
import androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@LargeTest
class ExportActivityTest {
    @Test
    fun statusBarIconsContrastWithTheCurrentTheme() {
        ActivityScenario.launch(ExportActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val isLightMode =
                    activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK !=
                        Configuration.UI_MODE_NIGHT_YES
                val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)

                assertEquals(isLightMode, controller.isAppearanceLightStatusBars)
            }
        }
    }

    @Test
    fun searchFieldFiltersImmediatelyClearsAndSurvivesRecreation() {
        ActivityScenario.launch(ExportActivity::class.java).use { scenario ->
            waitFor(3000)

            onView(withId(R.id.app_search)).check(matches(isAssignableFrom(TextInputEditText::class.java)))
            scenario.onActivity { activity ->
                val search = activity.findViewById<TextInputEditText>(R.id.app_search)
                val layout =
                    generateSequence(search as android.view.View?) { it.parent as? android.view.View }
                        .filterIsInstance<TextInputLayout>()
                        .first()
                assertEquals(TextInputLayout.BOX_BACKGROUND_OUTLINE, layout.boxBackgroundMode)
            }
            onView(withId(R.id.app_search)).perform(click(), replaceText("not-a-real-app-package"))
            onView(withId(R.id.no_results)).check(matches(withEffectiveVisibility(VISIBLE)))
            waitFor(300)
            onView(withContentDescription("Clear text")).perform(click())
            onView(withId(R.id.app_search)).check(matches(withText("")))
            onView(withId(R.id.no_results)).check(matches(withEffectiveVisibility(GONE)))

            onView(withId(R.id.app_search)).perform(replaceText("android"))
            scenario.recreate()
            onView(withId(R.id.app_search)).check(matches(withText("android")))
            onView(withId(R.id.app_search)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun reviewKeepsFooterSummaryAndExportVisible() {
        ActivityScenario.launch(ExportActivity::class.java).use { scenario ->
            waitFor(3000)
            var selectedCount = ""
            var exportLabel = ""
            scenario.onActivity { activity ->
                val countView = activity.findViewById<android.widget.TextView>(R.id.selected_count)
                selectedCount = countView.text.toString()
                exportLabel = activity.findViewById<android.widget.Button>(R.id.export_button).text.toString()
                assertEquals(R.id.selection_footer, (countView.parent as android.view.View).id)
            }

            onView(withId(R.id.review_selected)).perform(click())

            onView(withId(R.id.review_selected)).check(matches(withEffectiveVisibility(GONE)))
            onView(withId(R.id.selected_count)).check(matches(withText(selectedCount)))
            onView(withId(R.id.export_button)).check(matches(withText(exportLabel)))
        }
    }

    @Test
    fun selectionChangeSynchronizesFooterAndExportBeforeAndDuringReview() {
        ActivityScenario.launch(ExportActivity::class.java).use { scenario ->
            waitFor(3000)
            var countBefore = ""
            scenario.onActivity { activity ->
                countBefore = activity.findViewById<android.widget.TextView>(R.id.selected_count).text.toString()
            }

            onView(withId(R.id.app_list))
                .perform(RecyclerViewActions.actionOnItemAtPosition<androidx.recyclerview.widget.RecyclerView.ViewHolder>(0, click()))

            var countAfter = ""
            var expectedExportLabel = ""
            scenario.onActivity { activity ->
                countAfter = activity.findViewById<android.widget.TextView>(R.id.selected_count).text.toString()
                val count = countAfter.substringBefore(" app").toInt()
                expectedExportLabel = activity.resources.getQuantityString(R.plurals.export_action_count, count, count)
            }
            assertNotEquals(countBefore, countAfter)
            onView(withId(R.id.export_button)).check(matches(withText(expectedExportLabel)))
            onView(withId(R.id.review_selected)).check(matches(withEffectiveVisibility(VISIBLE)))
            onView(withId(R.id.review_selected)).check(matches(isEnabled()))

            onView(withId(R.id.review_selected)).perform(click())
            onView(withId(R.id.review_selected)).check(matches(withEffectiveVisibility(GONE)))
            onView(withId(R.id.selected_count)).check(matches(withText(countAfter)))
            onView(withId(R.id.export_button)).check(matches(withText(expectedExportLabel)))
        }
    }

    @Test
    fun userFilterArchiveAndFormatAreSelectedByDefault() {
        ActivityScenario.launch(ExportActivity::class.java).use {
            waitFor(3000)

            onView(withId(R.id.filter_user_apps)).check(matches(isChecked()))
            onView(withId(R.id.show_archived)).check(matches(isNotChecked()))
            onView(withId(R.id.format_xml)).check(matches(isChecked()))
            onView(withId(R.id.selection_controls)).check(matches(withEffectiveVisibility(VISIBLE)))
        }
    }

    @Test
    fun changingTypeFilterPreservesSelection() {
        ActivityScenario.launch(ExportActivity::class.java).use { scenario ->
            waitFor(3000)
            var selectedCount = ""
            scenario.onActivity { activity ->
                selectedCount = activity.findViewById<android.widget.TextView>(R.id.selected_count).text.toString()
            }

            onView(withId(R.id.filter_system_apps)).perform(scrollTo(), click())

            onView(withId(R.id.filter_system_apps)).check(matches(isChecked()))
            onView(withId(R.id.selected_count)).check(matches(withText(selectedCount)))
        }
    }

    @Test
    fun rotationRetainsFilterSelectionArchiveAndFormat() {
        ActivityScenario.launch(ExportActivity::class.java).use { scenario ->
            waitFor(3000)
            var selectedCount = ""
            scenario.onActivity { activity ->
                selectedCount = activity.findViewById<android.widget.TextView>(R.id.selected_count).text.toString()
            }
            onView(withId(R.id.filter_system_apps)).perform(scrollTo(), click())
            onView(withId(R.id.show_archived)).perform(click())
            onView(withId(R.id.format_csv)).perform(scrollTo(), click())

            scenario.recreate()

            onView(withId(R.id.filter_system_apps)).check(matches(isChecked()))
            onView(withId(R.id.selected_count)).check(matches(withText(selectedCount)))
            onView(withId(R.id.show_archived)).check(matches(isChecked()))
            onView(withId(R.id.format_csv)).check(matches(isChecked()))
        }
    }

    @Test
    fun reviewModeShowsClearAllAndReturnsToBrowse() {
        ActivityScenario.launch(ExportActivity::class.java).use {
            waitFor(3000)

            onView(withId(R.id.review_selected)).perform(click())
            onView(withId(R.id.review_header)).check(matches(withEffectiveVisibility(VISIBLE)))
            onView(withId(R.id.browse_filters)).check(matches(withEffectiveVisibility(GONE)))
            onView(withId(R.id.clear_selection)).check(matches(withText(R.string.export_clear_all)))
            onView(withId(R.id.return_to_browse)).perform(scrollTo(), click())
            onView(withId(R.id.browse_filters)).check(matches(withEffectiveVisibility(VISIBLE)))
            onView(withId(R.id.review_header)).check(matches(withEffectiveVisibility(GONE)))
        }
    }

    @Test
    fun systemBackFromReviewReturnsToBrowse() {
        ActivityScenario.launch(ExportActivity::class.java).use { scenario ->
            waitFor(3000)
            onView(withId(R.id.review_selected)).perform(click())

            pressBackUnconditionally()

            onView(withId(R.id.browse_filters)).check(matches(withEffectiveVisibility(VISIBLE)))
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }

    @Test
    fun toolbarUpFromReviewReturnsToBrowse() {
        ActivityScenario.launch(ExportActivity::class.java).use { scenario ->
            waitFor(3000)
            onView(withId(R.id.review_selected)).perform(click())

            var navigationDescription = ""
            scenario.onActivity {
                navigationDescription =
                    it
                        .findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
                        .navigationContentDescription
                        .toString()
            }
            onView(withContentDescription(navigationDescription)).perform(click())

            onView(withId(R.id.browse_filters)).check(matches(withEffectiveVisibility(VISIBLE)))
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }

    @Test
    fun backCancelsExportFlow() {
        ActivityScenario.launch(ExportActivity::class.java).use { scenario ->
            val destroyed = CountDownLatch(1)
            scenario.onActivity { activity ->
                activity.lifecycle.addObserver(
                    LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_DESTROY) destroyed.countDown()
                    },
                )
            }
            pressBackUnconditionally()

            assertTrue("Export activity did not close after Back", destroyed.await(5, TimeUnit.SECONDS))
            assertEquals(Lifecycle.State.DESTROYED, scenario.state)
        }
    }
}
