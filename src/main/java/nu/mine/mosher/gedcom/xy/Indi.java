package nu.mine.mosher.gedcom.xy;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
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
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import nu.mine.mosher.collection.TreeNode;
import nu.mine.mosher.gedcom.GedcomLine;
import nu.mine.mosher.gedcom.date.DatePeriod;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.OptionalDouble;

public class Indi {
    public static final CornerRadii CORNERS = new CornerRadii(4.0D);
    private final DatePeriod death;
    private final String name;

    private Metrics metrics;
    private final TreeNode<GedcomLine> node;
    private final String id;
    private Point2D coordsOrig;
    private final int sex;
    private final DatePeriod birth;

    private final Circle center = new Circle(0, Color.TRANSPARENT);
    private final StackPane plaque = new StackPane();

    private boolean wasSelected = false;
    private boolean wasDragged = false;
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

    public void shiftOrig(final double dx, final double dy) {
        this.coordsOrig = new Point2D(this.coordsOrig.getX() - dx, this.coordsOrig.getY() - dy);
    }

    public void select(final boolean select) {
        this.selected.setValue(select);
    }


    public Indi(final TreeNode<GedcomLine> node, final Point2D coords, String id, String name, DatePeriod birth, DatePeriod death, String refn, final int sex) {
        this.node = node;
        this.id = id;
        this.coordsOrig = coords;
        this.sex = sex;
        this.birth = birth;
        this.death = death;
        this.name = name;
    }

    public void calc() {
        final Text textshape = new Text();
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

        final Background bgNormal = new Background(new BackgroundFill(Color.CORNSILK, CORNERS, Insets.EMPTY));
        final Background bgSelected = new Background(new BackgroundFill(Color.ORANGE, CORNERS, Insets.EMPTY));
        final ObjectBinding<Background> bgBinding = new ObjectBinding<Background>() {
            {
                super.bind(selected);
            }

            @Override
            protected Background computeValue() {
                return selected.get() ? bgSelected : bgNormal;
            }
        };

        this.plaque.backgroundProperty().bind(bgBinding);
        this.plaque.setBorder(new Border(new BorderStroke(Color.OLIVEDRAB, BorderStrokeStyle.SOLID, CORNERS, BorderWidths.DEFAULT)));
        StackPane.setMargin(textshape, new Insets(inset));
        this.plaque.getChildren().addAll(textshape);

        this.plaque.layoutXProperty().bind(this.center.layoutXProperty().subtract(w / 2.0D));
        this.plaque.layoutYProperty().bind(this.center.layoutYProperty().subtract(h / 2.0D));

        this.plaque.setOnMouseEntered(t -> {
            t.consume();
            plaque.setCursor(Cursor.HAND);
        });
        this.plaque.setOnMouseExited(t -> {
            t.consume();
            plaque.setCursor(Cursor.DEFAULT);
        });

        this.plaque.setOnMousePressed(t -> {
            t.consume();
            plaque.setCursor(Cursor.MOVE);
            if (selected.get()) {
                wasSelected = true;
            }
            selection.select(this, true);
            selection.beginDrag(new Point2D(t.getX(), t.getY()));
        });
        this.plaque.setOnMouseDragged(t -> {
            t.consume();
            wasDragged = true;
            selection.drag(new Point2D(t.getX(), t.getY()));
        });
        this.plaque.setOnMouseReleased(t -> {
            t.consume();
            plaque.setCursor(Cursor.HAND);
            if (wasSelected && !wasDragged) {
                selection.select(this, false);
            }
            wasSelected = false;
            wasDragged = false;
        });
        this.plaque.setOnMouseClicked(Event::consume);
    }

    public void drag(final Point2D delta) {
        center.setLayoutX(center.getLayoutX() + delta.getX());
        center.setLayoutY(center.getLayoutY() + delta.getY());
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

    public Point2D getCoordsOrig() {
        return this.coordsOrig;
    }

    public void setFromCoords() {
        this.center.relocate(this.coordsOrig.getX(), this.coordsOrig.getY());
    }

    public Circle getCircle() {
        return this.center;
    }

    public void setOrigCoords(double x, double y) {
        this.coordsOrig = new Point2D(x,y);
    }

    public  void setSelection(FamilyChart.Selection selection) {
        this.selection = selection;
    }
}
