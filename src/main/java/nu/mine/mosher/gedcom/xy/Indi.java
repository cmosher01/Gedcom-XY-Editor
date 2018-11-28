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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class Indi {
    public static final CornerRadii CORNERS = new CornerRadii(4.0D);
    private final DatePeriod death;
    private final String name;

    private Metrics metrics;
    private final TreeNode<GedcomLine> node;
    private final String id;
    private Point2D coords;
    private Point2D coordsInit;
    private final int sex;
    private final DatePeriod birth;

    private final Circle center = new Circle(0, Color.TRANSPARENT);
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

    public void shiftOrig(final double dx, final double dy) {
        this.coords = new Point2D(this.coords.getX() - dx, this.coords.getY() - dy);
    }

    public void select(final boolean select) {
        this.selected.setValue(select);
    }


    public Indi(final TreeNode<GedcomLine> node, final Point2D coords, String id, String name, DatePeriod birth, DatePeriod death, String refn, final int sex) {
        this.node = node;
        this.id = id;
        this.coords = coords;
        this.coordsInit = coords;
        this.sex = sex;
        this.birth = birth;
        this.death = death;
        this.name = name;
    }

    public void calc() {
        final Text textshape = new Text();
        final ObjectBinding<Color> fillBinding = new ObjectBinding<>() {
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

        this.plaque.setBorder(new Border(new BorderStroke(metrics.colorIndiBorder(), BorderStrokeStyle.SOLID, CORNERS, BorderWidths.DEFAULT)));
        StackPane.setMargin(textshape, new Insets(inset));
        this.plaque.getChildren().addAll(textshape);

        this.plaque.layoutXProperty().bind(this.center.layoutXProperty().subtract(w / 2.0D));
        this.plaque.layoutYProperty().bind(this.center.layoutYProperty().subtract(h / 2.0D));

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
            selection.select(this, true);
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
                selection.select(this, false);
            }
            wasSelected = false;
            t.consume();
        });
        this.plaque.setOnMouseClicked(Event::consume);
    }

    public void drag(final Point2D delta) {
        center.setLayoutX(snap(center.getLayoutX() + delta.getX()));
        center.setLayoutY(snap(center.getLayoutY() + delta.getY()));
    }

    private double snap(final double c) {
        final int grid = metrics.grid();
        if (grid == 0) {
            return c;
        }
        return Math.rint(Math.floor(c/grid)*grid);
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

    public Point2D getCoords() {
        return this.coords;
    }

    public void setFromCoords() {
        this.center.relocate(this.coords.getX(), this.coords.getY());
    }

    public Circle getCircle() {
        return this.center;
    }

    public void setInitCoords(double x, double y) {
        this.coordsInit = new Point2D(x,y);
    }

    public  void setSelection(FamilyChart.Selection selection) {
        this.selection = selection;
    }

    public boolean intersects(double x, double y, double w, double h) {
        return this.plaque.getBoundsInParent().intersects(x,y,w,h);
    }

    public boolean dirty() {
        return !(near(this.center.getLayoutX(), this.coords.getX()) && near(this.center.getLayoutY(), this.coords.getY()));
    }

    private boolean near(double a, double b) {
        return Math.abs(b-a) < .01D;
    }

    public String name() {
        return this.name;
    }

    public void saveXyToTree() {
        final String xy = newXY();
        System.err.println("changing "+this.name+": "+xy);
        boolean set = false;
        for (final TreeNode<GedcomLine> c : this.node) {
            if (c.getObject().getTagString().equals("_XY")) {
                c.setObject(c.getObject().replaceValue(xy));
                set = true;
            }
        }
        if (!set) {
            this.node.addChild(new TreeNode<>(this.node.getObject().createChild("_XY", xy)));
        }

        this.coords = new Point2D(this.center.getLayoutX(), this.center.getLayoutY());
    }

    private String newXY() {
        double x = this.coordsInit.getX()+(this.center.getLayoutX()-this.coords.getX());
        double y = this.coordsInit.getY()+(this.center.getLayoutY()-this.coords.getY());
        return coord(x)+" "+coord(y);
    }

    private static String coord(final double c) {
        final String sDecimal = BigDecimal.valueOf(c).setScale(2, RoundingMode.HALF_DOWN).toPlainString();
        final String sInteger = BigDecimal.valueOf(c).setScale(0, RoundingMode.HALF_DOWN).toPlainString();
        return sDecimal;
    }

    public TreeNode<GedcomLine> node() {
        return this.node;
    }
}
