package com.zero.zero_tools.host

import com.zero.zero_tools.zeroui.http.Cancelable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PageEntryRegistryTest {

    private class CountingCancelable : Cancelable {
        var cancelCount: Int = 0
            private set
        override fun cancel() {
            cancelCount++
        }
        val cancelled: Boolean get() = cancelCount > 0
    }

    @Test
    fun registerAndCancelAllForEntry() {
        val r = PageEntryRegistry()
        val a = CountingCancelable()
        val b = CountingCancelable()
        r.register(1, a)
        r.register(1, b)

        assertEquals(2, r.size())
        r.cancelAll(1)

        assertTrue(a.cancelled)
        assertTrue(b.cancelled)
        assertEquals(0, r.size())
    }

    @Test
    fun cancelAllForEntryIsIdempotent() {
        val r = PageEntryRegistry()
        val a = CountingCancelable()
        r.register(1, a)
        r.cancelAll(1)
        r.cancelAll(1) // must not throw, must not double-cancel

        assertEquals(1, a.cancelCount)
        assertEquals(0, r.size())
    }

    @Test
    fun entriesAreIsolated() {
        val r = PageEntryRegistry()
        val homeC = CountingCancelable()
        val detailC = CountingCancelable()
        r.register(entryId = 1, cancelable = homeC)
        r.register(entryId = 2, cancelable = detailC)

        r.cancelAll(2)

        assertFalse("home cancelable must not fire when detail is cancelled", homeC.cancelled)
        assertTrue(detailC.cancelled)
        assertEquals(1, r.size()) // home's cancelable still registered
    }

    @Test
    fun cancelAllAcrossEntries() {
        val r = PageEntryRegistry()
        val a = CountingCancelable()
        val b = CountingCancelable()
        val c = CountingCancelable()
        r.register(1, a)
        r.register(2, b)
        r.register(2, c)

        r.cancelAll()

        assertTrue(a.cancelled)
        assertTrue(b.cancelled)
        assertTrue(c.cancelled)
        assertEquals(0, r.size())
    }

    @Test
    fun registerAfterCancelAllStartsFresh() {
        val r = PageEntryRegistry()
        val first = CountingCancelable()
        r.register(1, first)
        r.cancelAll(1)

        // After bucket removed, register adds to a fresh list — and the old `first`
        // doesn't get re-cancelled on the next cancelAll(1).
        val second = CountingCancelable()
        r.register(1, second)
        r.cancelAll(1)

        assertEquals("first should have been cancelled exactly once", 1, first.cancelCount)
        assertEquals(1, second.cancelCount)
    }
}
