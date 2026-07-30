package com.doodlefrens.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * A coroutine-based countdown timer that emits the remaining time at a fixed frequency.
 *
 * This timer uses a cold [flow] so it is inherently cancellation-safe and composable.
 *
 * @param duration        Total countdown time in milliseconds.
 * @param emissionFrequency How often (ms) to emit a tick. Defaults to 100 ms.
 */
class CoroutineTimer {

    /**
     * Starts a countdown from [duration] down to 0, emitting the remaining time
     * (in ms) every [emissionFrequency] milliseconds, and returns the backing [Job]
     * so the caller can cancel it at any time.
     *
     * @param duration          Total time in milliseconds.
     * @param coroutineScope    Scope in which the timer runs (typically [viewModelScope]).
     * @param emissionFrequency Tick interval in milliseconds (default 100 ms).
     * @param onEmit            Callback invoked on every tick with the remaining time in ms.
     */
    fun timeAndEmit(
        duration: Long,
        coroutineScope: CoroutineScope,
        emissionFrequency: Long = 100L,
        onEmit: suspend (Long) -> Unit
    ): Job = coroutineScope.launch {
        countdownFlow(duration, emissionFrequency).collect { remaining ->
            onEmit(remaining)
        }
    }

    /**
     * Returns a cold [kotlinx.coroutines.flow.Flow] that emits [duration], then counts
     * down in steps of [emissionFrequency] until it reaches 0 (inclusive).
     */
    private fun countdownFlow(duration: Long, emissionFrequency: Long) = flow {
        var remaining = duration
        while (remaining >= 0L) {
            emit(remaining)
            if (remaining == 0L) break
            delay(emissionFrequency.milliseconds)
            remaining = (remaining - emissionFrequency).coerceAtLeast(0L)
        }
    }
}
