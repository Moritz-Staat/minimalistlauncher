package de.moritzstaat.launcher.data.settings

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Keeps the font the user picked inside the app's own files.
 *
 * The document the picker returns is only readable while the permission grant lasts; copying
 * it once means the launcher still has its font after a reboot.
 */
class FontStore(private val context: Context) {

    private val directory: File
        get() = File(context.filesDir, DIRECTORY)

    /**
     * Copies the document to a new file and drops every earlier one.
     *
     * The name carries a timestamp on purpose: Compose caches loaded fonts by file, so writing
     * a different font to the same path would keep showing the old letters.
     *
     * @return the absolute path of the stored copy, or null when the document is unreadable.
     */
    fun store(uri: Uri): String? {
        directory.mkdirs()
        val target = File(directory, PREFIX + System.currentTimeMillis())
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use(input::copyTo)
            } ?: return null
            clearExcept(target)
            target.absolutePath
        }.getOrElse {
            target.delete()
            null
        }
    }

    fun clear() = clearExcept(null)

    private fun clearExcept(keep: File?) {
        directory.listFiles()
            ?.filter { it.name.startsWith(PREFIX) && it != keep }
            ?.forEach { it.delete() }
    }

    private companion object {
        const val DIRECTORY = "fonts"
        const val PREFIX = "custom-font-"
    }
}
