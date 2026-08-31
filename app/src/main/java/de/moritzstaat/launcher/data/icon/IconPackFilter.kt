package de.moritzstaat.launcher.data.icon

/**
 * The contents of an icon pack's appfilter.xml.
 *
 * [componentToDrawable] maps a flattened component name to the drawable the pack ships for it.
 * The rest is the pack's own recipe for apps it does not cover: a background, a mask, an
 * overlay and a scale factor.
 */
data class IconPackFilter(
    val componentToDrawable: Map<String, String> = emptyMap(),
    val iconBacks: List<String> = emptyList(),
    val iconMask: String? = null,
    val iconUpon: String? = null,
    val scale: Float = 1f,
) {
    val isEmpty: Boolean get() = componentToDrawable.isEmpty() && iconBacks.isEmpty() && iconMask == null
}

/**
 * Collects appfilter entries. Both readers, the compiled res/xml one and the plain assets one,
 * feed into this, so the actual rules live in one tested place.
 */
class IconPackFilterBuilder {

    private val components = LinkedHashMap<String, String>()
    private val iconBacks = ArrayList<String>()
    private var iconMask: String? = null
    private var iconUpon: String? = null
    private var scale: Float = 1f

    fun onElement(name: String, attributes: Map<String, String>) {
        when (name) {
            "item" -> {
                val component = attributes["component"] ?: return
                val drawable = attributes["drawable"] ?: return
                val flattened = parseComponent(component) ?: return
                // First entry wins: packs list the most specific alias first.
                components.putIfAbsent(flattened, drawable)
            }

            "iconback" -> iconBacks += attributes.imageValues()
            "iconmask" -> iconMask = attributes.imageValues().firstOrNull() ?: iconMask
            "iconupon" -> iconUpon = attributes.imageValues().firstOrNull() ?: iconUpon
            "scale" -> scale = attributes["factor"]?.toFloatOrNull() ?: scale
        }
    }

    fun build(): IconPackFilter = IconPackFilter(
        componentToDrawable = components.toMap(),
        iconBacks = iconBacks.toList(),
        iconMask = iconMask,
        iconUpon = iconUpon,
        scale = scale,
    )

    /** iconback and friends use img, img1, img2 ... for their variants. */
    private fun Map<String, String>.imageValues(): List<String> =
        entries.filter { it.key.startsWith("img") }
            .sortedBy { it.key }
            .map { it.value }
            .filter { it.isNotBlank() }

    companion object {
        /**
         * Turns `ComponentInfo{pkg/cls}` into `pkg/cls`. Packs also ship bare `pkg/cls` and
         * the special `:BACK`, `:UPON` and `:MASK` markers, which are not components.
         */
        fun parseComponent(raw: String): String? {
            val trimmed = raw.trim()
            if (trimmed.startsWith(":")) return null
            val inner = if (trimmed.startsWith(COMPONENT_PREFIX) && trimmed.endsWith("}")) {
                trimmed.substring(COMPONENT_PREFIX.length, trimmed.length - 1)
            } else {
                trimmed
            }
            if (!inner.contains('/')) return null
            val (packageName, className) = inner.split('/', limit = 2)
            if (packageName.isBlank() || className.isBlank()) return null
            // Some packs abbreviate an inner class as ".Main"; expand it back.
            val fullClass = if (className.startsWith(".")) packageName + className else className
            return "$packageName/$fullClass"
        }

        private const val COMPONENT_PREFIX = "ComponentInfo{"
    }
}
