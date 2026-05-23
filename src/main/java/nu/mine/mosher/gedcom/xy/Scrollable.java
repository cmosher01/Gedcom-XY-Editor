package nu.mine.mosher.gedcom.xy;

import javafx.geometry.*;

public interface Scrollable {
    void scrollTo(Point2D hv);

    void scaleTo(double scale);
    void scaleTo();

    void scaleToFit(Bounds boundsChart);

    Point2D center();
}
