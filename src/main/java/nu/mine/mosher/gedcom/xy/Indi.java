package nu.mine.mosher.gedcom.xy;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
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
import nu.mine.mosher.gedcom.date.DatePeriod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class Indi {
    private static final Logger LOG = LoggerFactory.getLogger(Indi.class);

    public static final int XY_SCALE = 2;
    public static final CornerRadii CORNERS = new CornerRadii(4.0D);

    private final DatePeriod death;
    private final String name;

    private Metrics metrics;
    private final TreeNode<GedcomLine> node;
    private final String id;
    private final Coords coords;
    private final int sex;
    private final DatePeriod birth;

    private final StackPane plaque = new StackPane();

    private boolean wasSelected = false;
    private final BooleanProperty selected = new SimpleBooleanProperty(this, "selected", false);
    private FamilyChart.Selection selection;


    public int getSex() {
        return this.sex;
    }

    public DatePeriod getBirth() {
        return this.birth;
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


    public Indi(final TreeNode<GedcomLine> node, final Optional<Point2D> wxyOriginal, String id, String name, DatePeriod birth, DatePeriod death, String refn, final int sex) {
        this.node = node;
        this.id = id;
        this.coords = new Coords(wxyOriginal);
        this.sex = sex;
        this.birth = birth;
        this.death = death;
        this.name = name;
    }

    public void calc() {
        final Text textshape = new Text();
        final ObjectBinding<Color> fillBinding = new ObjectBinding<Color>() {
            {
                super.bind(selected);
            }

            @Override
            protected Color computeValue() {
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
        final double snapped = Math.rint(Math.floor(c / grid) * grid);
        return snapped;
    }

    private String buildLabel() {
        final StringBuilder label = new StringBuilder(32);

        if (!this.name.isEmpty()) {
            label.append(this.name);
        } else {
            label.append("?");
        }

        final String displayBirth = dateString(this.birth);
        final String displayDeath = dateString(this.death);
        if (!displayBirth.isEmpty() || !displayDeath.isEmpty()) {
            label.append("\n");
            label.append(displayBirth.isEmpty() ? "?" : displayBirth);
            label.append('\u2013');
            label.append(displayDeath.isEmpty() ? "?" : displayDeath);
        }

        return label.toString();
    }

    private static String dateString(final DatePeriod date) {
        final Date d = date.getStartDate().getApproxDay().asDate();
        if (d.getTime() == 0) {
            return "";
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        int year = cal.get(Calendar.YEAR);
        return "" + year;
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

    public void saveXyToTree() {
        final String value_XY = toValue_XY(this.coords.get());
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

    private static String toValue_XY(final Point2D xy) {
        return coord(xy.getX())+" "+coord(xy.getY());
    }

    private static String coord(final double coord) {
        return BigDecimal.valueOf(coord).setScale(XY_SCALE, RoundingMode.HALF_DOWN).toPlainString();
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
}
