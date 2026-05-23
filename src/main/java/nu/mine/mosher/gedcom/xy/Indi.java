package nu.mine.mosher.gedcom.xy;

import javafx.beans.binding.*;
import javafx.beans.property.*;
import javafx.event.Event;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import nu.mine.mosher.collection.TreeNode;
import nu.mine.mosher.gedcom.GedcomLine;
import nu.mine.mosher.gedcom.xy.undo.ModificationTracker;
import nu.mine.mosher.gedcom.xy.util.*;
import org.slf4j.*;

import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.regex.*;

// TODO add Tooltips
public class Indi {
    private static final Logger LOG = LoggerFactory.getLogger(Indi.class);

    public static final CornerRadii CORNERS = new CornerRadii(4.0D);
    private static final double PARENT_OFFSET_X = 8.0D;
    private static final double PARENT_OFFSET_Y = 8.0D;
    private static final BorderWidths borderWidthsSelected = BorderWidths.DEFAULT;// TODO after fix dynamic layout, new BorderWidths(3,3,3,3);
    private static final Point2D NO_POINT = new Point2D(Double.NaN, Double.NaN);

    private final String name;
    private final String nameGiven;
    private final String nameSur;

    private Metrics metrics;
    private final TreeNode<GedcomLine> node;
    private final String id;
    private String idCoords;
    private final Coords coords;
    private final int sex;
    private final long nBirthForSort;
    private final String lifespan;
    private final String tagline;

    private final Pane plaque = new StackPane();

    private boolean wasSelected = false;
    private final BooleanProperty selected = new SimpleBooleanProperty(this, "selected", false);
    private Selection selection;

    private final List<Fami> rFamiSpouseTo = new ArrayList<>();
    private final List<Fami> rFamiChildTo = new ArrayList<>();


    public Indi(final TreeNode<GedcomLine> node, final Optional<Point2D> wxyOriginal, String id, String idCoords, String name, String lifespan, final long nBirthForSort, String tagline, final int sex) {
        final String n = name == null ? "" : name;
        this.node = node;
        this.id = id;
        this.idCoords = Objects.nonNull(idCoords) ? idCoords : "";
        this.coords = new Coords(wxyOriginal, n);
        this.sex = sex;
        this.name = n;
        this.lifespan = lifespan;
        this.nBirthForSort = nBirthForSort;
        this.nameGiven = parseNameGiven(n);
        this.nameSur = parseNameSur(n);
        this.tagline = Optional.ofNullable(tagline).orElse("");
    }

    private static final Pattern PAT_NAME = Pattern.compile("(.*)/([^/]*?)/([^/]*?)");

    private static String parseNameSur(String name) {
        final Matcher matcher = PAT_NAME.matcher(name);
        if (!matcher.matches()) {
            return "";
        }

        return matcher.group(2).trim();
    }

    private static String parseNameGiven(String name) {
        final Matcher matcher = PAT_NAME.matcher(name);
        if (!matcher.matches()) {
            return name.trim();
        }
        final String n1 = matcher.group(1);
        final String n2 = matcher.group(3);
        if (n1.isBlank() && n2.isBlank()) {
            return "";
        }
        if (!n1.isBlank() && n2.isBlank()) {
            return n1.trim();
        }
        if (n1.isBlank() && !n2.isBlank()) {
            return n2.trim();
        }
        return n1.trim()+" ~ "+n2.trim();
    }

    public int getSex() {
        return this.sex;
    }

    public void setMetrics(final Metrics metrics) {
        this.metrics = metrics;
    }

    public void addGraphicsTo(List<Node> addto) {
        addto.add(new Group(this.plaque));
    }

    public void select(final boolean select) {
        this.selected.setValue(select);
        this.rFamiChildTo.forEach(fami -> fami.select(select));
        this.rFamiSpouseTo.forEach(fami -> fami.select(select));
    }



    public void addAsSpouseTo(final Fami famiSpouseTo) {
        this.rFamiSpouseTo.add(famiSpouseTo);
    }

    public void setAsChildTo(final Fami famiChildTo) {
        this.rFamiChildTo.add(famiChildTo);
    }



