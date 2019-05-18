package source

import edu.wpi.first.shuffleboard.api.data.DataTypes
import edu.wpi.first.shuffleboard.api.sources.SourceType
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import java.util.HashMap
import data.GStreamerData
import data.GStreamerDataType
import edu.wpi.first.networktables.NetworkTable
import edu.wpi.first.networktables.NetworkTableInstance
import edu.wpi.first.shuffleboard.api.sources.UiHints
import edu.wpi.first.shuffleboard.api.util.FxUtils
import edu.wpi.first.shuffleboard.plugin.networktables.util.NetworkTableUtils
import edu.wpi.first.shuffleboard.api.sources.recording.TimestampedData
import java.net.URI

@UiHints(showConnectionIndicator = false)
object GStreamerSourceType : SourceType("GStreamer", false, "gstreamer://", GStreamerSourceType::forName) {
    init {
        NetworkTableInstance.getDefault().addEntryListener("/GStreamer", { entryNotification ->
            FxUtils.runOnFxThread {
                val hierarchy = NetworkTable.getHierarchy(entryNotification.name)
                // 0 is "/", 1 is "/GStreams", 2 is "/GStreams/<name>"
                val name = NetworkTable.basenameKey(hierarchy[2])
                val uri = toUri(name)
                val table = NetworkTableInstance.getDefault().getTable(hierarchy[2])
                if (table.keys.isEmpty() && table.subTables.isEmpty()) {
                    availableUris.remove(uri)
                    availableSources.remove(uri)
                } else if (!NetworkTableUtils.isDelete(entryNotification.flags)) {
                    if (!availableUris.contains(uri)) {
                        availableUris.add(uri)
                    }
                    availableSources[uri] = GStreamerDataType.defaultValue
                }
            }
        }, 0xFF)
    }
    private val sources = HashMap<String, GStreamerSource>()
    private val uriToSources = HashMap<URI, GStreamerSource>()
    private val availableUris = FXCollections.observableArrayList<String>()
    private val availableSources = FXCollections.observableHashMap<String, Any>()

    override fun getAvailableSources(): ObservableMap<String, Any> = availableSources
    override fun getAvailableSourceUris(): ObservableList<String> = availableUris

    override fun dataTypeForSource(registry: DataTypes?, sourceUri: String?): GStreamerDataType = GStreamerDataType

    fun forName(name: String): GStreamerSource = sources.computeIfAbsent(name, ::GStreamerSource)
    fun forURI(uri: URI): GStreamerSource {
        return if(!uriToSources.containsKey(uri))
            GStreamerSource(uri)
        else
            uriToSources[uri]!!
    }
    fun registerURI(source: GStreamerSource) {
        uriToSources.put(source.uriProperty.value, source)
    }
    fun unregisterURI(uri: URI) {
        uriToSources.remove(uri)
    }
    fun removeSource(source: GStreamerSource) {
        sources.remove(source.name)

    }

    override fun read(recordedData: TimestampedData) {
        super.read(recordedData)
        val source = forUri(recordedData.sourceId) as GStreamerSource
        source.data = recordedData.data as GStreamerData
    }

    override fun connect() {
        super.connect()
        sources.values.forEach(GStreamerSource::connect)
    }

    override fun disconnect() {
        sources.values.forEach(GStreamerSource::disconnect)
        super.disconnect()
    }

    override fun createSourceEntryForUri(uri: String): GStreamerSourceEntry =
            GStreamerSourceEntry(removeProtocol(uri))
}
