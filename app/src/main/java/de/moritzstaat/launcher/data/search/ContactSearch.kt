package de.moritzstaat.launcher.data.search

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

/** One contact hit in the search results. */
data class ContactHit(
    val lookupKey: String,
    val displayName: String,
    val photoUri: String?,
) {
    fun contentUri(): Uri =
        Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey)
}

/**
 * Optional contact search. The launcher works fully without the permission; without it this
 * simply reports nothing and the search field shows apps only.
 */
class ContactSearch(context: Context) {

    private val appContext = context.applicationContext

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    fun search(query: String, limit: Int = MAX_RESULTS): List<ContactHit> {
        if (query.length < MIN_QUERY_LENGTH || !hasPermission()) return emptyList()

        val uri = Uri.withAppendedPath(
            ContactsContract.Contacts.CONTENT_FILTER_URI,
            Uri.encode(query),
        )
        val projection = arrayOf(
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
        )

        return runCatching {
            appContext.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${ContactsContract.Contacts.STARRED} DESC, ${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC",
            )?.use { cursor ->
                val hits = ArrayList<ContactHit>(limit)
                val lookupColumn = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY)
                val nameColumn =
                    cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                val photoColumn =
                    cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI)
                while (cursor.moveToNext() && hits.size < limit) {
                    val lookupKey = cursor.getString(lookupColumn) ?: continue
                    val name = cursor.getString(nameColumn) ?: continue
                    hits += ContactHit(lookupKey, name, cursor.getString(photoColumn))
                }
                hits
            }.orEmpty()
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val MIN_QUERY_LENGTH = 2
        const val MAX_RESULTS = 3
    }
}
