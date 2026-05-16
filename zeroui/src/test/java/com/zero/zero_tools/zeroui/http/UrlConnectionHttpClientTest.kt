package com.zero.zero_tools.zeroui.http

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

class UrlConnectionHttpClientTest {

    @Test
    fun cancelDisconnectsLiveConnectionAndClearsRef() {
        val fake = FakeHttpURLConnection()
        val ref = AtomicReference<HttpURLConnection?>(fake)
        val job = Job()

        val cancelable = buildHttpCancelable(job, ref)
        cancelable.cancel()

        assertEquals(1, fake.disconnectCount)
        assertNull(ref.get())
        assertTrue(job.isCancelled)
    }

    @Test
    fun cancelIsSafeWhenRefAlreadyCleared() {
        val ref = AtomicReference<HttpURLConnection?>(null)
        val job = Job()

        buildHttpCancelable(job, ref).cancel()

        assertNull(ref.get())
        assertTrue(job.isCancelled)
    }

    @Test
    fun cancelIsIdempotent() {
        val fake = FakeHttpURLConnection()
        val ref = AtomicReference<HttpURLConnection?>(fake)
        val cancelable = buildHttpCancelable(Job(), ref)

        cancelable.cancel()
        cancelable.cancel()
        cancelable.cancel()

        assertEquals(1, fake.disconnectCount)
    }

    @Test
    fun ioFinallyAndCancelRaceProducesExactlyOneDisconnect() {
        val fake = FakeHttpURLConnection()
        val ref = AtomicReference<HttpURLConnection?>(fake)

        buildHttpCancelable(Job(), ref).cancel()

        val ioOwned = ref.compareAndSet(fake, null)
        assertFalse(ioOwned)
        assertEquals(1, fake.disconnectCount)
    }

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
