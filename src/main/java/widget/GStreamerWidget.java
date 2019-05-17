package widget;

import data.GStreamerData;
import edu.wpi.first.shuffleboard.api.prefs.Group;
import edu.wpi.first.shuffleboard.api.widget.Description;
import edu.wpi.first.shuffleboard.api.widget.ParametrizedController;
import edu.wpi.first.shuffleboard.api.widget.SimpleAnnotatedWidget;
import edu.wpi.first.shuffleboard.api.prefs.Setting;

import com.google.common.collect.ImmutableList;

import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import org.fxmisc.easybind.EasyBind;
import source.GStreamerSource;

import java.net.URI;
import java.util.List;

@Description(name = "GStreamer", dataTypes = GStreamerData.class)
@ParametrizedController("GStreamerWidget.fxml")
public class GStreamerWidget extends SimpleAnnotatedWidget<GStreamerData> {
  @FXML
  private Pane root;
  @FXML
  private Pane imageContainer;
  @FXML
  private ImageView imageView;
  @FXML
  private Image emptyImage;
  @FXML
  private Pane controls;
  @FXML
  private Node crosshairs;

  private final BooleanProperty showControls = new SimpleBooleanProperty(this, "showControls", true);
  private final BooleanProperty showCrosshair = new SimpleBooleanProperty(this, "showCrosshair", true);
  private final Property<Color> crosshairColor = new SimpleObjectProperty<>(this, "crosshairColor", Color.WHITE);
  private StringProperty uriField = new SimpleStringProperty(this, "uriField", "rtsp:");

  private final ChangeListener<URI> sourceURIListener =
          (__, ___, val) -> uriField.setValue(val.toASCIIString());

  @FXML
  private void initialize() {
    imageView.imageProperty().bind(EasyBind.map(dataOrDefault, n -> {
      if(n.getImage() == null) {
        return emptyImage;
      }
      return SwingFXUtils.toFXImage(n.getImage(), null);
    }));

    sourceProperty().addListener((__, old, source) -> {
      if (source instanceof GStreamerSource) {
        GStreamerSource gstSource = (GStreamerSource) source;
        if (source.hasClients()) {
          uriField.setValue(gstSource.uriProperty().getValue().toASCIIString());
        }
        gstSource.uriProperty().addListener(sourceURIListener);
      }
      if (old instanceof GStreamerSource) {
        GStreamerSource gstSource = (GStreamerSource) old;
        gstSource.uriProperty().removeListener(sourceURIListener);
      }
    });
  }

  @Override
  public List<Group> getSettings() {
    return ImmutableList.of(
        Group.of("Crosshair",
            Setting.of("Show crosshair", showCrosshair, Boolean.class),
            Setting.of("Crosshair color", crosshairColor, Color.class)
        ),
        Group.of("URI",
            Setting.of("URI", uriField, String.class)
        )
    );
  }

  @Override
  public Pane getView() {
    return root;
  }

  public boolean isShowControls() {
    return showControls.get();
  }

  public BooleanProperty showControlsProperty() {
    return showControls;
  }

  public void setShowControls(boolean showControls) {
    this.showControls.set(showControls);
  }

  public boolean isShowCrosshair() {
    return showCrosshair.get();
  }

  public BooleanProperty showCrosshairProperty() {
    return showCrosshair;
  }

  public void setShowCrosshair(boolean showCrosshair) {
    this.showCrosshair.set(showCrosshair);
  }

  public Color getCrosshairColor() {
    return crosshairColor.getValue();
  }

  public Property<Color> crosshairColorProperty() {
    return crosshairColor;
  }

  public void setCrosshairColor(Color crosshairColor) {
    this.crosshairColor.setValue(crosshairColor);
  }

  public StringProperty getUriField() {
    return uriField;
  }

  public void setUri(String uri) {
    this.uriField.setValue(uri);
  }
}
