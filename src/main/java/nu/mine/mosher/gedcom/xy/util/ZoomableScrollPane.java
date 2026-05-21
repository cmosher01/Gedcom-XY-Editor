package nu.mine.mosher.gedcom.xy.util;

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Scale;

// NOT USED
// Experiment with using a Scale transform to implement zooming
public class ZoomableScrollPane extends ScrollPane {
//    private static final double ZOOM_INTENSITY = 1.0e-3D;
    private static final double ZOOM_INTENSITY = 1.0e-1D;

    private final Pane canvas;
    private final Group scrollable;

    private final Scale xformZoom = new Scale();

    public ZoomableScrollPane(final Pane canvas) {
        this.canvas = canvas;

        this.scrollable = new Group(this.canvas);
        this.scrollable.getTransforms().add(this.xformZoom);
        setContent(this.scrollable);

        setFitToWidth(true);
        setFitToHeight(true);

        setHbarPolicy(ScrollBarPolicy.ALWAYS);
        setVbarPolicy(ScrollBarPolicy.ALWAYS);

        setPannable(true);
    }

    private void zoomAroundPivot(final double zoom, final Point2D pivot) {
        final var zoomOld = this.xformZoom.getX();
        final var zoomNew = zoom * zoomOld;

        if (zoomNew < 1e-2D || 1e+2D < zoomNew) {
            // limit of zoom in/out
            System.out.println("hit min/max zoom, ignoring");
            System.out.println("----------");
            return;
        }

        if (Math.abs(zoomNew-zoomOld) < 1e-5D) {
            System.out.println("too small change in zoom factor, ignoring");
            System.out.println("----------");
            return;
        }

        this.xformZoom.setX(zoomNew);
        this.xformZoom.setY(zoomNew);

        this.xformZoom.setPivotX(pivot.getX());
        this.xformZoom.setPivotY(pivot.getY());
    }

    private void updateScale(final double scale) {
        // ???
        this.xformZoom.setX(ZOOM_INTENSITY * this.xformZoom.getX());
        this.xformZoom.setY(ZOOM_INTENSITY * this.xformZoom.getY());
    }

    private void setZoomTo(final Point2D pt) {
        this.xformZoom.setPivotX(pt.getX());
        this.xformZoom.setPivotY(pt.getY());
    }
}
