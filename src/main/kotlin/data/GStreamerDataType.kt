package data

import edu.wpi.first.shuffleboard.api.data.ComplexDataType
import java.awt.image.BufferedImage
import java.awt.image.IndexColorModel
import java.util.function.Function

object GStreamerDataType : ComplexDataType<GStreamerData>("Ffmpeg", GStreamerData::class.java) {
    override fun getDefaultValue(): GStreamerData {
        return GStreamerData("Default", BufferedImage(1, 1, IndexColorModel.OPAQUE))
    }

    override fun fromMap(): Function<MutableMap<String, Any>, GStreamerData> =
            Function { map -> GStreamerData(map["name"] as String, map["image"] as BufferedImage) }
}