    public void calc() {
        final ColorScheme colors = this.metrics.colors();

        final Text textshape = new Text();
        final ObjectBinding<Color> fillBinding = new ObjectBinding<>()
        {
            {
                super.bind(selected);
            }

            @Override
            protected Color computeValue()
            {
                return selected.get() ? colors.indiSelText() : colors.indiText();
            }
        };
        textshape.fillProperty().bind(fillBinding);
        textshape.setFont(this.metrics.getFont());
        textshape.setTextAlignment(TextAlignment.CENTER);
        textshape.setText(buildLabel());
        new Scene(new Group(textshape));
        textshape.applyCss();
        if (textshape.getLayoutBounds().getWidth() > this.metrics.getWidthMax()) {
            textshape.setWrappingWidth(this.metrics.getWidthMax());
        }
        final double inset = this.metrics.getFontSize() / 2.0D;
        final double w = textshape.getLayoutBounds().getWidth() + inset * 2.0D;
        final double h = textshape.getLayoutBounds().getHeight() + inset * 2.0D;

        final Background bgNormal = new Background(new BackgroundFill(colors.indiBg(), CORNERS, Insets.EMPTY));
        final Background bgSelected = new Background(new BackgroundFill(colors.indiSelBg(), CORNERS, Insets.EMPTY));
        this.plaque.backgroundProperty().bind(Bindings.when(selected).then(bgSelected).otherwise(bgNormal));

        final Border borderNormal = new Border(new BorderStroke(colors.indiBorder(), BorderStrokeStyle.SOLID, CORNERS, BorderWidths.DEFAULT));
        final Border borderDirty = new Border(new BorderStroke(colors.indiBorderDirty(), BorderStrokeStyle.SOLID, CORNERS, BorderWidths.DEFAULT));
        final Border borderNormalSelected = new Border(new BorderStroke(colors.indiBorder(), BorderStrokeStyle.SOLID, CORNERS, borderWidthsSelected));
        final Border borderDirtySelected = new Border(new BorderStroke(colors.indiBorderDirty(), BorderStrokeStyle.SOLID, CORNERS, borderWidthsSelected));
        this.plaque.borderProperty().bind(
                Bindings.when(this.coords.propertyDirty())
                .then(
                    Bindings.when(selected)
                    .then(borderDirtySelected)
                    .otherwise(borderDirty))
                .otherwise(
                    Bindings.when(selected)
                    .then(borderNormalSelected)
                    .otherwise(borderNormal)));

        StackPane.setMargin(textshape, new Insets(inset));
        this.plaque.getChildren().addAll(textshape);

        // TODO bind these, to allow dynamic sizing if font, border width, etc. change
        this.plaque.layoutXProperty().bind(x().subtract(w / 2.0D));
        this.plaque.layoutYProperty().bind(y().subtract(h / 2.0D));



        this.plaque.setOnMouseEntered(t -> {
            plaque.setCursor(Cursor.HAND);
            t.consume();
        });
        this.plaque.setOnMouseExited(t -> {
            plaque.setCursor(Cursor.DEFAULT);
            t.consume();
        });



        // in canvas coordinates
        final var dragOrig = new AtomicReference<>(NO_POINT); // for debug only

        // note: mouse events sent to the plaque are
        // in PLAQUE coordinates (not canvas coordinates)
        this.plaque.setOnMousePressed(t -> {
            final var pt = new Point2D(t.getX(), t.getY());
            final var ptCanvas = plaque.localToParent(pt);
            dragOrig.set(ptCanvas);
            dumpEvent("pressed", ptCanvas, ptCanvas);

            plaque.setCursor(Cursor.MOVE);
            if (selected.get()) {
                wasSelected = true;
            }
            selection.select(this, true, true);
            selection.beginDrag(pt);

            t.consume();
        });
        this.plaque.setOnMouseDragged(t -> {
            final var pt = new Point2D(t.getX(), t.getY());
            final var ptCanvas = plaque.localToParent(pt);
            final var ptOrig = dragOrig.get();
            assert !ptOrig.equals(NO_POINT); // TODO?
            dumpEvent("dragged", ptOrig, ptCanvas);

            selection.drag(pt);
            // don't consume event, so status bar gets updated by scroller event handler
        });
        this.plaque.setOnMouseReleased(t -> {
            final var pt = new Point2D(t.getX(), t.getY());
            final var ptCanvas = plaque.localToParent(pt);
            final var ptOrig = dragOrig.get();
            assert !ptOrig.equals(NO_POINT); // TODO?
            dumpEvent("released", ptOrig, ptCanvas);

            plaque.setCursor(Cursor.HAND);

            if (wasSelected && t.isStillSincePress()) {
                selection.select(this, false, true);
            }
            wasSelected = false;

            dragOrig.set(NO_POINT);
            selection.endDrag();
            t.consume();
        });
        this.plaque.setOnMouseClicked(Event::consume);
    }

    private void dumpEvent(final String event, final Point2D ptOrig, final Point2D ptCurrent) {
//        final var magnitude = Math.abs(ptCurrent.subtract(ptOrig).magnitude());
//        final var tl = new Point2D(this.boundaryChart.minX(),this.boundaryChart.minY());
//        final var v_tl = this.plaque.getParent().localToParent(tl); // convert from canvas to scroller (not including padding around chart)
//        System.out.printf("selection: %8s (%7.1f,%7.1f)->(%7.1f,%7.1f) [%7.1f]     canvasTopLeftInWindowCoords=(%7.1f,%7.1f)\n",
//            event, ptOrig.getX(), ptOrig.getY(), ptCurrent.getX(), ptCurrent.getY(), magnitude, v_tl.getX(), v_tl.getY());
    }

