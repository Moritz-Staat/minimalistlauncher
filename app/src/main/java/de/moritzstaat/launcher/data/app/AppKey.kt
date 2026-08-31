package de.moritzstaat.launcher.data.app

/**
 * Identifies one launchable activity for one user profile.
 *
 * Persisted as a single flat string so that Room tables and the JSON backup do not have to
 * model three columns everywhere. The separators cannot occur in a package or class name.
 */
data class AppKey(
    val packageName: String,
    val className: String,
    val userSerial: Long,
) {
    fun flatten(): String = "$packageName/$className#$userSerial"

    override fun toString(): String = flatten()

    companion object {
        fun parse(flat: String): AppKey? {
            val hash = flat.lastIndexOf('#')
            if (hash <= 0) return null
            val slash = flat.indexOf('/')
            if (slash <= 0 || slash > hash) return null
            val serial = flat.substring(hash + 1).toLongOrNull() ?: return null
            return AppKey(
                packageName = flat.substring(0, slash),
                className = flat.substring(slash + 1, hash),
                userSerial = serial,
            )
        }
    }
}
