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

class StreamDiscoverer(publisherTable: NetworkTable, cameraName: String) : Closeable {
    private val streams: NetworkTableEntry

    private val urlsProperty = AsyncProperty<Array<String>>(this, "urlsProperty", emptyStringArray)
    private val listenerHandle: Int

    val urls: Array<String>
        get() = urlsProperty.get()

    init {
        streams = publisherTable.getSubTable(cameraName).getEntry(STREAMS_KEY)
        listenerHandle = streams.addListener({ this.updateUrls(it) }, 0xFF)
    }

    fun urlsProperty(): ReadOnlyProperty<Array<String>> = urlsProperty

    override fun close() {
        streams.removeListener(listenerHandle)
        urlsProperty.value = emptyStringArray
    }

    private fun updateUrls(notification: EntryNotification) {
        if (BitUtils.flagMatches(notification.flags, EntryListenerFlags.kDelete) || notification.getEntry().type != NetworkTableType.kStringArray) {
            urlsProperty.setValue(emptyStringArray)
        } else {
            val arr = notification.getEntry().getStringArray(emptyStringArray)
            urlsProperty.setValue(arr)
        }
    }

    companion object {
        private const val STREAMS_KEY = "streams"
        @JvmStatic
        private val emptyStringArray = arrayOf<String>()
    }
}