    public Point2D xyUser() {
        return this.coords.xyUser();
    }

    public void dragWithSnap(final Point2D delta) {
        final var xyUser = this.coords.xyUser();
        final var xy = xyUser.add(delta);
        final var snapped = this.metrics.grid().snap(xy);
//        System.out.printf("dragWithSnap: delta=(%7.1f,%7.1f)  xyUser=(%7.1f,%7.1f)  xy=(%7.1f,%7.1f) snap=(%7.1f,%7.1f)\n",
//                delta.getX(),delta.getY(),xyUser.getX(),xyUser.getY(),xy.getX(),xy.getY(),snapped.getX(),snapped.getY());
        this.coords.dragTo(snapped);
    }

    public void moveTo(final Point2D pt) {
        this.coords.dragTo(pt);
    }

    private String buildLabel() {
        final StringBuilder label = new StringBuilder(32);

        label.append(buildNameForDisplay());

        if (!this.lifespan.isBlank()) {
            label.append("\n(");
            label.append(this.lifespan);
            label.append(")");
        }

        if (!this.tagline.isBlank()) {
            label.append("\n");
            label.append(this.tagline);
        }
        return label.toString();
    }

    private String buildNameForDisplay() {
        if (this.nameGiven.isBlank() && this.nameSur.isBlank()) {
            return "?";
        }
        if (this.nameGiven.isBlank()) {
            return this.nameSur;
        }
        if (this.nameSur.isBlank()) {
            return this.nameGiven;
        }
        return this.nameGiven+" "+this.nameSur;
    }

    public String getId() {
        return this.id;
    }

    public void setItemsFromChart(final Selection selection) {
        this.selection = selection;
    }

    public boolean intersects(double x, double y, double w, double h) {
        return this.plaque.getBoundsInParent().intersects(x,y,w,h);
    }

    private boolean near(double a, double b) {
        return Math.abs(b-a) < .01D;
    }

    public String name() {
        return this.name;
    }

    public long getBirthForSort() {
        return this.nBirthForSort;
    }

    public TreeNode<GedcomLine> node() {
        return this.node;
    }

    public void layOut(final Point2D at) {
        this.coords.layOut(at);
    }

    public boolean hadOriginalXY() {
        return this.coords.original().isPresent();
    }

    public void fillMissingCoords(final Point2D coordsTopLeftAfterLayout) {
        this.coords.fillMissing(coordsTopLeftAfterLayout);
    }

    public Optional<Point2D> laidOut() {
        return this.coords.laidOut();
    }

    public void startCoordTracking() {
        this.coords.start();
    }

    public boolean dirty() {
        return this.coords.dirty();
    }

    public void saveXyToFtm(final Connection conn, final long pkidFactTypeXy) throws SQLException {
        final var syncVersion = FamilyChart.readSyncVersion(conn);
        final String xy = Coords.toValueXY(this.coords.get());
        if (idCoords.isBlank()) {
            LOG.debug(
                "INSERT INTO Fact(LinkID, LinkTableID, FactTypeID, Preferred, Text, SyncVersion) VALUES ({},{},{},{},'{}',{})",
                Long.parseLong(id), 5L, pkidFactTypeXy, 1L, xy, syncVersion) ;
            final String sql = "INSERT INTO Fact(LinkID, LinkTableID, FactTypeID, Preferred, Text, SyncVersion) VALUES (?,?,?,?,?,?)";
            try (final PreparedStatement insert = conn.prepareStatement(sql)) {
                insert.setLong(1, Long.parseLong(id));
                insert.setLong(2, 5L);
                insert.setLong(3, pkidFactTypeXy);
                insert.setLong(4, 1L);
                insert.setString(5, xy);
                insert.setLong(6, syncVersion);
                insert.executeUpdate();

                final ResultSet generatedKeys = insert.getGeneratedKeys();
                if (!generatedKeys.next()) {
                    LOG.error("Could not update internal ID");
                    return;
                }
                this.idCoords = generatedKeys.getString(1);
                this.coords.save();
                if (generatedKeys.next()) {
                    LOG.warn("Database returned multiple IDs when we only expected one.");
                }
            }
        } else {
            LOG.debug("UPDATE Fact SET Text = '{}', SyncVersion = {} WHERE ID = {}",
                xy, syncVersion, Long.parseLong(this.idCoords));
            final String sql = "UPDATE Fact SET Text = ?, SyncVersion = ? WHERE ID = ?";
            try (final PreparedStatement update = conn.prepareStatement(sql)) {
                update.setString(1, xy);
                update.setLong(2, syncVersion);
                update.setLong(3, Long.parseLong(this.idCoords));
                update.executeUpdate();
                this.coords.save();
                LOG.debug("updated {} row(s)", update.getUpdateCount());
            }
        }
        LOG.debug("UPDATE Person SET SyncVersion = {} WHERE ID = {}",
            syncVersion, Long.parseLong(this.id));
        final String sql = "UPDATE Person SET SyncVersion = ? WHERE ID = ?";
        try (final PreparedStatement update = conn.prepareStatement(sql)) {
            update.setLong(1, syncVersion);
            update.setLong(2, Long.parseLong(this.id));
            update.executeUpdate();
            LOG.debug("updated {} row(s)", update.getUpdateCount());
        }
    }

