package source

import data.GStreamerData

import data.GStreamerDataType
import edu.wpi.first.shuffleboard.api.sources.AbstractDataSource
import edu.wpi.first.shuffleboard.api.sources.SourceType
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.nio.IntBuffer
import java.util.concurrent.locks.ReentrantLock

import org.freedesktop.gstreamer.elements.AppSink
import java.nio.ByteOrder
import org.freedesktop.gstreamer.Caps
import org.freedesktop.gstreamer.FlowReturn
import org.freedesktop.gstreamer.elements.PlayBin
import edu.wpi.first.networktables.NetworkTableInstance
import javafx.beans.value.ChangeListener
import edu.wpi.first.shuffleboard.api.DashboardMode

class GStreamerSource
constructor(name: String) : AbstractDataSource<GStreamerData>(GStreamerDataType) {
    private val publisherTable = NetworkTableInstance.getDefault().getTable("/CameraPublisher")
    private val videoSink: AppSink = AppSink("GstVideoComponent")
    private val currentImage: BufferedImage?
        get() = getData().image
    private val bufferLock = ReentrantLock()

    private val enabled = active.and(connected)

    private val streamDiscoverer: StreamDiscoverer

    private var playBin: PlayBin? = null
    private var curUrl: String = ""
    private var streaming: Boolean = false

    private val enabledListener: ChangeListener<Boolean> = ChangeListener { _, _, cur ->
        if (cur) {
            enable()
        } else {
            disable()
        }
    }

    private val urlChangeListener: ChangeListener<Array<String>> = ChangeListener { _, _, urls ->
        if (urls.isEmpty()) {
                isActive = false
    } else {
        if (curUrl != urls[0]) {
            playBin?.remove(videoSink)
            playBin?.close()
            playBin = null
            curUrl = ""
        }

        if (playBin == null) {
            playBin = PlayBin(urls[0])
            curUrl = urls[0]
            playBin!!.set("latency", 0)
            playBin!!.setVideoSink(videoSink)
        }
        isActive = true
    }
}

init {
    setName(name)
    setData(GStreamerDataType.defaultValue)

//    videoSink.set("emit-signals", true)
//        val listener = AppSinkListener()
//        videoSink.connect(listener as AppSink.NEW_SAMPLE)
//        videoSink.connect(listener as AppSink.NEW_PREROLL)
//        val capsString = StringBuilder("video/x-raw,pixel-aspect-ratio=1/1,")
//
//        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
//            capsString.append("format=BGRx")
//        } else {
//            capsString.append("format=xRGB")
//        }
//        videoSink.caps = Caps(capsString.toString())
//
        streamDiscoverer = StreamDiscoverer(publisherTable, name)
//        streamDiscoverer.urlsProperty().addListener(urlChangeListener)
//
//        val urls = streamDiscoverer.urls
//        if (urls.isNotEmpty()) {
//            playBin = PlayBin(urls[0])
//            curUrl = urls[0]
//            playBin!!.set("latency", 0)
//            playBin!!.setVideoSink(videoSink)
//        }
//
//        DashboardMode.currentModeProperty().addListener { _, _, mode ->
//            if (mode != DashboardMode.PLAYBACK) {
//                enable()
//                playBin?.play()
//            } else {
//                disable()
//                playBin?.pause()
//            }
//        }
//
//        enabled.addListener(enabledListener)
    }

    override fun getType(): SourceType = GStreamerSourceType

    override fun close() {
        playBin?.remove(videoSink)
        playBin?.close()
        videoSink.close()
        streamDiscoverer.close()
        enabled.removeListener(enabledListener)
    }

    private fun enable() {
        val streamUrls = streamDiscoverer.urls
        isActive = streamUrls.isNotEmpty()
        streaming = true
        streamDiscoverer.urlsProperty().addListener(urlChangeListener)
    }

    private fun disable() {
        isActive = false
        streaming = false
        streamDiscoverer.urlsProperty().removeListener(urlChangeListener)
    }

    private fun allocateImage(width: Int, height: Int): BufferedImage {
        if (currentImage != null) {
            currentImage!!.flush()

            if (currentImage!!.width != width && currentImage!!.height != height) {
                setData(getData().copy(image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)))
            }
            getData().image!!.accelerationPriority = 0.0f
        } else {
            setData(getData().copy(image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)))
        }

        return currentImage!!
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
