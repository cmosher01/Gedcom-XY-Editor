/*
 *     Copyright © 2018-2026, Christopher Alan Mosher, New York, New York, USA, <cmosher01@gmail.com>.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
import nu.mine.mosher.gedcom.*;
import nu.mine.mosher.gedcom.xy.util.*;
import org.slf4j.*;

import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

// TODO add Tooltips
@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "StringConcatenationArgumentToLogCall"})
public class Indi {
    private static final Logger LOG = LoggerFactory.getLogger(Indi.class);

    public static final CornerRadii CORNERS = new CornerRadii(4.0D);
    private static final double PARENT_OFFSET_X = 8.0D;
    private static final double PARENT_OFFSET_Y = 8.0D;
    private static final BorderWidths borderWidthsSelected = new BorderWidths(3,3,3,3);
    private static final Point2D NO_POINT = new Point2D(Double.NaN, Double.NaN);

    private final GedcomIndiName nameParsed;

    private Metrics metrics;
    private ColorScheme colors;
    private ObjectBinding<Color> fillBinding;
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
        this.nameParsed = GedcomIndiName.create(n);
        this.lifespan = lifespan;

        this.nBirthForSort = nBirthForSort;
        this.tagline = Optional.ofNullable(tagline).orElse("");
    }

    public void setItemsFromChart(final Selection selection) {
        this.selection = selection;
    }

    public void setMetrics(final Metrics metrics) {
        this.metrics = metrics;
        this.colors = this.metrics.colors();
        this.fillBinding = Bindings.createObjectBinding(() -> selected.get() ? colors.indiSelText() : colors.indiText(), selected);

    }

    public void addGraphicsTo(final List<Node> addto) {
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

    public void addAsChildTo(final Fami famiChildTo) {
        this.rFamiChildTo.add(famiChildTo);
    }



    public void calc() {
        final var labelNameG0 = createTextNode(this.nameParsed.given0());
        labelNameG0.setFont(this.metrics.getFontBold());
        final var labelNameS = createTextNode(this.nameParsed.sur());
        final var labelNameG1 = createTextNode(this.nameParsed.given1());

        final var textFlow = new TextFlow();
        addNamesToFlow(this.nameParsed.tokenized(), labelNameG0, labelNameS, labelNameG1, textFlow);

        final var labelLifespan = new Text();
        {
            labelLifespan.fillProperty().bind(fillBinding);
            if (!this.lifespan.isBlank()) {
                labelLifespan.setText("\n" + this.lifespan);
            }
            labelLifespan.setFont(this.metrics.getFontSmall());
        }
        final var labelTagline = new Text();
        {
            labelTagline.fillProperty().bind(fillBinding);
            if (!this.tagline.isBlank()) {
                labelTagline.setText("\n" + this.tagline);
            }
            labelTagline.setFont(this.metrics.getFontSmall());
        }
        textFlow.getChildren().addAll(labelLifespan, labelTagline);

        textFlow.setMaxWidth(this.metrics.getWidthMax());
        textFlow.setTextAlignment(TextAlignment.CENTER);
        StackPane.setMargin(textFlow, new Insets(this.metrics.getFontSize() / 2.0D));




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

        this.plaque.layoutXProperty().bind(x().subtract(this.plaque.widthProperty().divide(2.0D)));
        this.plaque.layoutYProperty().bind(y().subtract(this.plaque.heightProperty().divide(2.0D)));

        this.plaque.getChildren().addAll(textFlow);




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
            assert !ptOrig.equals(NO_POINT);
            dumpEvent("dragged", ptOrig, ptCanvas);

            selection.drag(pt, ptCanvas);
            // don't consume event, so status bar gets updated by scroller event handler
        });
        this.plaque.setOnMouseReleased(t -> {
            final var pt = new Point2D(t.getX(), t.getY());
            final var ptCanvas = plaque.localToParent(pt);
            final var ptOrig = dragOrig.get();
            assert !ptOrig.equals(NO_POINT);
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

    private void addNamesToFlow(
        final List<GedcomIndiName.Token> tokenized,
        final Text labelNameG0, final Text labelNameS, final Text labelNameG1,
        final TextFlow textFlow)
    {
        for (final var t : tokenized) {
            switch (t) {
                case NULL -> {}
                case UNKNOWN -> textFlow.getChildren().add(createTextNode("?"));
                case SPACE -> textFlow.getChildren().add(createTextNode(" "));
                case GIVEN0 -> textFlow.getChildren().add(labelNameG0);
                case SUR -> textFlow.getChildren().add(labelNameS);
                case GIVEN1 -> textFlow.getChildren().add(labelNameG1);
            }
        }
    }

    private Text createTextNode(final String s) {
        final var node = new Text();
        node.fillProperty().bind(this.fillBinding);
        node.setText(s);
        node.setFont(this.metrics.getFont());
        return node;
    }

    private void dumpEvent(final String event, final Point2D ptOrig, final Point2D ptCurrent) {
//        final var magnitude = Math.abs(ptCurrent.subtract(ptOrig).magnitude());
//        System.out.printf("selection: %8s (%7.1f,%7.1f)->(%7.1f,%7.1f) [%7.1f]\n",
//            event, ptOrig.getX(), ptOrig.getY(), ptCurrent.getX(), ptCurrent.getY(), magnitude);
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

    public String getId() {
        return this.id;
    }

    public boolean intersects(double x, double y, double w, double h) {
        return this.plaque.getBoundsInParent().intersects(x,y,w,h);
    }

    public int getSex() {
        return this.sex;
    }

    public String nameSimple() {
        return this.nameParsed.simple();
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

    public void savePdf(final PdfBuilder pdf) {
        pdf.addPerson(bounds(), this.nameParsed, this.lifespan, this.tagline, this.id);
    }

    public void saveSvg(final SvgBuilder svg) {
        svg.addPerson(bounds(), this.nameParsed, this.lifespan, this.tagline, this.id);
    }

    public void saveXyToTree() {
        final String xy = Coords.toValueXY(this.coords.get());
        final Optional<TreeNode<GedcomLine>> existingXyNode = findChild(this.node, "_XY");
        final TreeNode<GedcomLine> newNode = new TreeNode<>(this.node.getObject().createChild("_XY", xy));
        if (existingXyNode.isPresent()) {
            final TreeNode<GedcomLine> oldNode = existingXyNode.get();
            if (this.coords.original().isPresent()) {
                oldNode.setObject(oldNode.getObject().replaceValue(xy));
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
        LOG.warn(String.format("discarding,\"%s\",\"_XY %.2f %.2f\"", this.nameParsed.simple(), coords.getX(), coords.getY()));
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

    public String getTagline() {
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

    public String nameIdent() {
        return this.nameSimple()+" "+this.lifespan;
    }
}
