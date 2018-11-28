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
 * Changes copyright © 2018, by Christopher A. Mosher, cmosher01@gmail.com, Shelton, Connecticut, USA.
 */
public final class ZoomPane extends ScrollPane {
    private static final double ZOOM_INTENSITY = 0.005D;

    private final Node target;
    private final Node zoomNode;

    private double scaleValue = 1.0D;



    public ZoomPane(final Node target) {
        this.target = target;
        this.zoomNode = new Group(target);

        final VBox content = new VBox(this.zoomNode);
        content.setAlignment(Pos.CENTER);
        content.setOnScroll(t -> {
            onScroll(Math.exp(ZOOM_INTENSITY * t.getDeltaY()), new Point2D(t.getX(), t.getY()));
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



    private void updateScale() {
        this.target.setScaleX(this.scaleValue);
        this.target.setScaleY(this.scaleValue);
    }

    private void onScroll(final double zoomFactor, final Point2D mousePoint) {
        // calculate pixel offsets from [0, 1] range
        final double valX = getHvalue() * (this.zoomNode.getLayoutBounds().getWidth() - getViewportBounds().getWidth());
        final double valY = getVvalue() * (this.zoomNode.getLayoutBounds().getHeight() - getViewportBounds().getHeight());

        this.scaleValue *= zoomFactor;
        updateScale();
        layout(); // refresh ScrollPane scroll positions & target bounds

        // convert target coordinates to zoomTarget coordinates
        final Point2D posInZoomTarget = this.target.parentToLocal(this.zoomNode.parentToLocal(mousePoint));

        // calculate adjustment of scroll position (pixels)
        final Point2D adjustment = this.target.getLocalToParentTransform().deltaTransform(posInZoomTarget.multiply(zoomFactor - 1));

        // convert back to [0, 1] range
        // (too large/small values are automatically corrected by ScrollPane)
        this.setHvalue((valX + adjustment.getX()) / (this.zoomNode.getBoundsInLocal().getWidth() - getViewportBounds().getWidth()));
        this.setVvalue((valY + adjustment.getY()) / (this.zoomNode.getBoundsInLocal().getHeight() - getViewportBounds().getHeight()));
    }
}
