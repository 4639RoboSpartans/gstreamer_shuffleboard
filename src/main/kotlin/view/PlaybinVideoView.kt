package view

import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.nio.IntBuffer
import java.util.concurrent.locks.ReentrantLock

import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.ImageView
import org.freedesktop.gstreamer.Element
import org.freedesktop.gstreamer.elements.AppSink
import java.nio.ByteOrder
import org.freedesktop.gstreamer.Caps
import org.freedesktop.gstreamer.FlowReturn
import java.awt.image.IndexColorModel

class PlaybinVideoView
@JvmOverloads constructor(private val videosink: AppSink = AppSink("GstVideoComponent")) : ImageView() {
    var currentImage: BufferedImage = BufferedImage(1, 1, IndexColorModel.OPAQUE)
        private set
    private val bufferLock = ReentrantLock()

    val element: Element
        get() = videosink

    init {
        videosink.set("emit-signals", true)
        val listener = AppSinkListener()
        videosink.connect(listener as AppSink.NEW_SAMPLE)
        videosink.connect(listener as AppSink.NEW_PREROLL)
        val caps = StringBuilder("video/x-raw,pixel-aspect-ratio=1/1,")

        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            caps.append("format=BGRx")
        } else {
            caps.append("format=xRGB")
        }
        videosink.caps = Caps(caps.toString())
    }

    private fun allocateImage(width: Int, height: Int): BufferedImage {
        if (currentImage.width != width || currentImage.height != height) {
            currentImage.flush()
            currentImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
            currentImage.accelerationPriority = 0.0f
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
                image = SwingFXUtils.toFXImage(renderImage, null)
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
