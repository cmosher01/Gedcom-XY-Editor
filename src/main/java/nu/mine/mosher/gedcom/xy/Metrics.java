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

import javafx.geometry.Point2D;
import javafx.scene.*;
import javafx.scene.text.*;
import nu.mine.mosher.gedcom.xy.util.Grid;
import org.slf4j.*;

import java.util.*;

import static java.util.stream.Collectors.*;

public final class Metrics {
    private static final Logger LOG = LoggerFactory.getLogger(Metrics.class);

    public static final String FONT_FAMILY_NAME = "Noto Sans";
    public static final String FONT_FAMILY_NAME_MONO = java.awt.Font.MONOSPACED;
    public static final double FONT_SIZE_NOMINAL = 8.0D;
    public static final double FONT_SIZE_SMALL = 6.0D;
    public static final double FONT_SIZE_RATIO = 25.0D;
    public static final double DX_DEFAULT = FONT_SIZE_NOMINAL * FONT_SIZE_RATIO;

    public static final double NOMINAL_DISTANCE_MIN = 5.51D;
    public static final double NOMINAL_DISTANCE_MAX = 1000.0D;
    public static final double YDIV = 29D;
    public static final String PLAQUE_MAX = "MMMMMMMMMMMMMMMM\nM\nM";
    public static final double MARRIAGE_SPACING_FACTOR = 0.8D;

    private final double fontSize;
    private final double fontSizeSmall;
    private final double dxPartner;
    private final double dyGeneration;
    private final double dxAvg;
    private final double widthMax;
    private final double heightNominal;
    private final Font font;
    private final Font fontBold;
    private final Font fontSmall;
    private final Font fontSmallBold;
    private final Grid grid;

    // TODO: make more than just two color schemes
    // Note: the initial scheme (set here) must match the
    // initial menu-item setting
    private ColorScheme colors = new ColorSchemeBold();


    public static Metrics buildMetricsFor(final List<Indi> indis, final List<Fami> famis) {
        final double dxPartner = famis.stream().mapToDouble(Fami::getMarrDistance).filter(Metrics::nominalDistance).average().orElse(0D);
        final double dyGeneration = famis.stream().mapToDouble(Fami::getGenDistance).filter(Metrics::nominalDistance).average().orElse(0D);
        final double dxAvg = calculateAverageX(indis);
        final Grid grid = Grid.createFromPoints(indis);
        return new Metrics(dxPartner * MARRIAGE_SPACING_FACTOR, dyGeneration, dxAvg, grid);
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

    private Metrics(final double dxPartner, final double dyGeneration, final double dxAvg, final Grid grid) {
        this.dxAvg = nominalDistance(dxAvg) ? dxAvg : DX_DEFAULT;
        this.dxPartner = nominalDistance(dxPartner) ? dxPartner : dxAvg;
        this.dyGeneration = nominalDistance(dyGeneration) ? dyGeneration : dxAvg * 2.0D;

        this.fontSize = Math.clamp(Double.valueOf(Math.rint(this.dxAvg / FONT_SIZE_RATIO)).intValue(), 6, 24);
        this.fontSizeSmall = this.fontSize * 0.73D;

        this.font = loadFont("util/NotoSans-Regular.ttf", this.fontSize);
        this.fontSmall = loadFont("util/NotoSans-Regular.ttf", this.fontSizeSmall);
        this.fontBold = loadFont("util/NotoSans-Bold.ttf", this.fontSize);
        this.fontSmallBold = loadFont("util/NotoSans-Bold.ttf", this.fontSizeSmall);

        final Text text = new Text(PLAQUE_MAX);
        text.setFont(this.font);
        new Scene(new Group(text));
        text.applyCss();
        this.widthMax = text.getLayoutBounds().getWidth();
        this.heightNominal = text.getLayoutBounds().getHeight();

        this.grid = grid;

        LOG.info("metrics: dxAvg={},dxPartner={},dyGeneration={},fontSizeEst={},font=\"{}\",fontSize={},widthMax={},heightNominal={}", this.dxAvg, this.dxPartner, this.dyGeneration, this.fontSize, this.font.getName(), this.font.getSize(), this.widthMax, this.heightNominal);
    }

    private Font loadFont(final String pathRes, final double size) {
        final var res = Optional.ofNullable(getClass().getResource(pathRes));
        if (res.isEmpty()) {
            throw new IllegalStateException("Can't load resource: NotoSans-Regular.ttf");
        }
        final var loadedFont = Font.loadFont(res.get().toExternalForm(), size);
        System.out.println("Loaded font: "+loadedFont.getName());
        return loadedFont;
    }

    private static void logFont(Font font) {
    }

    public double getFontSize() {
        return this.fontSize;
    }

    public double getFontSizeSmall() {
        return this.fontSizeSmall;
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

    public Font getFontBold() {
        return this.fontBold;
    }

    public Font getFontSmall() {
        return this.fontSmall;
    }

    public Font getFontSmallBold() {
        return this.fontSmallBold;
    }

    public ColorScheme colors() {
        return this.colors;
    }





    public void setColors(final ColorScheme newColorScheme) {
        this.colors = Objects.requireNonNull(newColorScheme);
    }



    public Grid grid() {
        return this.grid;
    }
}
