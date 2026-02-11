package com.github.keeganwitt.applist.utils

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.github.keeganwitt.applist.TestAppListApplication
import com.github.keeganwitt.applist.services.PackageService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestAppListApplication::class)
class IconLoaderTest {

    private lateinit var packageService: PackageService
    private lateinit var iconLoader: IconLoader

    @Before
    fun setup() {
        packageService = mockk()
        iconLoader = IconLoader(packageService)
    }

    @Test
    fun `given icon in cache, when loadIcon called, then returns cached icon`() = runTest {
        val packageName = "com.example.app"
        val mockDrawable = mockk<Drawable>(relaxed = true)
        
        every { packageService.getApplicationIcon(packageName) } returns mockDrawable
        
        // First load to populate cache
        iconLoader.loadIcon(packageName)
        
        // Second load should hit cache
        val result = iconLoader.loadIcon(packageName)
        
        assertEquals(mockDrawable, result)
        // Verify service was called exactly once (for the first load)
        verify(exactly = 1) { packageService.getApplicationIcon(packageName) }
    }

    @Test
    fun `given icon not in cache, when loadIcon called, then loads from service and returns icon`() = runTest {
        val packageName = "com.example.app"
        val mockDrawable = mockk<Drawable>(relaxed = true)
        every { packageService.getApplicationIcon(packageName) } returns mockDrawable

        val result = iconLoader.loadIcon(packageName)

        assertEquals(mockDrawable, result)
        verify(exactly = 1) { packageService.getApplicationIcon(packageName) }
    }

    @Test
    fun `given service returns null, when loadIcon called, then returns null`() = runTest {
        val packageName = "com.example.unknown"
        every { packageService.getApplicationIcon(packageName) } returns null

        val result = iconLoader.loadIcon(packageName)

        assertNull(result)
        verify(exactly = 1) { packageService.getApplicationIcon(packageName) }
    }

    @Test
    fun `given bitmap drawable, when loadIcon called, then caches with bitmap size`() = runTest {
        val packageName = "com.example.bitmap"
        val mockBitmap = mockk<Bitmap>()
        every { mockBitmap.byteCount } returns 10240 // 10KB
        val mockDrawable = mockk<BitmapDrawable>()
        every { mockDrawable.bitmap } returns mockBitmap
        every { packageService.getApplicationIcon(packageName) } returns mockDrawable

        val result = iconLoader.loadIcon(packageName)

        assertEquals(mockDrawable, result)
        // This execution path hits the "if (drawable is BitmapDrawable)" branch in sizeOf
    }
    @Test
    fun `given small bitmap, when loadIcon called, then caches with minimum size`() = runTest {
        val packageName = "com.example.small"
        val mockBitmap = mockk<Bitmap>()
        every { mockBitmap.byteCount } returns 500 // < 1KB
        val mockDrawable = mockk<BitmapDrawable>()
        every { mockDrawable.bitmap } returns mockBitmap
        every { packageService.getApplicationIcon(packageName) } returns mockDrawable

        val result = iconLoader.loadIcon(packageName)

        assertEquals(mockDrawable, result)
        // This execution path hits the "if (size == 0)" branch returning 1
    }
    @Test
    fun `given vector drawable with intrinsic size, when loadIcon called, then caches with calculated size`() = runTest {
        val packageName = "com.example.vector"
        val mockDrawable = mockk<Drawable>()
        every { mockDrawable.intrinsicWidth } returns 100
        every { mockDrawable.intrinsicHeight } returns 100
        every { packageService.getApplicationIcon(packageName) } returns mockDrawable

        val result = iconLoader.loadIcon(packageName)

        assertEquals(mockDrawable, result)
        // This execution path hits the "else" block in sizeOf and the "if > 0" branches for dimensions
    }
}
