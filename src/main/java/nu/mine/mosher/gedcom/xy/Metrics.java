package nu.mine.mosher.gedcom.xy;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import nu.mine.mosher.gedcom.util.Solarized;

public class Metrics {
    private static final String FONT_FAMILY_NAME = java.awt.Font.SANS_SERIF;

    private final double fontSize;
    private final double marrDistance;
    private final double genDistance;
    private final double widthMax;
    private final double heightNominal;
    private final Font font;

    public Metrics(final double marrDistance, final double genDistance) {
        this.marrDistance = (marrDistance < 1.4D ? 150.0D : marrDistance);
        System.err.println("average marriage distance: " + this.marrDistance);
        this.genDistance = (genDistance < 1.4D ? 200.0D : genDistance);
        System.err.println("average distance between generations: " + this.genDistance);

        this.fontSize = clamp(6, Math.round(Math.rint((this.marrDistance / 15.0D + this.genDistance / 20.0D) / 2.0D)), 24);
        System.err.println("heuristically determined font point size: " + this.fontSize);

        this.font = Font.font(FONT_FAMILY_NAME, this.fontSize);
        final String stringMax = "MMMMMMMMMMMMMMMM";
        final Text text = new Text(stringMax + "\nX");
        text.setFont(this.font);
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

    public Color colorLines() {
        return Solarized.YELLOW;
    }

    public Color colorIndiBorder() {
        return Solarized.GREEN;
    }

    public Color colorIndiText() {
        return Solarized.BASE00;
    }

    public Color colorIndiBg() {
        return Solarized.BASE3;
    }

    public Color colorSelectionChooser() {
        return Solarized.MAGENTA;
    }

    public Color colorIndiSelText() {
        return Solarized.MAGENTA;
    }

    public Color colorIndiSelBg() {
        return Solarized.BASE2;
    }

    public Color colorBg() {
        return Color.TRANSPARENT;
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
