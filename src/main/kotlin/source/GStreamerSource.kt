package source

import data.GStreamerData
import data.GStreamerDataType
import edu.wpi.first.shuffleboard.api.DashboardMode
import edu.wpi.first.shuffleboard.api.properties.AsyncValidatingProperty
import edu.wpi.first.shuffleboard.api.sources.AbstractDataSource
import edu.wpi.first.shuffleboard.api.sources.SourceType
import edu.wpi.first.shuffleboard.api.sources.Sources
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.net.URI
import java.nio.ByteOrder
import java.nio.IntBuffer
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.locks.ReentrantLock
import javafx.beans.property.Property
import javafx.beans.property.ReadOnlyProperty
import javafx.beans.value.ChangeListener
import org.freedesktop.gstreamer.Bus
import org.freedesktop.gstreamer.Caps
import org.freedesktop.gstreamer.FlowReturn
import org.freedesktop.gstreamer.elements.AppSink
import org.freedesktop.gstreamer.elements.PlayBin

class GStreamerSource : AbstractDataSource<GStreamerData> {
    private val uriProperty: Property<URI> = AsyncValidatingProperty(this, "uriProperty", URI("rtsp://localhost")) {
        it.scheme == "rtsp"
    }

    fun uriProperty(): ReadOnlyProperty<URI> = uriProperty
    fun asciiRepresentation() = uriSource.let {
        when (it) {
            is NetworkTablesURISource -> "NetworkTables ${it.name}"
            is StaticURISource -> uriProperty.value.toASCIIString()!!
        }
    }

    private val videoSink: AppSink = AppSink("GstVideoComponent")
    private val bufferLock = ReentrantLock()
    private val enabled = active.and(connected)

    private lateinit var playBin: PlayBin
    private var uriSource: GStreamerURISource

    private val enabledListener: ChangeListener<Boolean> = ChangeListener { _, _, cur ->
//        println("$cur@@@")
        if (cur) {
            enable()
        } else {
            disable()
        }
    }

    private val curURIListener: ChangeListener<URI> = ChangeListener { _, old, uri ->
        if (!this::playBin.isInitialized) {
            GStreamerSourceType.registerURI(this)

            playBin = PlayBin("GStreamerPlayBin", uri)
            playBin.set("latency", 0)

            playBin.bus.connect(Bus.ERROR { _, _, msg ->
                if (msg == "Internal data stream error." || msg == "Could not open resource for reading and writing.") {
                    isConnected = false

                    Timer(true).schedule(
                        object : TimerTask() {
                            override fun run() {
                                enable()
                            }
                        }, 5000)
                }
            })

            playBin.setVideoSink(videoSink)
            enable()
        } else {
            GStreamerSourceType.unregisterURI(old)
            GStreamerSourceType.registerURI(this)

            playBin.stop()
            playBin.setURI(uri)
            playBin.play()
        }
    }

    constructor(name: String) : super(GStreamerDataType) {
        val urlChangeListener: ChangeListener<Array<URI>> = ChangeListener { _, _, urls ->
            if (urls.isEmpty()) {
                isActive = false
            } else {
                uriProperty.value = urls[0]
                enable()
            }
        }

        setName(name)
        uriSource = NetworkTablesURISource(name, urlChangeListener)

        if (uriSource.urls.isNotEmpty()) {
            uriProperty.value = uriSource.urls[0]
        }
    }

    constructor(uri: URI) : super(GStreamerDataType) {
        uriSource = StaticURISource(uri)
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
                disable()
                playBin.pause()
            } else {
                enable()
                playBin.play()
            }
        }

        enabled.addListener(enabledListener)
        uriProperty.addListener(curURIListener)
    }

    override fun getType(): SourceType = GStreamerSourceType

    override fun close() {
        isActive = false
        isConnected = false

        playBin.stop()
        playBin.remove(videoSink)
        videoSink.stop()
        playBin.close()
        videoSink.close()
        (uriSource as? NetworkTablesURISource)?.close()
        enabled.removeListener(enabledListener)

        GStreamerSourceType.removeSource(this)
        GStreamerSourceType.unregisterURI(uriProperty.value)
        Sources.getDefault().unregister(this)
    }

    private fun enable() {
        val streamUrls = uriSource.urls
        if (streamUrls.isNotEmpty()) {
            isActive = true
            playBin.play()
            isConnected = true
        }
        (uriSource as? NetworkTablesURISource)?.enable()
    }

    private fun disable() {
        playBin.stop()
        isActive = false
        (uriSource as? NetworkTablesURISource)?.disable()
    }

    private inner class AppSinkListener : AppSink.NEW_SAMPLE, AppSink.NEW_PREROLL {
        private fun allocateImage(width: Int, height: Int): BufferedImage =
            BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

        private fun rgbFrame(width: Int, height: Int, rgb: IntBuffer) {
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
