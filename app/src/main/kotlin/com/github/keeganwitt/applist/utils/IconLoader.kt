package com.github.keeganwitt.applist.utils

import android.graphics.drawable.Drawable
import android.util.LruCache
import com.github.keeganwitt.applist.services.PackageService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class IconLoader(
    private val packageService: PackageService,
) {
    private val memoryCache: LruCache<String, Drawable>

    init {
        // Use 1/8th of available memory for cache
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8
        memoryCache =
            object : LruCache<String, Drawable>(cacheSize) {
                // Determine size of drawable roughly (this is an approximation)
                override fun sizeOf(
                    key: String,
                    drawable: Drawable,
                ): Int {
                    // Start with a base size
                    var size = 1

                    // Try to get bitmap size if it's a BitmapDrawable
                    if (drawable is android.graphics.drawable.BitmapDrawable) {
                        size = drawable.bitmap.byteCount / 1024
                    } else {
                        // Fallback for vector drawables/adaptive icons: estimate based on intrinsic dimensions
                        // Assuming ARGB_8888 (4 bytes per pixel)
                        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
                        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
                        size = (width * height * 4) / 1024
                    }

                    // Ensure we never return 0 to avoid division by zero errors elsewhere/not counting it
                    return if (size == 0) 1 else size
                }
            }
    }

    suspend fun loadIcon(packageName: String): Drawable? =
        withContext(Dispatchers.IO) {
            val cached = memoryCache.get(packageName)
            if (cached != null) {
                return@withContext cached
            }

            val icon = packageService.getApplicationIcon(packageName)
            if (icon != null) {
                memoryCache.put(packageName, icon)
            }
            icon
        }
}
