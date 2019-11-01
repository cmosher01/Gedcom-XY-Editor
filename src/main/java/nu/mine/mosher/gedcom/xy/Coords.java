package nu.mine.mosher.gedcom.xy;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Point2D;
import javafx.scene.shape.Circle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

import static java.util.Optional.empty;
import static javafx.scene.paint.Color.TRANSPARENT;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "WeakerAccess"})
public final class Coords {
    private static final Logger LOG = LoggerFactory.getLogger(Coords.class);

    private static final Point2D xyMISSING = new Point2D(37, 73);
    private static final double SMALL = 0.51D;

    private final String of;
    private Optional<Point2D> wxyOrig = empty();
    private Optional<Point2D> wxyLayout = empty();
    private Point2D wxyStart;
    private Point2D xyStart;
    private final Circle xyLayoutUser = new Circle(0, TRANSPARENT); // TODO make this a Point2D property
    private boolean forceDirty;
    private BooleanProperty propDirty = new SimpleBooleanProperty();

    private void dumpToLog(final String label) {
        LOG.debug("{}: {},{},{},{},{},{},{},{}", label,
            toDump("wxyOrig", this.wxyOrig),
            toDump("wxyLayout", this.wxyLayout),
            toDump("wxyStart", this.wxyStart),
            toDump("xyStart", this.xyStart),
            toDump("xyLayoutUser", new Point2D(this.xyLayoutUser.getLayoutX(), this.xyLayoutUser.getLayoutY())),
            String.format("mag=%.0f",userMoved().magnitude()),
            (!dirty() ? "-" : this.forceDirty ? "F" : "D"),
            this.of);
    }

    private String toDump(final String name, final Optional<Point2D> p) {
        if (p.isPresent()) {
            return toDump(name, p.get());
        }
        return String.format("%s=()", name);
    }
    private String toDump(final String name, final Point2D p) {
        return String.format("%s=(%.2f,%.2f)", name, p.getX(), p.getY());
    }

    /**
     * Initializes this set of coordinates with the original {@code _XY} value as
     * read in (and parsed into {@code double} values) from the GEDCOM file.
     * If there is no {@code _XY} record for the individual, then pass in {@link Optional#empty()}.
     * @param original {@code _XY} coordinates from GEDCOM file, cannot be {@code null}
     * @param name
     */
    public Coords(final Optional<Point2D> original, final String name) {
        this.wxyOrig = Objects.requireNonNull(original);
        this.wxyLayout = this.wxyOrig;
        this.of = name;
    }

    /**
     * Sets the automatic (synthetic) layout coordinates for the individual.
     * These will be the result of a layout algorithm.
     * @param at coordinates generated for the individual by a layout algorithm, cannot be {@code null}
     */
    public void layOut(final Point2D at) {
        this.wxyLayout = Optional.of(at);
    }

    /**
     * Gets the original or laid out coordinates.
     * @return laid out coordinates, or original if layOut was not called
     */
    public Optional<Point2D> laidOut() {
        return this.wxyLayout;
    }

    /**
     * Warning: this method must always be called.
     * After (optionally) calling {@link Coords#layOut(Point2D)}, call
     * {@link Coords#laidOut()} for all individuals, find the minimum
     * x and y values, and pass them (as a {@link Point2D}) into this method.
     * @param coordsTopLeftAfterLayout min(x) and min(y) of all individuals, cannot be {@code null}.
     */
    public void fillMissing(final Point2D coordsTopLeftAfterLayout) {
        Objects.requireNonNull(coordsTopLeftAfterLayout);
        if (Objects.nonNull(this.xyStart)) {
            throw new IllegalStateException("Cannot call fillMissing more than once on Coords object.");
        }
        this.wxyStart = this.wxyLayout.orElseGet(() -> coordsTopLeftAfterLayout.add(xyMISSING));
        this.xyStart = this.wxyStart.subtract(coordsTopLeftAfterLayout);
        update();
    }

    public Optional<Point2D> getOriginal() {
        return this.wxyOrig;
    }

    /**
     * Gets the x value to bind GUI objects to.
     * @return bindable x
     */
    public DoubleProperty x() {
        return this.xyLayoutUser.layoutXProperty();
    }

    /**
     * Gets the y value to bind GUI objects to.
     * @return bindable y
     */
    public DoubleProperty y() {
        return this.xyLayoutUser.layoutYProperty();
    }

    /**
     * Prepare for user-controlled dragging of the individual, by setting
     * the starting point of the variable to the read-in _XY value (normalized for display)
     */
    public void start() {
        if (Objects.isNull(this.xyStart)) {
            throw new IllegalStateException("Must call fillMissing before calling start on Coords object.");
        }
        dragTo(this.xyStart);
        dumpToLog("start");
    }


    /**
     * Moves this individual to the given point.
     * Does not handle snap-to-grid.
     * @param here absolute position to move to, cannot be {@code null}.
     */
    public void dragTo(final Point2D here) {
        Objects.requireNonNull(here);
        this.xyLayoutUser.relocate(here.getX(), here.getY());
        update();
    }

    private void update() {
        this.propDirty.set(dirty());
    }

    public BooleanProperty propertyDirty() {
        return this.propDirty;
    }

    public Point2D xyUser() {
        return new Point2D(x().get(), y().get());
    }

    /**
     * Returns the (delta) user movement of this individual
     * within the chart (away from its original position)
     * @return (x,y) delta user movement
     */
    public Point2D userMoved() {
        return xyUser().subtract(this.xyStart);
    }

    /**
     * The current value, as indicated by user movements or user normalization.
     * Or the original value otherwise.
     * Save this as the _XY value in the GEDCOM file.
     * @return (x,y) coordinates of current location indicated by user drags
     */
    public Point2D get() {
        return this.wxyStart.add(userMoved());
    }

    /**
     * Indicates a user-initiated normalization of coordinates.
     * @param coordsTopLeft min(x) and min(y) of current positions of all individuals, cannot be {@code null}.
     */
    public void normalize(final Point2D coordsTopLeft) {
        dragTo(xyUser().subtract(Objects.requireNonNull(coordsTopLeft)));
        dumpToLog("normalize");
    }

    /**
     * Forces this object to be considered dirty, or not.
     * @param force force, or not force
     */
    public void forceDirty(final boolean force) {
        this.forceDirty = force;
        update();
    }

    /**
     * Checks whether the user has indicated this individual should be moved
     * (or was automatically laid out), and has not yet been {@link Coords#save()}'d.
     * @return
     */
    public boolean dirty() {
        return
            (SMALL < userMoved().magnitude()) ||
            (!this.wxyOrig.isPresent() && this.wxyLayout.isPresent()) ||
            (this.forceDirty);
    }

    /**
     * After saving the individual (using the value returned by {@link Coords#get()}),
     * call this method to indicate so, marking this object as "not dirty".
     * Also clears the "force dirty" setting.
     */
    public void save() {
        dumpToLog("save before");
        if (dirty()) {
            this.wxyOrig = Optional.of(get());
            this.wxyLayout = this.wxyOrig;
            this.wxyStart = this.wxyLayout.get();
            this.xyStart = xyUser();
            forceDirty(false);
            dumpToLog("save after");
        }
    }

    public Optional<Point2D> original() {
        return this.wxyOrig;
    }
}
