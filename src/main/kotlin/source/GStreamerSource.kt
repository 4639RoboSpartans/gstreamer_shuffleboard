package source

import data.GStreamerData
import data.GStreamerDataType
import edu.wpi.first.shuffleboard.api.sources.AbstractDataSource
import edu.wpi.first.shuffleboard.api.sources.SourceType
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.nio.IntBuffer
import java.util.concurrent.locks.ReentrantLock

import org.freedesktop.gstreamer.Element
import org.freedesktop.gstreamer.elements.AppSink
import java.nio.ByteOrder
import org.freedesktop.gstreamer.Caps
import org.freedesktop.gstreamer.FlowReturn

class GStreamerSource
constructor(name: String) : AbstractDataSource<GStreamerData>(GStreamerDataType) {
    private val videosink: AppSink = AppSink("GstVideoComponent")
    val element: Element
        get() = videosink

    private val currentImage: BufferedImage
        get() = getData().image
    private var frames: Byte = 0
    private val bufferLock = ReentrantLock()

    init {
        setName(name)
        setData(GStreamerDataType.defaultValue)

        videosink.set("emit-signals", true)
        val listener = AppSinkListener()
        videosink.connect(listener as AppSink.NEW_SAMPLE)
        videosink.connect(listener as AppSink.NEW_PREROLL)
        val capsString = StringBuilder("video/x-raw,pixel-aspect-ratio=1/1,")

        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            capsString.append("format=BGRx")
        } else {
            capsString.append("format=xRGB")
        }
        videosink.caps = Caps(capsString.toString())
    }

    override fun getType(): SourceType {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun allocateImage(width: Int, height: Int): BufferedImage {
        if (currentImage.width != width || currentImage.height != height) {
            currentImage.flush()

            setData(getData().copy(image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)))
            getData().image.accelerationPriority = 0.0f
        }

        return currentImage
    }

    private inner class AppSinkListener : AppSink.NEW_SAMPLE, AppSink.NEW_PREROLL {
        fun rgbFrame(width: Int, height: Int, rgb: IntBuffer) {
            if (!bufferLock.tryLock()) {
                return
            }

            try {
                val renderImage = allocateImage(width, height)
                val pixels = (renderImage.raster.dataBuffer as DataBufferInt).data
                rgb.get(pixels, 0, width * height)
            } finally {
                bufferLock.unlock()
            }
        }

        override fun newSample(elem: AppSink): FlowReturn {
            val sample = elem.pullSample()
            val capsStruct = sample.caps.getStructure(0)
            val w = capsStruct.getInteger("width")
            val h = capsStruct.getInteger("height")
            val buffer = sample.buffer
            val bb = buffer.map(false)
            if (bb != null) {
                rgbFrame(w, h, bb.asIntBuffer())
                buffer.unmap()
            }
            sample.dispose()
            return FlowReturn.OK
        }

        override fun newPreroll(elem: AppSink): FlowReturn {
            val sample = elem.pullPreroll()
            val capsStruct = sample.caps.getStructure(0)
            val w = capsStruct.getInteger("width")
            val h = capsStruct.getInteger("height")
            val buffer = sample.buffer
            val bb = buffer.map(false)
            if (bb != null) {
                rgbFrame(w, h, bb.asIntBuffer())
                buffer.unmap()
            }
            sample.dispose()
            return FlowReturn.OK
        }
    }
}
