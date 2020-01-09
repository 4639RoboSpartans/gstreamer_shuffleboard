package source

import edu.wpi.first.networktables.NetworkTableInstance
import java.io.Closeable
import java.net.URI
import javafx.beans.value.ChangeListener

sealed class GStreamerURISource {
    abstract val urls: Array<URI>
}

class NetworkTablesURISource(name: String, private val changeListener: ChangeListener<Array<URI>>) : GStreamerURISource(), Closeable, AutoCloseable {
    override val urls: Array<URI>
        get() = streamDiscoverer.urls
    private val publisherTable = NetworkTableInstance.getDefault().getTable("/GStreamer")
    private val streamDiscoverer: StreamDiscoverer

    init {
        streamDiscoverer = StreamDiscoverer(publisherTable, name)
        streamDiscoverer.urlsProperty().addListener(changeListener)
    }

    fun enable() {
        streamDiscoverer.urlsProperty().addListener(changeListener)
    }

    fun disable() {
        streamDiscoverer.urlsProperty().removeListener(changeListener)
    }

    override fun close() {
        streamDiscoverer.close()
    }
}

class EntryURISource(url: URI) : GStreamerURISource() {
    override val urls = arrayOf(url)
}
