package com.github.keeganwitt.applist

import android.view.View
import androidx.test.espresso.Espresso
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import org.hamcrest.Matcher

fun waitFor(delay: Long) {
    Espresso.onView(isRoot()).perform(
        object : ViewAction {
            override fun getConstraints(): Matcher<View> = isRoot()

            override fun getDescription(): String = "wait for $delay milliseconds"

            override fun perform(
                uiController: UiController,
                view: View,
            ) {
                uiController.loopMainThreadForAtLeast(delay)
            }
        },
    )
}
