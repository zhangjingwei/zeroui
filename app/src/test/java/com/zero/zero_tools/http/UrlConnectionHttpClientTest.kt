package com.zero.zero_tools.http

import kotlinx.coroutines.Job
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicReference

/**
 * Wave 6 unit tests — focus on the disconnect routing built by [buildHttpCancelable].
 *
 * We intentionally do NOT exercise the coroutine pipeline here. Spinning up a real
 * `request()` in a unit test would require timing primitives (latches, sleeps) that
 * make tests flaky. Instead the helper [buildHttpCancelable] is extracted so we can
 * verify its contract in isolation:
 *   - cancel must call `disconnect()` on the live connection
 *   - cancel must clear the ref so the IO thread's finally block doesn't double-disconnect
 *   - cancel must cancel the Job
 *   - cancel must be idempotent and safe when the ref is already null
 *
 * The IO-side `compareAndSet + disconnect` in `runRequest`'s finally block is verified
 * by code review (it's the symmetric half of the same protocol).
 */
class UrlConnectionHttpClientTest {

    @Test
    fun cancelDisconnectsLiveConnectionAndClearsRef() {
        val fake = FakeHttpURLConnection()
        val ref = AtomicReference<HttpURLConnection?>(fake)
        val job = Job()

        val cancelable = buildHttpCancelable(job, ref)
        cancelable.cancel()

        assertEquals("disconnect must be called exactly once", 1, fake.disconnectCount)
        assertNull("ref must be cleared so IO finally won't re-disconnect", ref.get())
        assertTrue("job must be cancelled", job.isCancelled)
    }

    @Test
    fun cancelIsSafeWhenRefAlreadyCleared() {
        // Simulates the case where runRequest's finally already completed normally:
        // it CAS-cleared the ref and disconnected. A subsequent cancel from the host
        // (because the page popped just after IO finished) must be a no-op for the
        // connection — and still cancel the (already finished) Job harmlessly.
        val ref = AtomicReference<HttpURLConnection?>(null)
        val job = Job()

        buildHttpCancelable(job, ref).cancel()

        assertNull(ref.get())
        assertTrue(job.isCancelled)
        // No connection was held so nothing to assert about disconnect — the point is
        // that this branch must not NPE.
    }

    @Test
    fun cancelIsIdempotent() {
        val fake = FakeHttpURLConnection()
        val ref = AtomicReference<HttpURLConnection?>(fake)
        val cancelable = buildHttpCancelable(Job(), ref)

        cancelable.cancel()
        cancelable.cancel() // second call must not double-disconnect
        cancelable.cancel() // third for good measure

        assertEquals(1, fake.disconnectCount)
    }

    @Test
    fun ioFinallyAndCancelRaceProducesExactlyOneDisconnect() {
        // The contract: at most one party disconnects the same connection.
        // Here we simulate "cancel wins" — it takes the ref first; the IO thread's
        // finally then sees null and skips disconnect. The reverse direction (IO
        // wins) is the same logic with roles swapped.
        val fake = FakeHttpURLConnection()
        val ref = AtomicReference<HttpURLConnection?>(fake)

        // Cancel claims the ref via getAndSet.
        buildHttpCancelable(Job(), ref).cancel()

        // IO finally now arrives. It compares-and-set's its own local `connection`
        // (== fake) against the ref (now null). The CAS fails, so it skips disconnect.
        val ioOwned = ref.compareAndSet(fake, null)
        assertFalse("IO finally must not claim a ref already taken by cancel", ioOwned)
        assertEquals("Still only one disconnect from the cancel path", 1, fake.disconnectCount)
    }

    /**
     * Minimal [HttpURLConnection] stub. Only [disconnect] is meaningful — the rest
     * are stubbed to satisfy the abstract contract. Tests never actually drive
     * connect / read / write through this class; they only verify the disconnect
     * routing inside [buildHttpCancelable].
     */
    private class FakeHttpURLConnection : HttpURLConnection(URL("http://fake.local")) {
        var disconnectCount: Int = 0
            private set

        override fun disconnect() {
            disconnectCount++
        }

        override fun connect() = Unit
        override fun usingProxy(): Boolean = false

        override fun getInputStream(): InputStream =
            throw UnsupportedOperationException("not used")
        override fun getOutputStream(): OutputStream =
            throw UnsupportedOperationException("not used")
    }
}
