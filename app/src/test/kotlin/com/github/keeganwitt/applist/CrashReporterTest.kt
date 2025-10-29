package com.github.keeganwitt.applist

import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class CrashReporterTest {
    private lateinit var crashlytics: FirebaseCrashlytics
    private lateinit var crashReporter: FirebaseCrashReporter

    @Before
    fun setup() {
        crashlytics = mockk(relaxed = true)
        mockkStatic(FirebaseCrashlytics::class)
        every { FirebaseCrashlytics.getInstance() } returns crashlytics
        crashReporter = FirebaseCrashReporter()
    }

    @Test
    fun `given message, when log called, then crashlytics logs message`() {
        val message = "Test log message"

        crashReporter.log(message)

        verify { crashlytics.log(message) }
    }

    @Test
    fun `given throwable without message, when record called, then crashlytics records exception`() {
        val throwable = RuntimeException("Test exception")

        crashReporter.record(throwable)

        verify { crashlytics.recordException(throwable) }
    }

    @Test
    fun `given throwable with message, when record called, then crashlytics logs message and records exception`() {
        val throwable = RuntimeException("Test exception")
        val message = "Error occurred"

        crashReporter.record(throwable, message)

        verify { crashlytics.log(message) }
        verify { crashlytics.recordException(throwable) }
    }

    @Test
    fun `given multiple log calls, when log called, then all messages are logged`() {
        crashReporter.log("Message 1")
        crashReporter.log("Message 2")
        crashReporter.log("Message 3")

        verify(exactly = 3) { crashlytics.log(any()) }
    }

    @Test
    fun `given multiple exceptions, when record called, then all exceptions are recorded`() {
        val exception1 = RuntimeException("Exception 1")
        val exception2 = IllegalStateException("Exception 2")

        crashReporter.record(exception1)
        crashReporter.record(exception2)

        verify { crashlytics.recordException(exception1) }
        verify { crashlytics.recordException(exception2) }
    }
}
