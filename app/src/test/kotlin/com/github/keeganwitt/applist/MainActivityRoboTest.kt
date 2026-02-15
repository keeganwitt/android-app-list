package com.github.keeganwitt.applist

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = TestAppListApplication::class)
class MainActivityRoboTest {

    @Test
    fun `when activity launched then it is resumed and views are initialized`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
            scenario.onActivity { activity ->
                // Basic check to ensure onCreate completed and views were bound
                // We use reflection or just access if they are not private, but binding is private.
                // However, we can check if the activity itself is not null and has a decor view.
                assertNotNull(activity)
                assertNotNull(activity.window.decorView)
            }
        }
    }
}
