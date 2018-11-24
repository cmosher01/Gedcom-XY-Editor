package nu.mine.mosher.gedcom.xy;

import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.*;
import nu.mine.mosher.collection.TreeNode;
import nu.mine.mosher.gedcom.GedcomLine;
import nu.mine.mosher.gedcom.date.DatePeriod;

import java.util.*;

public class Indi {
    public static final double FONT_POINT_SIZE = 8.0D;
    public static final Font FONT = Font.font(Font.getDefault().getName(), FONT_POINT_SIZE);
    public static final CornerRadii CORNERS = new CornerRadii(4.0D);
    public static final double INSET = 3.0D;
    public static final Insets INSETS = new Insets(INSET);

    private final TreeNode<GedcomLine> node;
    private final String id;
    private Point2D coordsOrig;
    private final int sex;
    private final DatePeriod birth;

    private final Circle center;
    private final StackPane plaque;

    public int getSex() {
        return this.sex;
    }

    public DatePeriod getBirth() {
        return this.birth;
    }

    public void addGraphicsTo(List<Node> addto) {
        addto.add(this.center);
        addto.add(this.plaque);
    }

    public void shiftOrig(OptionalDouble x, OptionalDouble y) {
        this.coordsOrig = new Point2D(
            this.coordsOrig.getX() - (x.isPresent() ? x.getAsDouble() : 0.0D),
            this.coordsOrig.getY() - (y.isPresent() ? y.getAsDouble() : 0.0D));
    }

    private static double widthMax = Double.NaN;
    private static double heightNominal = Double.NaN;

    private static synchronized Double widthMax() {
        if (!(widthMax > 0.0D)) {
            final String stringMax = "MMMMMMMMMMMMMMMM";
            final Text text = new Text(stringMax + "\nX");
            text.setFont(FONT);
            new Scene(new Group(text));
            text.applyCss();
            widthMax = text.getLayoutBounds().getWidth();
            heightNominal = text.getLayoutBounds().getHeight();
            System.err.println("Calculated maximum width for person: " + widthMax);
            System.err.println("Calculated nominal height for person: " + heightNominal);
        }
        return widthMax;
    }

    private static synchronized Double heightNominal() {
        if (!(widthMax > 0.0D)) {
            widthMax();
        }
        return heightNominal;
    }

    private javafx.geometry.Point2D dragDelta;

    public Indi(final TreeNode<GedcomLine> node, final Point2D coords, String id, String name, DatePeriod birth, DatePeriod death, String refn, final int sex) {
        this.node = node;
        this.id = id;
        this.coordsOrig = coords;
        this.sex = sex;
        this.birth = birth;
        center = new Circle(0, Color.TRANSPARENT);

        final StringBuilder label = new StringBuilder(23);
        if (!name.isEmpty()) {
            label.append(name);
        } else {
            label.append("?");
        }
        final String displayBirth = dateString(birth);
        final String displayDeath = dateString(death);
        if (!displayBirth.isEmpty() || !displayDeath.isEmpty()) {
            label.append("\n");
            if (!displayBirth.isEmpty()) {
                label.append(displayBirth);
            }
            label.append('\u2013');
            if (!displayDeath.isEmpty()) {
                label.append(displayDeath);
            }
        }

        final Text textshape = new Text();
        textshape.setFont(FONT);
        textshape.setTextAlignment(TextAlignment.CENTER);
        textshape.setText(label.toString());
        new Scene(new Group(textshape));
        textshape.applyCss();
        if (textshape.getLayoutBounds().getWidth() > widthMax()) {
            textshape.setWrappingWidth(widthMax());
        }
        final double w = textshape.getLayoutBounds().getWidth() + INSET * 2.0D;
        final double h = textshape.getLayoutBounds().getHeight() + INSET * 2.0D;

        this.plaque = new StackPane();
        this.plaque.setBackground(new Background(new BackgroundFill(Color.CORNSILK, CORNERS, Insets.EMPTY)));
        this.plaque.setBorder(new Border(new BorderStroke(Color.OLIVEDRAB, BorderStrokeStyle.SOLID, CORNERS, BorderWidths.DEFAULT)));
        StackPane.setMargin(textshape, INSETS);
        this.plaque.getChildren().addAll(textshape);

        this.plaque.layoutXProperty().bind(center.layoutXProperty().subtract(w / 2.0D));
        this.plaque.layoutYProperty().bind(center.layoutYProperty().subtract(h / 2.0D));

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
            dragDelta = new Point2D(t.getX(), t.getY());
            plaque.setCursor(Cursor.MOVE);
        });
        this.plaque.setOnMouseDragged(t -> {
            t.consume();
            center.setLayoutX(center.getLayoutX() + t.getX() - dragDelta.getX());
            center.setLayoutY(center.getLayoutY() + t.getY() - dragDelta.getY());
        });
        this.plaque.setOnMouseReleased(t -> {
            t.consume();
            plaque.setCursor(Cursor.HAND);
        });
    }

    private static String dateString(final DatePeriod date) {
        final Date d = date.getStartDate().getApproxDay().asDate();
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
}
