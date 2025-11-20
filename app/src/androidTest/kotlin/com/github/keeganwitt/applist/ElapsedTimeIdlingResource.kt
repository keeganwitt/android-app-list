package com.github.keeganwitt.applist

import androidx.test.espresso.IdlingResource
import androidx.test.espresso.IdlingResource.ResourceCallback

class ElapsedTimeIdlingResource(
    private val waitingTime: Long,
) : IdlingResource {
    private val startTime: Long = System.currentTimeMillis()
    private var resourceCallback: ResourceCallback? = null

    override fun getName(): String = ElapsedTimeIdlingResource::class.java.name + waitingTime

    override fun isIdleNow(): Boolean {
        val elapsed = System.currentTimeMillis() - startTime
        val idle = elapsed >= waitingTime
        if (idle) {
            resourceCallback?.onTransitionToIdle()
        }
        return idle
    }

    override fun registerIdleTransitionCallback(callback: ResourceCallback) {
        this.resourceCallback = callback
    }
}
