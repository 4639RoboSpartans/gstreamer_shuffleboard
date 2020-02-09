package source

import edu.wpi.first.networktables.EntryNotification
import edu.wpi.first.networktables.NetworkTable
import edu.wpi.first.networktables.NetworkTableEntry
import edu.wpi.first.shuffleboard.api.properties.AsyncProperty
import java.io.Closeable
import java.net.URI
import javafx.beans.property.ReadOnlyProperty

class StreamDiscoverer(publisherTable: NetworkTable, cameraName: String) : Closeable {
    private val streams: NetworkTableEntry

    private val urlsProperty = AsyncProperty(this, "urlsProperty", emptyURIArray)
    private val listenerHandle: Int

    val urls: Array<URI>
        get() = urlsProperty.get()

    init {
        streams = publisherTable.getSubTable(cameraName).getEntry(STREAMS_KEY)

        val arr = streams.getStringArray(emptyStringArray).map { URI(it) }.toTypedArray()
        urlsProperty.value = arr

        listenerHandle = streams.addListener({ this.updateUrls(it) }, 0xFF)
    }

    fun urlsProperty(): ReadOnlyProperty<Array<URI>> = urlsProperty

    override fun close() {
        streams.removeListener(listenerHandle)
    }

    private fun updateUrls(notification: EntryNotification) {
        val arr = notification.getEntry().getStringArray(emptyStringArray).map { URI(it) }.toTypedArray()
        urlsProperty.value = arr
    }

    companion object {
        private const val STREAMS_KEY = "streams"
        @JvmStatic
        private val emptyURIArray = arrayOf<URI>()
        @JvmStatic
        private val emptyStringArray = arrayOf<String>()
    }
}
