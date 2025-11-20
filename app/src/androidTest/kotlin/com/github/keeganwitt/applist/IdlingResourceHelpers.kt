package com.github.keeganwitt.applist

import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry

fun waitFor(delay: Long) {
    val idlingResource = ElapsedTimeIdlingResource(delay)
    try {
        IdlingRegistry.getInstance().register(idlingResource)
        Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(android.R.id.content)).perform(androidx.test.espresso.action.ViewActions.swipeUp())
    } finally {
        IdlingRegistry.getInstance().unregister(idlingResource)
    }
}