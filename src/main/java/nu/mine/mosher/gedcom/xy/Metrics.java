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
import nu.mine.mosher.gedcom.xy.util.*;
import org.slf4j.*;

import java.util.*;
import java.util.prefs.Preferences;

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

    public static final double MARGIN = 200.0D;



    private final double fontSize;
    private final double fontSizeSmall;
    private final double dxPartner;
    private final double dyGeneration;
    private final double dxAvg;
    private final double widthMax;
    private final double heightNominal;
    private final double margin = MARGIN;
    private final Font font;
    private final Font fontBold;
    private final Font fontSmall;
    private final Font fontSmallBold;
    private final Grid grid;

    // TODO: make more than just two color schemes
    // Note: the initial scheme (set here) must match the
    // initial menu-item setting
    private ColorScheme colors = new ColorSchemeBold();



    public static Metrics buildMetricsFor(final List<Indi> indis, final List<Fami> famis, final Preferences prefs) {
        final Grid grid = Grid.createFromPoints(indis, prefs);
        final double dxAvg = nominal(grid.grid(), DX_DEFAULT);
        final double dxPartner = nominal(calcDxPartner(famis), dxAvg);
        final double dyGeneration = nominal(calcDyGeneration(famis), 2.0D*dxAvg);
        return new Metrics(dxPartner * MARRIAGE_SPACING_FACTOR, dyGeneration, dxAvg, grid);
    }

    private static double calcDxPartner(final List<Fami> famis) {
        final var rn = famis.stream().map(Fami::getMarrDistance).filter(Metrics::nominalDistance).toList();
        return MathUtil.median(rn);
    }

    private static double calcDyGeneration(final List<Fami> famis) {
        final var rn = famis.stream().map(Fami::getGenDistance).filter(Metrics::nominalDistance).toList();
        return MathUtil.median(rn);
    }

    private static double nominal(final double n, final double nDefault) {
        return nominalDistance(n) ? n : nDefault;
    }

    private static boolean nominalDistance(final double d) {
        return NOMINAL_DISTANCE_MIN < d && d < NOMINAL_DISTANCE_MAX;
    }

    private Metrics(final double dxPartner, final double dyGeneration, final double dxAvg, final Grid grid) {
        this.dxAvg = dxAvg;
        this.dxPartner = dxPartner;
        this.dyGeneration = dyGeneration;

        this.fontSize = Math.clamp(this.dxAvg/FONT_SIZE_RATIO, 6.0D, 24.0D);
        this.fontSizeSmall = this.fontSize * 0.75D;

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

        LOG.info("metrics: dxAvg={},dxPartner={},dyGeneration={},fontSizeEst={},font=\"{}\",fontSize={},fontSizeSmall={},widthMax={},heightNominal={}",
            this.dxAvg, this.dxPartner, this.dyGeneration, this.fontSize, this.font.getName(), this.fontSize, this.fontSizeSmall, this.widthMax, this.heightNominal);
    }

    private Font loadFont(final String pathRes, final double size) {
        final var res = Optional.ofNullable(getClass().getResource(pathRes));
        if (res.isEmpty()) {
            throw new IllegalStateException("Can't load resource: "+pathRes);
        }
        final var loadedFont = Font.loadFont(res.get().toExternalForm(), size);
        LOG.info("Loaded font: {}", loadedFont.getName());
        return loadedFont;
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

    public double margin() {
        return this.margin;
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

    public Grid grid() {
        return this.grid;
    }

    public ColorScheme colors() {
        return this.colors;
    }




    public void setColors(final ColorScheme newColorScheme) {
        this.colors = Objects.requireNonNull(newColorScheme);
    }
}
