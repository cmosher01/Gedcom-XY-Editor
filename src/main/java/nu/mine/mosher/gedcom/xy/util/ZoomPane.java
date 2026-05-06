package nu.mine.mosher.gedcom.xy.util;

import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import nu.mine.mosher.gedcom.xy.Scrollable;
import nu.mine.mosher.gedcom.xy.shape.Position;


/**
 * From Stack Overflow.
 * Copyright © 2017, by Dániel Hári, haridaniel0@gmail.com, Budapest, Hungary.
 * Changes copyright © 2018, by Christopher A. Mosher, cmosher01@gmail.com, Shelton, Connecticut, USA.
 */
public final class ZoomPane extends ScrollPane implements Scrollable {
    private static final double ZOOM_INTENSITY = 0.004D;

    private final Pane target;
    private final Node zoomNode;

    private double scaleValue = 1.0D;



    public ZoomPane(final Pane target) {
        this.target = target;
        this.zoomNode = new Group(target);

        final VBox content = new VBox(this.zoomNode);
        content.setAlignment(Pos.CENTER);
        content.setOnScroll(t -> {
            zoomTowards(Math.exp(ZOOM_INTENSITY * t.getDeltaY()), new Point2D(t.getX(), t.getY()));
            t.consume();
        });
        setContent(content);

        setPannable(true);
        setHbarPolicy(ScrollBarPolicy.ALWAYS);
        setVbarPolicy(ScrollBarPolicy.ALWAYS);
        setFitToWidth(true);
        setFitToHeight(true);

        updateScale();
    }



    @Override
    public void scrollTo(final Point2D position) {
        final var hv = new Position(
            position.getX()/this.target.getWidth(),
            position.getY()/this.target.getHeight());

        final var p = hv.within(getHmin(), getHmax(), getVmin(), getVmax());

        this.setHvalue(p.h());
        this.setVvalue(p.v());
    }



    private void updateScale() {
        this.target.setScaleX(this.scaleValue);
        this.target.setScaleY(this.scaleValue);
    }

    private void zoomTowards(final double zoom, final Point2D mouse) {
        final var newScale = this.scaleValue * zoom;
        if (newScale < 1e-2D || 1e+2D < newScale) {
            // limit of zoom in/out
            return;
        }

        // calculate pixel offsets from [0, 1] range
        final double h = getHvalue() * (this.zoomNode.getLayoutBounds().getWidth() - getViewportBounds().getWidth());
        final double v = getVvalue() * (this.zoomNode.getLayoutBounds().getHeight() - getViewportBounds().getHeight());

        this.scaleValue = newScale;

        updateScale();
        layout(); // refresh ScrollPane scroll positions & target bounds

        // convert target coordinates to zoomTarget coordinates
        final Point2D mouseLocal = this.target.parentToLocal(this.zoomNode.parentToLocal(mouse))
            .add(40,20); // TODO Fix this: when zooming into mouse position, it's a little off, and the amount varies by tree

        // calculate adjustment of scroll position (pixels)
        final Point2D adjustment = this.target.getLocalToParentTransform().deltaTransform(mouseLocal.multiply(zoom - 1));

        // convert back to [0, 1] range
        // (too large/small values are automatically corrected by ScrollPane)
        setHvalue((h + adjustment.getX()) / (this.zoomNode.getBoundsInLocal().getWidth() - getViewportBounds().getWidth()));
        setVvalue((v + adjustment.getY()) / (this.zoomNode.getBoundsInLocal().getHeight() - getViewportBounds().getHeight()));
    }
}
