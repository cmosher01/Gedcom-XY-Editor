package nu.mine.mosher.gedcom.xy;

import javafx.geometry.Point2D;
import javafx.scene.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import nu.mine.mosher.gedcom.xy.util.Solarized;
import org.slf4j.*;

import java.util.*;

import static java.util.stream.Collectors.*;

public final class Metrics {
    private static final Logger LOG = LoggerFactory.getLogger(Metrics.class);

    public static final String FONT_FAMILY_NAME = "Noto Sans";
    public static final double FONT_SIZE_NOMINAL = 8.0D;
    public static final double FONT_SIZE_RATIO = 25.0D;
    public static final double DX_DEFAULT = FONT_SIZE_NOMINAL * FONT_SIZE_RATIO;

    public static final double NOMINAL_DISTANCE_MIN = 5.51D;
    public static final double NOMINAL_DISTANCE_MAX = 1000.0D;
    public static final double YDIV = 29D;
    public static final String PLAQUE_MAX = "MMMMMMMMMMMMMMMM\nM\nM";
    public static final double MARRIAGE_SPACING_FACTOR = 0.8D;

    private final double fontSize;
    private final double dxPartner;
    private final double dyGeneration;
    private final double dxAvg;
    private final double widthMax;
    private final double heightNominal;
    private final Font font;

    // TODO: make more than just two color schemes
    // Note: the initial scheme (set here) must match the
    // initial menu-item setting
    private ColorScheme colors = new ColorSchemeBold();



    public static Metrics buildMetricsFor(final List<Indi> indis, final List<Fami> famis) {
        final double dxPartner = famis.stream().mapToDouble(Fami::getMarrDistance).filter(Metrics::nominalDistance).average().orElse(0D);
        final double dyGeneration = famis.stream().mapToDouble(Fami::getGenDistance).filter(Metrics::nominalDistance).average().orElse(0D);
        final double dxAvg = calculateAverageX(indis);
        return new Metrics(dxPartner * MARRIAGE_SPACING_FACTOR, dyGeneration, dxAvg);
    }

    private static double calculateAverageX(final List<Indi> indis) {
        final Map<Double, TreeSet<Double>> mapYtoXs = indis
            .stream()
            .map(Indi::laidOut)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(groupingBy(p -> Math.floor(p.getY() / YDIV), mapping(Point2D::getX, toCollection(TreeSet::new))));

        double avg = 0.0D;
        int c = 0;

        for (final TreeSet<Double> setX : mapYtoXs.values()) {
            final Double[] rX = setX.toArray(new Double[0]);
            for (int i = 0; i < rX.length-1; ++i) {
                final double dist =rX[i+1]-rX[i];
                if (nominalDistance(dist)) {
                    avg += dist;
                    ++c;
                }
            }
        }

        if (c <= 0) {
            LOG.warn("Could not find any valid distances between individuals.");
        } else {
            avg /= c;
        }

        return avg;
    }

    private static boolean nominalDistance(final double d) {
        return NOMINAL_DISTANCE_MIN < d && d < NOMINAL_DISTANCE_MAX;
    }

    private Metrics(final double dxPartner, final double dyGeneration, double dxAvg) {
        this.dxAvg = nominalDistance(dxAvg) ? dxAvg : DX_DEFAULT;
        this.dxPartner = nominalDistance(dxPartner) ? dxPartner : dxAvg;
        this.dyGeneration = nominalDistance(dyGeneration) ? dyGeneration : dxAvg * 2.0D;

        this.fontSize = clamp(6, Math.rint(this.dxAvg/ FONT_SIZE_RATIO), 24);

        this.font = Font.font(FONT_FAMILY_NAME, FontWeight.BOLD, this.fontSize);
        final Text text = new Text(PLAQUE_MAX);
        text.setFont(this.font);
        new Scene(new Group(text));
        text.applyCss();
        this.widthMax = text.getLayoutBounds().getWidth();
        this.heightNominal = text.getLayoutBounds().getHeight();

        LOG.info("metrics: dxAvg={},dxPartner={},dyGeneration={},fontSizeEst={},font=\"{}\",fontSize={},widthMax={},heightNominal={}", this.dxAvg, this.dxPartner, this.dyGeneration, this.fontSize, this.font.getName(), this.font.getSize(), this.widthMax, this.heightNominal);
    }

    public double getFontSize() {
        return this.fontSize;
    }

    public double getMarrDistance() {
        return this.dxPartner;
    }

    public double getGenDistance() {
        return this.dyGeneration;
    }

    public double getBarHeight() {
        return this.fontSize / 4.0D;
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

    public ColorScheme colors() {
        return this.colors;
    }

    private static double clamp(final double min, final double n, final  double max) {
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
            final int g = Integer.parseInt(s);
            if (0 <= g && g <= 1000) {
                GenXyEditor.prefs().putInt("snapToGrid", g);
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public int grid() {
        return GenXyEditor.prefs().getInt("snapToGrid", 25);
    }

    public void setColors(final ColorScheme newColorScheme) {
        this.colors = Objects.requireNonNull(newColorScheme);
    }
}
