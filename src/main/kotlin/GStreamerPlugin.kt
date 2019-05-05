import data.GStreamerDataType
import edu.wpi.first.shuffleboard.api.data.DataType
import edu.wpi.first.shuffleboard.api.plugin.Description
import edu.wpi.first.shuffleboard.api.plugin.Plugin
import edu.wpi.first.shuffleboard.api.plugin.Requires
import edu.wpi.first.shuffleboard.api.sources.SourceType
import edu.wpi.first.shuffleboard.api.widget.Component
import edu.wpi.first.shuffleboard.api.widget.ComponentType
import edu.wpi.first.shuffleboard.api.widget.WidgetType
import org.freedesktop.gstreamer.Gst
import source.GStreamerSourceType
import widget.GStreamerWidget

@Description(group = "com.example", name = "MyPlugin", version = "0.0.0", summary = "An example plugin")
@Requires(group = "edu.wpi.first.shuffleboard", name = "NetworkTables", minVersion = "2.2.5")
class GStreamerPlugin : Plugin() {
    override fun onLoad() {
        Gst.init("GStreamerPlugin", "")
    }

    override fun getComponents(): List<ComponentType<out Component>> =
        listOf(WidgetType.forAnnotatedWidget(GStreamerWidget::class.java))

    override fun getSourceTypes(): List<SourceType> =
        listOf(GStreamerSourceType)

    override fun getDefaultComponents(): Map<DataType<out Any>, ComponentType<out Component>> =
        mapOf(GStreamerDataType to WidgetType.forAnnotatedWidget(GStreamerWidget::class.java))

    override fun getDataTypes(): List<DataType<out Any>> =
        listOf(GStreamerDataType)
}
