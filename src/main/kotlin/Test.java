import java.io.File;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.freedesktop.gstreamer.Element;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.elements.PlayBin;
import org.freedesktop.gstreamer.lowlevel.GObjectAPI;
import org.freedesktop.gstreamer.lowlevel.GstAPI;
import view.PlaybinVideoView;

public class Test extends Application {
    private static volatile Thread main;

    public static void main(String[] args) {
        Kernel32 k32 = Kernel32.INSTANCE;
        String path = System.getenv("path");
        k32.SetEnvironmentVariable("path", "gstreamer_bins\\bin" + (path == null || path.trim().equals("") ? "" : File.pathSeparator + path.trim()));

        Gst.init("GstPlayBin2Test", args);
        launch("a.sdp");
    }

    @Override
    public void start(final Stage primaryStage) throws Exception {


        final StackPane root = new StackPane();
        final PlaybinVideoView vc=  new PlaybinVideoView();

        root.getChildren().add(vc);
        vc.fitWidthProperty().bind(primaryStage.widthProperty());
        vc.fitHeightProperty().bind(primaryStage.heightProperty());

        final Scene scene = new Scene(root, 640, 480);

        primaryStage.setTitle("GStreamer");
        primaryStage.setScene(scene);
        primaryStage.show();

        main = new Thread(() -> {
            try {
                URI uri = new URI("rtsp://192.168.1.222:5800/test");
                PlayBin playbin2 = new PlayBin("GstPlayBin2Test", uri);
                playbin2.set("latency", 0);
                playbin2.setVideoSink(vc.getElement());
                playbin2.connect("notify::gsource", Object.class, null, new NothingCallBack());
                playbin2.play();
                Gst.main();
                playbin2.stop();
            } catch (Exception exception) {
                System.exit(1);
            }
        });
        main.start();
    }

    @Override
    public void stop() throws Exception {
        main.interrupt();
    }

    static class NothingCallBack implements GstAPI.GstCallback {
		public void callback(Element element, GObjectAPI.GParamSpec spec, Pointer user_data) {
		}
	}

}