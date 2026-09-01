package de.moritzstaat.launcher.data.media

/**
 * Where the track stands right now.
 *
 * A media session reports its position once and then stops talking: the number is only true for
 * the instant it was measured. Everything after that has to be extrapolated from the elapsed
 * time and the playback speed, which is why this is pure arithmetic and covered by tests rather
 * than a ticker that reads the controller every second.
 */
object MediaProgress {

    /**
     * @param positionMs the position the session last reported.
     * @param updatedAtMs the clock reading at that moment, on the same clock as [nowMs].
     * @param speed playback speed; 1.0 is normal, 0 while paused.
     * @return elapsed milliseconds, clamped to [durationMs] when that is known.
     */
    fun elapsedMs(
        positionMs: Long,
        durationMs: Long,
        updatedAtMs: Long,
        nowMs: Long,
        speed: Float,
        isPlaying: Boolean,
    ): Long {
        val base = positionMs.coerceAtLeast(0L)
        val advanced = if (isPlaying && speed > 0f) {
            base + ((nowMs - updatedAtMs).coerceAtLeast(0L) * speed).toLong()
        } else {
            base
        }
        return if (durationMs > 0L) advanced.coerceIn(0L, durationMs) else advanced.coerceAtLeast(0L)
    }

    /** 0..1 for the progress bar, or null when the track has no known length. */
    fun fraction(elapsedMs: Long, durationMs: Long): Float? {
        if (durationMs <= 0L) return null
        return (elapsedMs.toFloat() / durationMs).coerceIn(0f, 1f)
    }

    /** "3:07", or "1:02:30" once an hour is on the clock. Negative and unknown give "0:00". */
    fun label(milliseconds: Long): String {
        val total = (milliseconds / 1000L).coerceAtLeast(0L)
        val hours = total / 3600L
        val minutes = (total % 3600L) / 60L
        val seconds = total % 60L
        return if (hours > 0L) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }
}
