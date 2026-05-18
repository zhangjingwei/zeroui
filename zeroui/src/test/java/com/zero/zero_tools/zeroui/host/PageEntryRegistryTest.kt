package com.zero.zero_tools.zeroui.host

import com.zero.zero_tools.zeroui.http.Cancelable
import com.zero.zero_tools.zeroui.http.RetirableCancelable
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
        r.register(1, r.nextId(), a)
        r.register(1, r.nextId(), b)

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
        r.register(1, r.nextId(), a)
        r.cancelAll(1)
        r.cancelAll(1)

        assertEquals(1, a.cancelCount)
        assertEquals(0, r.size())
    }

    @Test
    fun entriesAreIsolated() {
        val r = PageEntryRegistry()
        val homeC = CountingCancelable()
        val detailC = CountingCancelable()
        r.register(entryId = 1, id = r.nextId(), cancelable = homeC)
        r.register(entryId = 2, id = r.nextId(), cancelable = detailC)

        r.cancelAll(2)

        assertFalse(homeC.cancelled)
        assertTrue(detailC.cancelled)
        assertEquals(1, r.size())
    }

    @Test
    fun cancelAllAcrossEntries() {
        val r = PageEntryRegistry()
        val a = CountingCancelable()
        val b = CountingCancelable()
        val c = CountingCancelable()
        r.register(1, r.nextId(), a)
        r.register(2, r.nextId(), b)
        r.register(2, r.nextId(), c)

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
        r.register(1, r.nextId(), first)
        r.cancelAll(1)

        val second = CountingCancelable()
        r.register(1, r.nextId(), second)
        r.cancelAll(1)

        assertEquals(1, first.cancelCount)
        assertEquals(1, second.cancelCount)
    }

    @Test
    fun unregisterRemovesSingleCompletedCancelable() {
        val r = PageEntryRegistry()
        val first = CountingCancelable()
        val second = CountingCancelable()
        val firstId = r.nextId()
        val secondId = r.nextId()
        r.register(1, firstId, first)
        r.register(1, secondId, second)

        r.unregister(1, firstId)

        assertEquals(1, r.size())
        r.cancelAll(1)
        assertFalse(first.cancelled)
        assertTrue(second.cancelled)
    }

    @Test
    fun retiredCancelableUnregistersItself() {
        val r = PageEntryRegistry()
        val cancelable = ManuallyRetirableCancelable()
        r.register(1, r.nextId(), cancelable)

        cancelable.retire()

        assertEquals(0, r.size())
    }

    @Test
    fun alreadyRetiredCancelableIsNotRegistered() {
        val r = PageEntryRegistry()
        val cancelable = ManuallyRetirableCancelable()
        cancelable.retire()

        r.register(1, r.nextId(), cancelable)

        assertEquals(0, r.size())
    }

    @Test
    fun keyedRegisterDoesNotLeaveStaleKeyWhenCancelableAlreadyRetired() {
        val r = PageEntryRegistry()
        val cancelable = ManuallyRetirableCancelable()
        cancelable.retire()

        r.register(1, r.nextId(), "search", cancelable)
        r.cancel(1, "search")

        assertEquals(0, r.size())
    }

    @Test
    fun beginRequestCancelsPreviousKeyAndInvalidatesOldGeneration() {
        val r = PageEntryRegistry()
        val first = CountingCancelable()
        val firstGeneration = r.beginRequest(1, "search", cancelPrevious = true)
        r.register(1, r.nextId(), "search", first)

        val secondGeneration = r.beginRequest(1, "search", cancelPrevious = true)

        assertTrue(first.cancelled)
        assertFalse(r.isCurrent(1, "search", firstGeneration))
        assertTrue(r.isCurrent(1, "search", secondGeneration))
    }

    @Test
    fun cancelAllClearsGenerationEvenWhenNoCancelableListExists() {
        val r = PageEntryRegistry()
        val generation = r.beginRequest(1, "search", cancelPrevious = true)

        r.cancelAll(1)

        assertFalse(r.isCurrent(1, "search", generation))
    }

    private class ManuallyRetirableCancelable : RetirableCancelable {
        private val callbacks = mutableListOf<() -> Unit>()
        private var retired = false

        override val isRetired: Boolean
            get() = retired

        override fun cancel() {
            retire()
        }

        override fun invokeOnRetired(callback: () -> Unit) {
            if (retired) {
                callback()
            } else {
                callbacks.add(callback)
            }
        }

        fun retire() {
            if (retired) return
            retired = true
            callbacks.toList().forEach { it() }
            callbacks.clear()
        }
    }
}
