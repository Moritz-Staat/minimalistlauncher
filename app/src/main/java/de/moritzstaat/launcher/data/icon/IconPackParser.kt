package de.moritzstaat.launcher.data.icon

import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.InputStream
import javax.xml.parsers.SAXParserFactory

/**
 * Reads an icon pack's appfilter.xml.
 *
 * Packs ship it in one of two shapes: compiled into res/xml, which only the Android XML pull
 * parser can read, or as a plain file in assets. Both funnel into [IconPackFilterBuilder], so
 * the rules are shared and the assets path is unit testable on a plain JVM.
 */
object IconPackParser {

    /** Reads a plain XML stream, as found in an icon pack's assets folder. */
    fun parse(input: InputStream): IconPackFilter {
        val builder = IconPackFilterBuilder()
        val factory = SAXParserFactory.newInstance().apply {
            isNamespaceAware = false
            runCatching { setFeature(FEATURE_EXTERNAL_GENERAL_ENTITIES, false) }
            runCatching { setFeature(FEATURE_EXTERNAL_PARAMETER_ENTITIES, false) }
        }
        val handler = object : DefaultHandler() {
            override fun startElement(
                uri: String?,
                localName: String?,
                qName: String?,
                attributes: Attributes?,
            ) {
                val name = (qName ?: localName ?: return).lowercase()
                val values = HashMap<String, String>()
                if (attributes != null) {
                    for (index in 0 until attributes.length) {
                        values[attributes.getQName(index).lowercase()] = attributes.getValue(index)
                    }
                }
                builder.onElement(name, values)
            }
        }
        runCatching { factory.newSAXParser().parse(input, handler) }
        return builder.build()
    }

    /** Reads the compiled res/xml variant through the Android pull parser. */
    fun parse(parser: org.xmlpull.v1.XmlPullParser): IconPackFilter {
        val builder = IconPackFilterBuilder()
        runCatching {
            var event = parser.eventType
            while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                if (event == org.xmlpull.v1.XmlPullParser.START_TAG) {
                    val values = HashMap<String, String>(parser.attributeCount)
                    for (index in 0 until parser.attributeCount) {
                        values[parser.getAttributeName(index).lowercase()] =
                            parser.getAttributeValue(index)
                    }
                    builder.onElement(parser.name.lowercase(), values)
                }
                event = parser.next()
            }
        }
        return builder.build()
    }

    /** Every drawable name a pack advertises, used for the automatic replacement. */
    fun parseDrawableNames(input: InputStream): Set<String> {
        val names = LinkedHashSet<String>()
        val factory = SAXParserFactory.newInstance().apply { isNamespaceAware = false }
        val handler = object : DefaultHandler() {
            override fun startElement(
                uri: String?,
                localName: String?,
                qName: String?,
                attributes: Attributes?,
            ) {
                val value = attributes?.getValue("drawable") ?: return
                if (value.isNotBlank()) names += value
            }
        }
        runCatching { factory.newSAXParser().parse(input, handler) }
        return names
    }

    fun parseDrawableNames(parser: org.xmlpull.v1.XmlPullParser): Set<String> {
        val names = LinkedHashSet<String>()
        runCatching {
            var event = parser.eventType
            while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                if (event == org.xmlpull.v1.XmlPullParser.START_TAG) {
                    for (index in 0 until parser.attributeCount) {
                        if (parser.getAttributeName(index).lowercase() != "drawable") continue
                        val value = parser.getAttributeValue(index)
                        if (!value.isNullOrBlank()) names += value
                    }
                }
                event = parser.next()
            }
        }
        return names
    }

    private const val FEATURE_EXTERNAL_GENERAL_ENTITIES =
        "http://xml.org/sax/features/external-general-entities"
    private const val FEATURE_EXTERNAL_PARAMETER_ENTITIES =
        "http://xml.org/sax/features/external-parameter-entities"
}
