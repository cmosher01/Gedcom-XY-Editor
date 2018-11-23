package nu.mine.mosher.gedcom.xy.util;

import java.awt.geom.Dimension2D;

/**
 * Why doesn't Java have this built in?
 */
public class Dim2D extends Dimension2D {
    private double w, h;

    public Dim2D(final double width, final double height) {
        this.w = width;
        this.h = height;
    }

    @Override
    public double getWidth() {
        return this.w;
    }

    @Override
    public double getHeight() {
        return this.h;
    }

    @Override
    public void setSize(final double width, final double height) {
        this.w = width;
        this.h = height;
    }
}
