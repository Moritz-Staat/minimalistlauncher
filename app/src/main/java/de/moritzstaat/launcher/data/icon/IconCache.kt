package de.moritzstaat.launcher.data.icon

import android.graphics.Bitmap
import android.util.LruCache

/**
 * Memory-only icon cache. Icons are never written to disk: they are cheap to rebuild from the
 * package manager and a stale disk cache is a classic source of wrong icons after an update.
 *
 * Sized in bytes so a handful of huge icons cannot push everything else out.
 */
class IconCache(maxBytes: Int = defaultMaxBytes()) {

    private val cache = object : LruCache<String, Bitmap>(maxBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    operator fun get(key: String): Bitmap? = cache.get(key)

    operator fun set(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }

    fun clear() = cache.evictAll()

    /** Called on trim-memory: keep a quarter so scrolling does not stutter right afterwards. */
    fun trim() = cache.trimToSize(cache.maxSize() / 4)

    fun sizeBytes(): Int = cache.size()

    companion object {
        /** Cap at 1/8 of the heap and never above 96 MB, see the budget in stage 17. */
        fun defaultMaxBytes(): Int {
            val heap = Runtime.getRuntime().maxMemory()
            val eighth = (heap / 8).coerceAtMost(HARD_CAP_BYTES)
            return eighth.coerceAtLeast(MIN_BYTES).toInt()
        }

        private const val HARD_CAP_BYTES = 96L * 1024 * 1024
        private const val MIN_BYTES = 8L * 1024 * 1024
    }
}
