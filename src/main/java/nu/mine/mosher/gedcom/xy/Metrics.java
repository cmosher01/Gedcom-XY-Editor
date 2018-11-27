package nu.mine.mosher.gedcom.xy;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import nu.mine.mosher.gedcom.xy.util.Solarized;

public final class Metrics {
    private static final String FONT_FAMILY_NAME = java.awt.Font.SANS_SERIF;

    private final double fontSize;
    private final double marrDistance;
    private final double genDistance;
    private final double widthMax;
    private final double heightNominal;
    private final Font font;
    private int grid = 25;

    public Metrics(final double marrDistance, final double genDistance) {
        this.marrDistance = (marrDistance < 1.4D ? 150.0D : marrDistance);
        this.genDistance = (genDistance < 1.4D ? 200.0D : genDistance);

        this.fontSize = clamp(6, Math.round(Math.rint((this.marrDistance / 15.0D + this.genDistance / 20.0D) / 2.0D)), 24);

        this.font = Font.font(FONT_FAMILY_NAME, this.fontSize);
        final String stringMax = "MMMMMMMMMMMMMMMM";
        final Text text = new Text(stringMax + "\nX");
        text.setFont(this.font);
        new Scene(new Group(text));
        text.applyCss();
        this.widthMax = text.getLayoutBounds().getWidth();
        this.heightNominal = text.getLayoutBounds().getHeight();
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

    public double getBarHeight() {
        return this.fontSize / 2.0D;
    }

    public double getChildHeight() {
        return this.fontSize * 4.0D;
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

    void setGrid(final String s) {
        try {
            final int g =Integer.parseInt(s);
            if (0 <= g && g <= 1000) {
                this.grid = g;
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public int grid() {
        return this.grid;
    }
}
