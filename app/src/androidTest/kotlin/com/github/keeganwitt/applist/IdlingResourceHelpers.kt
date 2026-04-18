package com.github.keeganwitt.applist

import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers

fun waitFor(delay: Long) {
    val idlingResource = ElapsedTimeIdlingResource(delay)
    try {
        IdlingRegistry.getInstance().register(idlingResource)
        Espresso
            .onView(
                ViewMatchers
                    .withId(android.R.id.content),
            ).perform(
                ViewActions
                    .swipeUp(),
            )
    } finally {
        IdlingRegistry.getInstance().unregister(idlingResource)
    }
}
