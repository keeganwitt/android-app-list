package com.github.keeganwitt.applist.utils

import android.graphics.drawable.Drawable
import coil.ImageLoader
import coil.fetch.DrawableResult
import coil.request.Options
import com.github.keeganwitt.applist.services.PackageService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PackageIconFetcherTest {
    private lateinit var packageService: PackageService
    private lateinit var fetcherFactory: PackageIconFetcher.Factory
    private lateinit var options: Options
    private lateinit var imageLoader: ImageLoader

    @Before
    fun setup() {
        packageService = mockk()
        fetcherFactory = PackageIconFetcher.Factory(packageService)
        options = mockk(relaxed = true)
        imageLoader = mockk(relaxed = true)
    }

    @Test
    fun `fetch returns DrawableResult when icon exists`() =
        runTest {
            val packageName = "com.example.app"
            val drawable = mockk<Drawable>()
            coEvery { packageService.getApplicationIcon(packageName) } returns drawable

            val fetcher = PackageIconFetcher(packageService, PackageIcon(packageName))
            val result = fetcher.fetch()

            assertNotNull(result)
            assertTrue(result is DrawableResult)
            assertEquals(drawable, (result as DrawableResult).drawable)
        }

    @Test
    fun `fetch returns null when icon does not exist`() =
        runTest {
            val packageName = "com.example.missing"
            coEvery { packageService.getApplicationIcon(packageName) } returns null

            val fetcher = PackageIconFetcher(packageService, PackageIcon(packageName))
            val result = fetcher.fetch()

            assertNull(result)
        }

    @Test
    fun `factory creates fetcher`() {
        val packageName = "com.example.app"
        val fetcher =
            fetcherFactory.create(
                PackageIcon(packageName),
                options,
                imageLoader,
            )

        assertNotNull(fetcher)
        assertTrue(fetcher is PackageIconFetcher)
    }

    @Test
    fun `keyer returns correct key`() {
        val keyer = PackageIconKeyer()
        val packageName = "com.example.app"
        val key = keyer.key(PackageIcon(packageName), options)

        assertEquals("pkg:com.example.app", key)
    }
}
