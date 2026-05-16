package com.zero.zero_tools.zeroui.http

/**
 * Minimal abstraction for "an effect that may still be running".
 *
 * The library does not commit to coroutines, threads, OkHttp Calls, or any other
 * specific runtime — it only commits to: "some side effect was started; the host can
 * ask for it to stop." Implementations are free to make `cancel` cooperative, forceful,
 * or even a no-op (e.g. if the work already completed).
 *
 * Calling [cancel] more than once must be safe (subsequent calls are no-ops).
 */
fun interface Cancelable {
    fun cancel()

    companion object {
        /** A cancelable that does nothing — for effects that have no abortable work. */
        val Noop: Cancelable = Cancelable {}
    }
}
