package widget

import com.google.common.collect.ImmutableList
import data.GStreamerData
import edu.wpi.first.shuffleboard.api.prefs.Group
import edu.wpi.first.shuffleboard.api.prefs.Setting
import edu.wpi.first.shuffleboard.api.widget.Description
import edu.wpi.first.shuffleboard.api.widget.ParametrizedController
import edu.wpi.first.shuffleboard.api.widget.SimpleAnnotatedWidget
import java.net.URI
import java.net.URISyntaxException
import javafx.beans.property.BooleanProperty
import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.embed.swing.SwingFXUtils
import javafx.fxml.FXML
import javafx.scene.Node
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import org.fxmisc.easybind.EasyBind
import source.GStreamerSource
import source.GStreamerSourceType.forURI

@Description(name = "GStreamer", dataTypes = [GStreamerData::class])
@ParametrizedController("GStreamerWidget.fxml")
class GStreamerWidget : SimpleAnnotatedWidget<GStreamerData>() {
    @FXML
    private lateinit var root: Pane
    @FXML
    private lateinit var imageContainer: Pane
    @FXML
    private lateinit var imageView: ImageView
    @FXML
    private lateinit var emptyImage: Image
    @FXML
    private lateinit var controls: Pane
    @FXML
    private lateinit var crosshairs: Node

    private val showControls = SimpleBooleanProperty(this, "showControls", true)
    private val showCrosshair = SimpleBooleanProperty(this, "showCrosshair", true)
    private val crosshairColor = SimpleObjectProperty(this, "crosshairColor", Color.WHITE)
    private val uriField = SimpleStringProperty(this, "uriField", "rtsp://localhost")

    private val sourceURIListener = ChangeListener { _: ObservableValue<out URI>?, _: URI?, uri: URI -> uriField.value = uri.toASCIIString() }

    @FXML
    private fun initialize() {
        imageView.imageProperty().bind(EasyBind.map<GStreamerData, Image>(dataOrDefault) { (_, image) ->
            if (image == null) {
                return@map emptyImage
            }
            SwingFXUtils.toFXImage(image, null)
        })
        sourceProperty().addListener { _, old, source ->
            if (source is GStreamerSource) {
                source.uriProperty.addListener(sourceURIListener)
                if (source.hasClients()) {
                    uriField.value = source.uriProperty.value.toASCIIString()
                }
            }
            if (old is GStreamerSource) {
                old.uriProperty.removeListener(sourceURIListener)
            }
        }
        uriField.addListener { _, _, source ->
            if (sourceProperty().value is GStreamerSource) {
                val gst = sourceProperty().value as GStreamerSource
                if (gst.uriProperty.value.toASCIIString() != source) {
                    try {
                        sourceProperty().value = forURI(URI(source))
                    } catch (ignored: URISyntaxException) {
                    }
                }
            } else {
                try {
                    sourceProperty().value = forURI(URI(source))
                } catch (ignored: URISyntaxException) {
                }
            }
        }
        if (sourceProperty().value is GStreamerSource) {
            val gstSource = sourceProperty().value as GStreamerSource
            uriField.value = gstSource.uriProperty.value.toASCIIString()
        }
    }

    override fun getSettings(): List<Group> {
        return ImmutableList.of(
                Group.of("Crosshair",
                        Setting.of("Show crosshair", showCrosshair, Boolean::class.java),
                        Setting.of("Crosshair color", crosshairColor, Color::class.java)
                ),
                Group.of("URI",
                        Setting.of("URI", uriField, String::class.java)
                )
        )
    }

    override fun getView(): Pane {
        return root
    }

    fun isShowControls(): Boolean {
        return showControls.get()
    }

    fun showControlsProperty(): BooleanProperty {
        return showControls
    }

    fun setShowControls(showControls: Boolean) {
        this.showControls.set(showControls)
    }

    fun isShowCrosshair(): Boolean {
        return showCrosshair.get()
    }

    fun showCrosshairProperty(): BooleanProperty {
        return showCrosshair
    }

    fun setShowCrosshair(showCrosshair: Boolean) {
        this.showCrosshair.set(showCrosshair)
    }

    fun getCrosshairColor(): Color {
        return crosshairColor.value
    }

    fun crosshairColorProperty(): Property<Color> {
        return crosshairColor
    }

    fun setCrosshairColor(crosshairColor: Color) {
        this.crosshairColor.value = crosshairColor
    }

    fun setUri(uri: String?) {
        uriField.value = uri
    }
}
