package nu.mine.mosher.gedcom.xy.shape;

import javafx.geometry.Point2D;

public record Translation(double h, double v) {
    public Point2D applyTo(final Point2D p) {
        return p.add(h(), v());
    }
}
