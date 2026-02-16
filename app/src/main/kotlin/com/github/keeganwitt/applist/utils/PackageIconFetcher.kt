package com.github.keeganwitt.applist.utils

import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.key.Keyer
import coil.request.Options
import com.github.keeganwitt.applist.services.PackageService

data class PackageIcon(
    val packageName: String,
)

class PackageIconFetcher(
    private val packageService: PackageService,
    private val data: PackageIcon,
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        val icon = packageService.getApplicationIcon(data.packageName) ?: return null
        return DrawableResult(
            drawable = icon,
            isSampled = false,
            dataSource = DataSource.DISK,
        )
    }

    class Factory(
        private val packageService: PackageService,
    ) : Fetcher.Factory<PackageIcon> {
        override fun create(
            data: PackageIcon,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher = PackageIconFetcher(packageService, data)
    }
}

class PackageIconKeyer : Keyer<PackageIcon> {
    override fun key(
        data: PackageIcon,
        options: Options,
    ): String = "pkg:${data.packageName}"
}
