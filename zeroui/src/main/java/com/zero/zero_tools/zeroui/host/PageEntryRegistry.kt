package com.zero.zero_tools.zeroui.host

import com.zero.zero_tools.zeroui.http.Cancelable
import com.zero.zero_tools.zeroui.http.RetirableCancelable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

internal class PageEntryRegistry {
    private val nextCancelableId = AtomicLong(0L)
    private val perEntry = ConcurrentHashMap<Long, MutableList<RegisteredCancelable>>()
    private val keyed = ConcurrentHashMap<Pair<Long, String>, Long>()
    private val generations = ConcurrentHashMap<Pair<Long, String>, AtomicLong>()

    fun nextId(): Long = nextCancelableId.incrementAndGet()

    fun register(entryId: Long, id: Long, cancelable: Cancelable): Boolean {
        val list = perEntry.computeIfAbsent(entryId) { mutableListOf() }
        if (cancelable is RetirableCancelable) {
            cancelable.invokeOnRetired { unregister(entryId, id) }
            if (cancelable.isRetired) return false
        }
        synchronized(list) {
            if (cancelable is RetirableCancelable && cancelable.isRetired) return false
            list.add(RegisteredCancelable(id = id, cancelable = cancelable))
        }
        return true
    }

    fun register(entryId: Long, id: Long, requestKey: String?, cancelable: Cancelable) {
        val registered = register(entryId, id, cancelable)
        if (registered && requestKey != null) {
            keyed[entryId to requestKey] = id
            if (cancelable is RetirableCancelable) {
                cancelable.invokeOnRetired { keyed.remove(entryId to requestKey, id) }
            }
        }
    }

    fun beginRequest(entryId: Long, requestKey: String?, cancelPrevious: Boolean): Long {
        if (requestKey == null) return 0L
        val key = entryId to requestKey
        val generation = generations.computeIfAbsent(key) { AtomicLong(0L) }.incrementAndGet()
        if (cancelPrevious) {
            cancel(entryId, requestKey)
        }
        return generation
    }

    fun isCurrent(entryId: Long, requestKey: String?, generation: Long): Boolean {
        if (requestKey == null) return true
        return generations[entryId to requestKey]?.get() == generation
    }

    fun unregister(entryId: Long, id: Long) {
        val list = perEntry[entryId] ?: return
        val empty = synchronized(list) {
            list.removeAll { it.id == id }
            list.isEmpty()
        }
        keyed.entries.removeAll { entry -> entry.key.first == entryId && entry.value == id }
        if (empty) {
            perEntry.remove(entryId, list)
        }
    }

    fun cancel(entryId: Long, requestKey: String) {
        val id = keyed.remove(entryId to requestKey) ?: return
        val list = perEntry[entryId] ?: return
        val cancelable = synchronized(list) {
            val match = list.firstOrNull { it.id == id }
            if (match != null) {
                list.removeAll { it.id == id }
            }
            match?.cancelable
        }
        cancelable?.cancel()
        if (synchronized(list) { list.isEmpty() }) {
            perEntry.remove(entryId, list)
        }
    }

    fun cancelAll(entryId: Long) {
        keyed.entries.removeAll { it.key.first == entryId }
        generations.entries.removeAll { it.key.first == entryId }
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
