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
import org.freedesktop.gstreamer.FlowReturn
import org.freedesktop.gstreamer.elements.PlayBin
import edu.wpi.first.shuffleboard.api.DashboardMode
import edu.wpi.first.shuffleboard.api.properties.AsyncValidatingProperty
import javafx.beans.property.Property
import javafx.beans.value.ChangeListener
import org.freedesktop.gstreamer.Caps
import java.net.URI
import java.nio.ByteOrder

class GStreamerSource : AbstractDataSource<GStreamerData> {
    val uriProperty: Property<URI> = AsyncValidatingProperty<URI>(this, "uriProperty", URI("rtsp:")) {
        it.scheme == "rtsp"
    }
        @JvmName("uriProperty") get
    private val videoSink: AppSink = AppSink("GstVideoComponent")
    private val bufferLock = ReentrantLock()
    private val enabled = active.and(connected)

    private lateinit var playBin: PlayBin
    private var uriSource: GStreamerURISource
    private var streaming: Boolean = false

//    private val urlUpdateDebouncer = Debouncer(Runnable {
//    }, ofMillis(10))
//
//    private val cameraUrlUpdater = { _ -> urlUpdateDebouncer.run() }

    private val enabledListener: ChangeListener<Boolean> = ChangeListener { _, _, cur ->
        if (cur) {
            enable()
        } else {
            disable()
        }
    }

    private val curURIListener: ChangeListener<URI> = ChangeListener { _, _, uri ->
        if (!this::playBin.isInitialized) {
            playBin = PlayBin("GStreamerPlayBin", uri)
            uriProperty.value = uri
            playBin.set("latency", 0)
            playBin.setVideoSink(videoSink)
            playBin.play()
        } else if (uriProperty.value != uri) {
            playBin.stop()
            playBin.setURI(uri)
            playBin.play()
            uriProperty.value = uri
        }
        playBin.setURI(uri)
    }

    constructor(name: String) : super(GStreamerDataType) {
        val urlChangeListener: ChangeListener<Array<URI>> = ChangeListener { _, _, urls ->
            if (urls.isEmpty()) {
                isActive = false
            } else {
                uriProperty.value = urls[0]
                isActive = true
            }
        }

        setName(name)
        uriSource = NetworkTablesURISource(name, urlChangeListener)

        if (uriSource.urls.isNotEmpty()) {
            uriProperty.value = uriSource.urls[0]
        }
    }

    constructor(uri: URI) : super(GStreamerDataType) {
        uriSource = EntryURISource(uri)
        uriProperty.value = uriSource.urls[0]
    }

    init {
        setData(GStreamerDataType.defaultValue)

        videoSink.set("emit-signals", true)
        val listener = AppSinkListener()
        videoSink.connect(listener as AppSink.NEW_SAMPLE)
        videoSink.connect(listener as AppSink.NEW_PREROLL)
        val capsString = StringBuilder("video/x-raw,pixel-aspect-ratio=1/1,")

        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            capsString.append("format=BGRx")
        } else {
            capsString.append("format=xRGB")
        }
        videoSink.caps = Caps(capsString.toString())

        DashboardMode.currentModeProperty().addListener { _, _, mode ->
            if (mode != DashboardMode.PLAYBACK) {
                enable()
                playBin.play()
            } else {
                disable()
                playBin.pause()
            }
        }

        enabled.addListener(enabledListener)
    }

    override fun getType(): SourceType = GStreamerSourceType

    override fun close() {
        playBin.remove(videoSink)
        playBin.stop()
        videoSink.stop()
        playBin.close()
        videoSink.close()
        (uriSource as? NetworkTablesURISource)?.close()
        enabled.removeListener(enabledListener)
    }

    private fun enable() {
        val streamUrls = uriSource.urls
        isActive = streamUrls.isNotEmpty()
        streaming = true
        (uriSource as? NetworkTablesURISource)?.enable()
    }

    private fun disable() {
        isActive = false
        streaming = false
        (uriSource as? NetworkTablesURISource)?.disable()
    }

    private fun allocateImage(width: Int, height: Int): BufferedImage =
            BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

    private inner class AppSinkListener : AppSink.NEW_SAMPLE, AppSink.NEW_PREROLL {
        fun rgbFrame(width: Int, height: Int, rgb: IntBuffer) {
            if (!bufferLock.tryLock()) {
                return
            }

            try {
                val renderImage = allocateImage(width, height)
                val pixels = (renderImage.raster.dataBuffer as DataBufferInt).data
                rgb.get(pixels, 0, width * height)
                setData(getData().copy(image = renderImage))
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
