package com.github.keeganwitt.applist

import androidx.test.core.app.ApplicationProvider
import com.github.keeganwitt.applist.services.AppStoreService
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestAppListApplication::class)
class AppRepositoryFactoryTest {
    @Test
    fun `factory creates Android repository with application services`() {
        val repository =
            AppRepositoryFactory.create(
                context = ApplicationProvider.getApplicationContext(),
                appStoreService = mockk<AppStoreService>(relaxed = true),
                crashReporter = mockk(relaxed = true),
            )

        assertTrue(repository is AndroidAppRepository)
    }
}
