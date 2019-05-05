package data

import edu.wpi.first.shuffleboard.api.data.ComplexData
import java.awt.image.BufferedImage

data class GStreamerData(val name: String, val image: BufferedImage) : ComplexData<GStreamerData>() {
    override fun asMap(): MutableMap<String, Any> =
        hashMapOf("name" to name, "image" to image)
}
