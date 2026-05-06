package nu.mine.mosher.gedcom.xy.shape;

/**
 * Horizontal and vertical position, each within range [0,1]
 * @param h
 * @param v
 */
public record Position(double h, double v) {
    public Position within(double hmin, double hmax, double vmin, double vmax) {
        return new Position(
            hmin + h*(hmax-hmin),
            vmin + v*(vmax-vmin));
    }
}
