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
import java.io.File
import org.apache.commons.lang3.SystemUtils
import org.freedesktop.gstreamer.Gst
import source.GStreamerSourceType
import widget.GStreamerWidget

@Description(group = "homebrew", name = "GStreamer Plugin", version = "0.1.1", summary = "GStreamer in Shuffleboard")
@Requires(group = "edu.wpi.first.shuffleboard", name = "NetworkTables", minVersion = "2.2.5")
class GStreamerPlugin : Plugin() {
    override fun onLoad() {
        if (SystemUtils.IS_OS_WINDOWS) {
            val k32 = Kernel32.INSTANCE
            val arch = if (SystemUtils.OS_ARCH.contains("64")) "X86_64" else "X86"

            val prefix = System.getenv("GSTREAMER_1_0_ROOT_$arch")
            val path = System.getenv("path")
            if (path == null || path.isBlank()) {
                k32.SetEnvironmentVariable("path", java.lang.String.join(File.pathSeparator, "$prefix\\bin", "$prefix\\lib"))
            } else {
                k32.SetEnvironmentVariable("path", java.lang.String.join(File.pathSeparator, "$prefix\\bin", "$prefix\\lib", path.trim()))
            }
        }
        Gst.init("GStreamerPlugin", "")
    }

    override fun getComponents(): List<ComponentType<out Component>> =
        listOf(WidgetType.forAnnotatedWidget(GStreamerWidget::class.java))

    override fun getDefaultComponents(): Map<DataType<out Any>, ComponentType<out Component>> =
            mapOf(GStreamerDataType to WidgetType.forAnnotatedWidget(GStreamerWidget::class.java))

    override fun getSourceTypes(): List<SourceType> =
        listOf(GStreamerSourceType)

    override fun getDataTypes(): List<DataType<out Any>> =
        listOf(GStreamerDataType)
}
