package nu.mine.mosher.gedcom.xy;

public interface ChartBoundary {
    double PADDING = 200.0D;

    double minX();
    double minY();
    double maxX();
    double maxY();
}
