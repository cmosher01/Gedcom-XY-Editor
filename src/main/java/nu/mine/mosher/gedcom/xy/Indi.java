package nu.mine.mosher.gedcom.xy;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.Event;
import javafx.geometry.*;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import nu.mine.mosher.collection.TreeNode;
import nu.mine.mosher.gedcom.GedcomLine;
import nu.mine.mosher.gedcom.xy.util.SvgBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;
import java.util.regex.*;

public class Indi {
    private static final Logger LOG = LoggerFactory.getLogger(Indi.class);

    public static final CornerRadii CORNERS = new CornerRadii(4.0D);

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

    private final StackPane plaque = new StackPane();

    private boolean wasSelected = false;
    private final BooleanProperty selected = new SimpleBooleanProperty(this, "selected", false);
    private FamilyChart.Selection selection;


    public int getSex() {
        return this.sex;
    }

    public void setMetrics(final Metrics metrics) {
        this.metrics = metrics;
    }

    public void addGraphicsTo(List<Node> addto) {
        addto.add(this.plaque);
    }

    public void select(final boolean select) {
        this.selected.setValue(select);
    }


    public Indi(final TreeNode<GedcomLine> node, final Optional<Point2D> wxyOriginal, String id, String idCoords, String name, String lifespan, final long nBirthForSort, String tagline, final int sex) {
        this.node = node;
        this.id = id;
        this.idCoords = Objects.nonNull(idCoords) ? idCoords : "";
        this.coords = new Coords(wxyOriginal, name);
        this.sex = sex;
        this.name = name;
        this.lifespan = lifespan;
        this.nBirthForSort = nBirthForSort;
        this.nameGiven = parseNameGiven(name);
        this.nameSur = parseNameSur(name);
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

    public void calc() {
        final Text textshape = new Text();
        final ObjectBinding<Color> fillBinding = new ObjectBinding<>()
        {
            {
                super.bind(selected);
            }

            @Override
            protected Color computeValue()
            {
                return selected.get() ? metrics.colorIndiSelText() : metrics.colorIndiText();
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

        final Background bgNormal = new Background(new BackgroundFill(metrics.colorIndiBg(), CORNERS, Insets.EMPTY));
        final Background bgSelected = new Background(new BackgroundFill(metrics.colorIndiSelBg(), CORNERS, Insets.EMPTY));
        this.plaque.backgroundProperty().bind(Bindings.when(selected).then(bgSelected).otherwise(bgNormal));

        final Border borderNormal = new Border(new BorderStroke(metrics.colorIndiBorder(), BorderStrokeStyle.SOLID, CORNERS, BorderWidths.DEFAULT));
        final Border borderDirty = new Border(new BorderStroke(metrics.colorIndiDirtyBorder(), BorderStrokeStyle.SOLID, CORNERS, BorderWidths.DEFAULT));
        this.plaque.borderProperty().bind(Bindings.when(this.coords.propertyDirty()).then(borderDirty).otherwise(borderNormal));

        StackPane.setMargin(textshape, new Insets(inset));
        this.plaque.getChildren().addAll(textshape);

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

        this.plaque.setOnMousePressed(t -> {
            plaque.setCursor(Cursor.MOVE);
            if (selected.get()) {
                wasSelected = true;
            }
            selection.select(this, true, true);
            selection.beginDrag(new Point2D(t.getX(), t.getY()));
            t.consume();
        });
        this.plaque.setOnMouseDragged(t -> {
            selection.drag(new Point2D(t.getX(), t.getY()));
            t.consume();
        });
        this.plaque.setOnMouseReleased(t -> {
            plaque.setCursor(Cursor.HAND);
            if (wasSelected && t.isStillSincePress()) {
                selection.select(this, false, true);
            }
            wasSelected = false;
            t.consume();
        });
        this.plaque.setOnMouseClicked(Event::consume);
    }

    public void drag(final Point2D delta) {
        this.coords.dragTo(snap(this.coords.xyUser().add(delta)));
    }

    private Point2D snap(final Point2D p) {
        return new Point2D(snap(p.getX()), snap(p.getY()));
    }

    private double snap(final double c) {
        final int grid = metrics.grid();
        if (grid == 0) {
            return c;
        }
        return Math.rint(Math.floor(c / grid) * grid);
    }

    private String buildLabel() {
        final StringBuilder label = new StringBuilder(32);

        if (!this.name.isEmpty()) {
            label.append(this.name);
        } else {
            label.append("?");
        }

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

    public String getId() {
        return this.id;
    }

    public  void setSelection(FamilyChart.Selection selection) {
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

    public void fillMissing(final Point2D coordsTopLeftAfterLayout) {
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

    public void saveSvg(final SvgBuilder svg) {
        final String dates =
            this.lifespan.isBlank()
                ? ""
                : ("("+this.lifespan+")");

        final Bounds bounds = this.plaque.getBoundsInParent();
        svg.addPerson(bounds, this.nameGiven, this.nameSur, dates, this.tagline, this.id);
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

    public void userNormalize(Point2D coordsTopLeft) {
        this.coords.forceDirty(true);
        this.coords.normalize(coordsTopLeft);
    }

    public DoubleProperty x() {
        return this.coords.x();
    }

    public DoubleProperty y() {
        return this.coords.y();
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

    public String getTagline()
    {
        return this.tagline;
    }
}