    public void savePdf(final PdfBuilder builder) {
        final String dates =
            this.lifespan.isBlank()
                ? ""
                : ("("+this.lifespan+")");

        builder.addPerson(bounds(), this.nameGiven, this.nameSur, dates, this.tagline, this.id);
    }

    public void saveSvg(final SvgBuilder svg) {
        final String dates =
            this.lifespan.isBlank()
                ? ""
                : ("("+this.lifespan+")");

        svg.addPerson(bounds(), this.nameGiven, this.nameSur, dates, this.tagline, this.id);
    }

    public void saveXyToTree() {
        final String value_XY = Coords.toValueXY(this.coords.get());
        final Optional<TreeNode<GedcomLine>> existingXyNode = findChild(this.node, "_XY");
        final TreeNode<GedcomLine> newNode = new TreeNode<>(this.node.getObject().createChild("_XY", value_XY));
        if (existingXyNode.isPresent()) {
            final TreeNode<GedcomLine> oldNode = existingXyNode.get();
            if (this.coords.original().isPresent()) {
                oldNode.setObject(oldNode.getObject().replaceValue(value_XY));
            } else {
                // This is the case where there was an original _XY record in the GEDCOM
                // file, but it had an invalid format.
                // Leave the existing _XY intact, and add a new _XY record before it
                // (in order to mask the old one without destroying it).
                this.node.addChildBefore(newNode, oldNode);
            }
        } else {
            this.node.addChild(newNode);
        }
        this.coords.save();
    }

    private static Optional<TreeNode<GedcomLine>> findChild(final TreeNode<GedcomLine> parent, final String tag) {
        for (final TreeNode<GedcomLine> child : parent) {
            if (child.getObject().getTagString().equals(tag)) {
                return Optional.of(child);
            }
        }
        return Optional.empty();
    }

    public Point2D coords() {
        return this.coords.get();
    }

    public Optional<Point2D> coordsOriginal() {
        return this.coords.getOriginal();
    }

//    public void userNormalize(Point2D coordsTopLeft) {
//        this.coords.forceDirty(true);
//        this.coords.normalize(coordsTopLeft);
//    }

    public DoubleProperty x() {
        return this.coords.x();
    }

    public DoubleProperty y() {
        return this.coords.y();
    }

    public DoubleBinding xForParent(final Fami fami) {
        return x().add(this.rFamiChildTo.indexOf(fami)*PARENT_OFFSET_X);
    }

    public DoubleBinding yForParent(final Fami fami) {
        return y().subtract(this.rFamiChildTo.indexOf(fami)*PARENT_OFFSET_Y);
    }

    public void logDiscard() {
        final Point2D coords = coords();
        LOG.warn(String.format("discarding,\"%s\",\"_XY %.2f %.2f\"", this.name, coords.getX(), coords.getY()));
    }

    public boolean selected() {
        return this.selected.get();
    }

    public double width() {
        return this.plaque.getWidth();
    }

    public double height() {
        return this.plaque.getHeight();
    }

    public Bounds bounds() {
        return this.plaque.getBoundsInParent();
    }

    public String getTagline()
    {
        return this.tagline;
    }

    /**
     * Gets this person's parent, siblings, spouses, and children.
     * @return list of relatives
     */
    public List<Indi> getRelatives() {
        final var rindi = new ArrayList<Indi>();

        // parents and siblings
        for (final var fami : this.rFamiChildTo) {
            rindi.addAll(fami.getParents());
            rindi.addAll(fami.getChildren());
        }

        // spouses and children
        for (final var fami : this.rFamiSpouseTo) {
            rindi.addAll(fami.getParents());
            rindi.addAll(fami.getChildren());
        }

        return rindi.stream().filter(s -> s != this).toList();
    }

    public double distanceFrom(final Indi other) {
        return this.coords.get().distance(other.coords.get());
    }
}
