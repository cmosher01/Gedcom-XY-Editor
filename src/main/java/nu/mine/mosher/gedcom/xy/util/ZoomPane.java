package nu.mine.mosher.gedcom.xy.util;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;


/**
 * From Stack Overflow.
 * Copyright © 2017, by Dániel Hári, haridaniel0@gmail.com, Budapest, Hungary.
 * Changes copyright © 2018, by Christopher A. Mosher, cmosher01@gmailcom, Shelton, Connecticut, USA.
 */
public final class ZoomPane extends ScrollPane {
    private static final double ZOOM_INTENSITY = 0.005D;

    private final Node target;
    private final Node zoomNode;

    private double scaleValue = 1.0D;
    private boolean scrolled = false;



    public ZoomPane(final Node target) {
        this.target = target;
        this.zoomNode = new Group(target);

        this.addEventFilter(MouseEvent.MOUSE_DRAGGED, t -> {
            if (t.getTarget() == this.target) {
                this.scrolled = true;
            }
        });

        final VBox content = new VBox(this.zoomNode);
        content.setAlignment(Pos.CENTER);
        content.setOnScroll(t -> {
            t.consume();
            onScroll(t.getDeltaY(), new Point2D(t.getX(), t.getY()));
        });
        setContent(content);

        setPannable(true);
        setHbarPolicy(ScrollBarPolicy.ALWAYS);
        setVbarPolicy(ScrollBarPolicy.ALWAYS);
        setFitToHeight(true);
        setFitToWidth(true);

        updateScale();
    }



    public boolean consumeScroll() {
        final boolean s = this.scrolled;
        this.scrolled = false;
        return s;
    }



    private void updateScale() {
        this.target.setScaleX(this.scaleValue);
        this.target.setScaleY(this.scaleValue);
    }

    private void onScroll(final double wheelDelta, final Point2D mousePoint) {
        final double zoomFactor = Math.exp(wheelDelta * ZOOM_INTENSITY);

        final Bounds innerBounds = this.zoomNode.getLayoutBounds();
        final Bounds viewportBounds = getViewportBounds();

        // calculate pixel offsets from [0, 1] range
        final double valX = this.getHvalue() * (innerBounds.getWidth() - viewportBounds.getWidth());
        final double valY = this.getVvalue() * (innerBounds.getHeight() - viewportBounds.getHeight());

        this.scaleValue *= zoomFactor;
        updateScale();
        this.layout(); // refresh ScrollPane scroll positions & target bounds

        // convert target coordinates to zoomTarget coordinates
        final Point2D posInZoomTarget = this.target.parentToLocal(this.zoomNode.parentToLocal(mousePoint));

        // calculate adjustment of scroll position (pixels)
        final Point2D adjustment = this.target.getLocalToParentTransform().deltaTransform(posInZoomTarget.multiply(zoomFactor - 1));

        // convert back to [0, 1] range
        // (too large/small values are automatically corrected by ScrollPane)
        final Bounds updatedInnerBounds = this.zoomNode.getBoundsInLocal();
        this.setHvalue((valX + adjustment.getX()) / (updatedInnerBounds.getWidth() - viewportBounds.getWidth()));
        this.setVvalue((valY + adjustment.getY()) / (updatedInnerBounds.getHeight() - viewportBounds.getHeight()));
    }
}
