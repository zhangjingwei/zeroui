package com.zero.zero_tools.zeroui.host

import com.zero.zero_tools.zeroui.http.Cancelable
import com.zero.zero_tools.zeroui.http.RetirableCancelable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

internal class PageEntryRegistry {
    private val nextCancelableId = AtomicLong(0L)
    private val perEntry = ConcurrentHashMap<Long, MutableList<RegisteredCancelable>>()

    fun nextId(): Long = nextCancelableId.incrementAndGet()

    fun register(entryId: Long, id: Long, cancelable: Cancelable) {
        val list = perEntry.computeIfAbsent(entryId) { mutableListOf() }
        if (cancelable is RetirableCancelable) {
            cancelable.invokeOnRetired { unregister(entryId, id) }
            if (cancelable.isRetired) return
        }
        synchronized(list) {
            if (cancelable is RetirableCancelable && cancelable.isRetired) return
            list.add(RegisteredCancelable(id = id, cancelable = cancelable))
        }
    }

    fun unregister(entryId: Long, id: Long) {
        val list = perEntry[entryId] ?: return
        val empty = synchronized(list) {
            list.removeAll { it.id == id }
            list.isEmpty()
        }
        if (empty) {
            perEntry.remove(entryId, list)
        }
    }

    fun cancelAll(entryId: Long) {
        val list = perEntry.remove(entryId) ?: return
        val cancelables = synchronized(list) {
            list.map { it.cancelable }.also { list.clear() }
        }
        cancelables.forEach { it.cancel() }
    }

    fun cancelAll() {
        perEntry.keys.toList().forEach(::cancelAll)
    }

    internal fun size(): Int = perEntry.values.sumOf { list ->
        synchronized(list) { list.size }
    }

    private data class RegisteredCancelable(
        val id: Long,
        val cancelable: Cancelable
    )
}
