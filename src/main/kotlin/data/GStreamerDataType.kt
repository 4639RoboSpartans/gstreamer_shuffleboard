package data

import edu.wpi.first.shuffleboard.api.data.ComplexDataType
import javafx.scene.image.Image
import java.awt.image.BufferedImage
import java.util.function.Function

object GStreamerDataType : ComplexDataType<GStreamerData>("GStreamer", GStreamerData::class.java) {
    override fun getDefaultValue(): GStreamerData {
        return GStreamerData("Default", null)
    }

    override fun fromMap(): Function<Map<String, Any>, GStreamerData> =
            Function { map -> GStreamerData(map["name"] as String, map["image"] as BufferedImage) }
}
