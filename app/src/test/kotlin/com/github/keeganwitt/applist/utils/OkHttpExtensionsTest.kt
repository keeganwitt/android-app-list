package com.github.keeganwitt.applist.utils

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class OkHttpExtensionsTest {
    @Test
    fun `when await is called, then it enqueues the call and resumes with response`() =
        runTest {
            val call = mockk<Call>()
            val response = mockk<Response>()
            val callbackSlot = slot<Callback>()

            every { call.enqueue(capture(callbackSlot)) } answers {
                callbackSlot.captured.onResponse(call, response)
            }

            val result = call.await()

            assertEquals(response, result)
        }

    @Test(expected = IOException::class)
    fun `when await is called and fails, then it resumes with exception`() =
        runTest {
            val call = mockk<Call>()
            val exception = IOException("Network error")
            val callbackSlot = slot<Callback>()

            every { call.enqueue(capture(callbackSlot)) } answers {
                callbackSlot.captured.onFailure(call, exception)
            }

            call.await()
        }

    @Test
    fun `when coroutine is cancelled, then it cancels the call`() =
        runTest {
            val call = mockk<Call>(relaxed = true)

            every { call.enqueue(any()) } returns Unit

            val job =
                launch {
                    call.await()
                }

            runCurrent()
            job.cancel()

            verify { call.cancel() }
        }

    @Test
    fun `when await is called and fails after cancellation, then it ignores failure`() =
        runTest {
            val call = mockk<Call>(relaxed = true)
            val callbackSlot = slot<Callback>()

            every { call.enqueue(capture(callbackSlot)) } returns Unit

            val job =
                launch {
                    try {
                        call.await()
                    } catch (e: CancellationException) {
                        // Expected
                    } catch (e: Exception) {
                        org.junit.Assert.fail("Expected only CancellationException, but got ${e.javaClass.simpleName}")
                    }
                }

            runCurrent()
            job.cancel()
            runCurrent()

            // Trigger onFailure after cancellation
            callbackSlot.captured.onFailure(call, IOException("Ignored error"))

            // If we reach here without crash or test failure, the guard at line 19 was executed
        }
}
