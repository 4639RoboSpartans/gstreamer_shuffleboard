package source

import edu.wpi.first.networktables.EntryListenerFlags
import edu.wpi.first.networktables.EntryNotification
import edu.wpi.first.networktables.NetworkTable
import edu.wpi.first.networktables.NetworkTableEntry
import edu.wpi.first.networktables.NetworkTableType
import edu.wpi.first.shuffleboard.api.properties.AsyncProperty
import edu.wpi.first.shuffleboard.api.util.BitUtils
import javafx.beans.property.ReadOnlyProperty
import java.io.Closeable
import java.net.URI

class StreamDiscoverer(publisherTable: NetworkTable, cameraName: String) : Closeable {
    private val streams: NetworkTableEntry

    private val urlsProperty = AsyncProperty<Array<URI>>(this, "urlsProperty", emptyURIArray)
    private val listenerHandle: Int

    val urls: Array<URI>
        get() = urlsProperty.get()

    init {
        streams = publisherTable.getSubTable(cameraName).getEntry(STREAMS_KEY)
        listenerHandle = streams.addListener({ this.updateUrls(it) }, 0xFF)
    }

    fun urlsProperty(): ReadOnlyProperty<Array<URI>> = urlsProperty

    override fun close() {
        streams.removeListener(listenerHandle)
    }

    private fun updateUrls(notification: EntryNotification) {
        if (BitUtils.flagMatches(notification.flags, EntryListenerFlags.kDelete) || notification.getEntry().type != NetworkTableType.kStringArray) {
            urlsProperty.setValue(emptyURIArray)
        } else {
            val arr = notification.getEntry().getStringArray(emptyStringArray).map { URI(it) }.toTypedArray()
            urlsProperty.setValue(arr)
        }
    }

    companion object {
        private const val STREAMS_KEY = "streams"
        @JvmStatic
        private val emptyURIArray = arrayOf<URI>()
        @JvmStatic
        private val emptyStringArray = arrayOf<String>()
    }
}
