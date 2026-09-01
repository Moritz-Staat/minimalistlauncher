package de.moritzstaat.launcher.data.backup

/** One hand picked icon, as it appears in the backup file. */
data class BackupIcon(
    val appKey: String,
    val iconPackPackage: String,
    val drawableName: String,
)

/** One folder with the apps in it, in the order the user put them. */
data class BackupFolder(
    val name: String,
    val apps: List<String>,
)

/**
 * Everything worth carrying to a new install: the settings and the hand made decisions.
 *
 * Deliberately not in it: placed widgets, because their ids belong to the `AppWidgetHost` of
 * this one install, and the open counters of the usage breaker, which describe today rather
 * than the setup. Both would restore as rubbish.
 */
data class LauncherBackup(
    val version: Int = VERSION,
    val preferences: Map<String, JsonValue> = emptyMap(),
    val favorites: List<String> = emptyList(),
    val hidden: List<String> = emptyList(),
    val labels: Map<String, String> = emptyMap(),
    val icons: List<BackupIcon> = emptyList(),
    val notificationRedacted: List<String> = emptyList(),
    val folders: List<BackupFolder> = emptyList(),
) {

    fun toJson(): JsonValue = JsonValue.Obj(
        linkedMapOf(
            KEY_VERSION to version.toString().toJson(),
            KEY_PREFERENCES to JsonValue.Obj(LinkedHashMap(preferences)),
            KEY_FAVORITES to favorites.toJson(),
            KEY_HIDDEN to hidden.toJson(),
            KEY_LABELS to JsonValue.Obj(
                LinkedHashMap(labels.mapValues { (_, label) -> label.toJson() }),
            ),
            KEY_ICONS to JsonValue.Arr(
                icons.map { icon ->
                    jsonOf(
                        KEY_APP_KEY to icon.appKey.toJson(),
                        KEY_ICON_PACK to icon.iconPackPackage.toJson(),
                        KEY_DRAWABLE to icon.drawableName.toJson(),
                    )
                },
            ),
            KEY_REDACTED to notificationRedacted.toJson(),
            KEY_FOLDERS to JsonValue.Arr(
                folders.map { folder ->
                    jsonOf(
                        KEY_NAME to folder.name.toJson(),
                        KEY_APPS to folder.apps.toJson(),
                    )
                },
            ),
        ),
    )

    fun encode(): String = JsonWriter.write(toJson())

    companion object {
        /** Raised only when an older file could no longer be read at all. */
        const val VERSION = 1

        const val KEY_VERSION = "version"
        const val KEY_PREFERENCES = "preferences"
        const val KEY_FAVORITES = "favorites"
        const val KEY_HIDDEN = "hidden"
        const val KEY_LABELS = "labels"
        const val KEY_ICONS = "icons"
        const val KEY_REDACTED = "notificationRedacted"
        const val KEY_FOLDERS = "folders"
        const val KEY_APP_KEY = "appKey"
        const val KEY_ICON_PACK = "iconPack"
        const val KEY_DRAWABLE = "drawable"
        const val KEY_NAME = "name"
        const val KEY_APPS = "apps"

        /**
         * Reads a backup file. A missing section restores as empty rather than as an error:
         * half a backup is still worth more than none.
         */
        fun decode(text: String): LauncherBackup? {
            val root = JsonReader.read(text)?.asObject() ?: return null
            if (!root.containsKey(KEY_VERSION)) return null

            return LauncherBackup(
                version = root[KEY_VERSION]?.asString()?.toIntOrNull() ?: VERSION,
                preferences = root[KEY_PREFERENCES]?.asObject().orEmpty(),
                favorites = root[KEY_FAVORITES]?.asStringList().orEmpty(),
                hidden = root[KEY_HIDDEN]?.asStringList().orEmpty(),
                labels = root[KEY_LABELS]?.asObject().orEmpty()
                    .mapNotNull { (key, value) -> value.asString()?.let { key to it } }
                    .toMap(),
                icons = root[KEY_ICONS]?.asArray().orEmpty().mapNotNull { it.toIcon() },
                notificationRedacted = root[KEY_REDACTED]?.asStringList().orEmpty(),
                folders = root[KEY_FOLDERS]?.asArray().orEmpty().mapNotNull { it.toFolder() },
            )
        }

        private fun JsonValue.toIcon(): BackupIcon? {
            val entries = asObject() ?: return null
            return BackupIcon(
                appKey = entries[KEY_APP_KEY]?.asString() ?: return null,
                iconPackPackage = entries[KEY_ICON_PACK]?.asString() ?: return null,
                drawableName = entries[KEY_DRAWABLE]?.asString() ?: return null,
            )
        }

        private fun JsonValue.toFolder(): BackupFolder? {
            val entries = asObject() ?: return null
            return BackupFolder(
                name = entries[KEY_NAME]?.asString() ?: return null,
                apps = entries[KEY_APPS]?.asStringList().orEmpty(),
            )
        }
    }
}
