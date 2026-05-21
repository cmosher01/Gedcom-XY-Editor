package nu.mine.mosher.gedcom.xy;

import javafx.geometry.*;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.text.Font;

public class StatusBar extends Pane {
    private final Label labelMouseViewport = new Label("window=()");
    private final Label labelMouseVpToCv = new Label("canvas=()");

    public StatusBar() {
        configureCoordinateLabel(this.labelMouseViewport);
        configureCoordinateLabel(this.labelMouseVpToCv);

        final var layout = new HBox(this.labelMouseViewport, this.labelMouseVpToCv);
        super.getChildren().add(layout);
    }

    public void updateViewPort(final Point2D coords) {
        this.labelMouseViewport.setText(displayCoords("window", coords));
    }

    public void updateVpToCv(final Point2D coords) {
        this.labelMouseVpToCv.setText(displayCoords("canvas", coords));
    }


    private static void configureCoordinateLabel(final Label label) {
        label.setPadding(new Insets(5.0D));
        label.setFont(Font.font("monospace"));
    }

    private static String displayCoords(String name6chars, Point2D coords) {
        return String.format("  %6s=(%7.1f,%7.1f)", name6chars, coords.getX(), coords.getY());
    }
}
