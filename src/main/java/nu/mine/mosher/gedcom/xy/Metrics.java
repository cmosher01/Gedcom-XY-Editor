package nu.mine.mosher.gedcom.xy;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class Metrics {
    private static final String FONT_FAMILY_NAME = java.awt.Font.SANS_SERIF;

    private final double fontSize;
    private final double marrDistance;
    private final double genDistance;
    private final double widthMax;
    private final double heightNominal;
    private final Font font;

    public Metrics(double marrDistance, double genDistance) {
        this.marrDistance = (marrDistance < 1.4D ? 150.0D : marrDistance);
        System.err.println("average marriage distance: " + marrDistance);
        this.genDistance = (genDistance < 1.4D ? 200.0D : genDistance);
        System.err.println("average distance between generations: " + genDistance);

        final long fontSizeGuess = clamp(7, Math.round(Math.rint((this.marrDistance / 15.0D + this.genDistance / 20.0D) / 2.0D)), 24);
        this.fontSize = fontSizeGuess;
        System.err.println("heuristically determined font point size: " + fontSize);

        this.font = Font.font(FONT_FAMILY_NAME, fontSize);
        final String stringMax = "MMMMMMMMMMMMMMMM";
        final Text text = new Text(stringMax + "\nX");
        text.setFont(font);
        new Scene(new Group(text));
        text.applyCss();
        this.widthMax = text.getLayoutBounds().getWidth();
        System.err.println("Calculated maximum width for person: " + this.widthMax);
        this.heightNominal = text.getLayoutBounds().getHeight();
        System.err.println("Calculated nominal height for person: " + this.heightNominal);
    }

    public double getFontSize() {
        return this.fontSize;
    }

    public double getMarrDistance() {
        return this.marrDistance;
    }

    public double getGenDistance() {
        return this.genDistance;
    }

    public double getWidthMax() {
        return this.widthMax;
    }

    public double getHeightNominal() {
        return this.heightNominal;
    }

    public Font getFont() {
        return this.font;
    }

    private static long clamp(final long min, final long n,final  long max) {
        if (n < min) {
            return min;
        }
        if (max < n) {
            return max;
        }
        return n;
    }
}