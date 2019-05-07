import com.sun.jna.platform.win32.Kernel32
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
import java.io.File

@Description(group = "homebrew", name = "GStreamer Plugin", version = "0.0.0", summary = "GStreamer in Shuffleboard")
@Requires(group = "edu.wpi.first.shuffleboard", name = "NetworkTables", minVersion = "2.2.5")
class GStreamerPlugin : Plugin() {
    override fun onLoad() {
        val codeSource = GStreamerPlugin::class.java.protectionDomain.codeSource
        val jarFile = File(codeSource.location.toURI().path)
        val jarDir = jarFile.parentFile.path

        val k32 = Kernel32.INSTANCE
        val path = System.getenv("path")
        k32.SetEnvironmentVariable("path", "$jarDir\\gstreamer_bins\\bin${if (path == null || path.isBlank()) "" else File.pathSeparator + path.trim()}")
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
