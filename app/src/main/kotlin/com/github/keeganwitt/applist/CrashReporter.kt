package com.github.keeganwitt.applist

import com.google.firebase.crashlytics.FirebaseCrashlytics

interface CrashReporter {
    fun log(message: String)

    fun recordException(
        throwable: Throwable,
        message: String? = null,
    )
}

class FirebaseCrashReporter : CrashReporter {
    private val crashlytics get() = FirebaseCrashlytics.getInstance()

    override fun log(message: String) {
        crashlytics.log(message)
    }

    override fun recordException(
        throwable: Throwable,
        message: String?,
    ) {
        message?.let { crashlytics.log(it) }
        crashlytics.recordException(throwable)
    }
}
