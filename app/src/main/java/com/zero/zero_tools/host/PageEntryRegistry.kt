package com.zero.zero_tools.host

import com.zero.zero_tools.zeroui.http.Cancelable

/**
 * Maps a page entry id → its in-flight [Cancelable]s.
 *
 * Wave 4 first defense line: when an entry is popped from the navigation stack, the
 * host calls [cancelAll] for that id; in-flight HTTP requests issued by that entry
 * have their `cancel()` invoked, freeing network / CPU. The second defense line
 * remains [com.zero.zero_tools.host.pageScopedFollowUp], which discards any late
 * response that nonetheless lands.
 *
 * Thread-safety: not synchronized. All access happens on the main thread
 * (Compose / `lifecycleScope` callbacks).
 */
internal class PageEntryRegistry {
    private val perEntry = mutableMapOf<Long, MutableList<Cancelable>>()

    /** Records a cancelable as belonging to [entryId]. */
    fun register(entryId: Long, cancelable: Cancelable) {
        perEntry.getOrPut(entryId) { mutableListOf() }.add(cancelable)
    }

    /**
     * Cancels and forgets every cancelable belonging to [entryId].
     * Idempotent — a second call is a no-op because the bucket is removed.
     */
    fun cancelAll(entryId: Long) {
        perEntry.remove(entryId)?.forEach { it.cancel() }
    }

    /**
     * Cancels every cancelable across every entry. Used when the host itself is
     * disposed (e.g. the composable leaves composition / the activity is destroyed).
     */
    fun cancelAll() {
        // Copy keys first so cancelAll(id) safely mutates the underlying map.
        perEntry.keys.toList().forEach(::cancelAll)
    }

    /** For tests: how many cancelables are currently registered (sum across entries). */
    internal fun size(): Int = perEntry.values.sumOf { it.size }
}
