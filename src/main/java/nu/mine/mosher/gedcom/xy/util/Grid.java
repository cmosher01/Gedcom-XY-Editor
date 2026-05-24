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

package nu.mine.mosher.gedcom.xy.util;

import javafx.geometry.Point2D;
import nu.mine.mosher.gedcom.xy.*;
import org.slf4j.*;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;

public class Grid {
    private static final Logger LOG = LoggerFactory.getLogger(Grid.class);

    private static final int NO_GRID = 1;
    private static final int MIN_GRID_TO_DETECT = 12;
    private static final int DEFAULT_GRID = 25;
    private static final int MAX_GRID = 1000;
    private static final int NO_OFFSET = 0;
    private static final int DEFAULT_OFFSET = NO_OFFSET;

    // TODO allow separate grid values for X and Y coordinates?
    private int grid = DEFAULT_GRID;
    private int offset = DEFAULT_OFFSET;



    public static Grid createDefault() {
        return new Grid();
    }

    private Grid() {
    }



    public static Grid createFromPoints(final List<Indi> individuals) {
        final var indis = Collections.unmodifiableList(individuals);
        final var coords = getAllCoordsFrom(indis);
        int grid = calcGrid(coords);
        int offset = NO_OFFSET;

        if (isGrid(grid)) {
            offset = calcOffset(coords, grid);
        } else {
            // can't detect grid from file, so use user's pref
            grid = getPref();
            // TODO read grid offset pref?
        }
        final var obj = new Grid(grid, offset);
        if (obj.isGrid()) {
            obj.reportGridAlignmentOf(indis);
        }
        return obj;
    }

    private Grid(final int grid, final int offset) {
        this.grid = grid;
        this.offset = offset;
    }



    void setGrid(final int grid) {
        this.grid = grid;
    }

    void setOffset(final int offset) {
        this.offset = offset;
    }



    public void setFromUserEnteredString(final Optional<String> s) {
        if (Objects.isNull(s) || s.isEmpty()) {
            return;
        }

        final int g = Math.clamp(parseIntSafe(s.get()), NO_GRID, MAX_GRID);
        this.grid = g;
        this.offset = DEFAULT_OFFSET; // TODO allow user to enter grid offset?
        setPref(g);
    }

    public String display() {
        return ""+this.grid; // TODO include offset?
    }

    public Point2D snap(final Point2D p) {
        return new Point2D(snap(p.getX()), snap(p.getY()));
    }

    public boolean isGrid() {
        return isGrid(this.grid);
    }

    private static boolean isGrid(final int rawGrid) {
        return NO_GRID < rawGrid;
    }

    double snap(final double c) {
        if (!isGrid()) {
            return c;
        }
        final long i = Math.round(Math.rint((c - this.offset) / this.grid));
        return (i * this.grid) + this.offset;
    }







    private static List<Long> getAllCoordsFrom(final List<Indi> indis) {
        final var xs = indis
            .stream()
            .map(Indi::laidOut)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(Point2D::getX)
            .filter(c -> c < -0.1D || 0.1D < c)
            // TODO handle fractional coordinates, but how to?
            .map(Math::rint)
            .map(Math::round)
            .toList();
        final var ys = indis
            .stream()
            .map(Indi::laidOut)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(Point2D::getY)
            .filter(c -> c < -0.1D || 0.1D < c)
            // TODO handle fractional coordinates, but how to?
            .map(Math::rint)
            .map(Math::round)
            .toList();

        return Stream.concat(xs.stream(), ys.stream()).toList();
    }

    // notes on variable names:
    // "dx" means "delta X", which is difference between two coordinates
    // "x" means any coordinate (regardless of whether it's x, y, or z, etc.)
    // "s" on end means plural, for example "xs" means plural of "x" (used for lists, arrays, etc.)

    private static int calcGrid(final List<Long> xs) {
        LOG.info("Total count of coordinates: {}", xs.size());

        // count unique differences between each pair of coordinates
        // map unique distances to their counts
        final var dxs = new HashMap<Long, Integer>();
        for (int i = 0; i < xs.size()-1; ++i) {
            for (int j = i+1; j < xs.size(); ++j) {
                final var dx = Math.abs(xs.get(i)-xs.get(j));
                if (0 < dx) {
                    dxs.merge(dx, 1, Integer::sum);
                }
            }
        }
        LOG.info("Total count of different distances between coordinates: {}", dxs.size());



        // sort dx values by popularity
        // use BigInteger just so we can use its "gcd()" method
        final var dxsSorted = new ArrayList<BigInteger>(dxs.size());
        {
            final double COVERAGE_RATE = 0.6D;
            LOG.info("         Distances by popularity (to {}% coverage rate)", Math.round(Math.rint(100D * COVERAGE_RATE)));
            LOG.info("            # fraction   distance-->count coverage");
            final var T = (double) (dxs.values().stream().reduce(0, Integer::sum));
            final var Tnum = (double) dxs.size();
            final var n = new AtomicInteger();
            final var inum = new AtomicInteger();
            final var cut = new AtomicBoolean();
            dxs
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())) // sort by count desc
                .forEach(e -> {
                    final var dx = e.getKey();
                    final var c = e.getValue();
                    final var p = n.addAndGet(c);
                    final var num = inum.incrementAndGet();
                    if (!cut.get()) {
                        LOG.info(String.format("%4d (%3.5f): %8d-->%5d (%3.5f)", num, num / Tnum, dx, c, p / T));
                        dxsSorted.add(BigInteger.valueOf(dx));
                        if (Double.compare(COVERAGE_RATE, p / T) <= 0) {
                            cut.set(true); // basically this terminates the loop (or makes it do nothing for the remaining iterations)
                        }
                    }
                });
        }



        // count unique GCDs (>= MIN_GRID_TO_DETECT) between each pair of distances
        final var gcds = new HashMap<BigInteger, Integer>();
        {
            final var gcdMinToDetect = BigInteger.valueOf(MIN_GRID_TO_DETECT);
            for (int i = 0; i < dxsSorted.size() - 1; ++i) {
                for (int j = i + 1; j < dxsSorted.size(); ++j) {
                    final var gcd = dxsSorted.get(i).gcd(dxsSorted.get(j));
                    if (1L < gcd.longValue()) {
                        if (gcdMinToDetect.compareTo(gcd) < 0) {
                            gcds.merge(gcd, 1, Integer::sum);
                        }
                    }
                }
            }
            LOG.info("Total count of different GCDs >= {} between each pair of distances: {}", MIN_GRID_TO_DETECT, gcds.size());
        }
        if (gcds.isEmpty()) {
            return NO_GRID;
        }



        // sort GCD values by popularity, and
        // convert from BigInteger to final result type for grid (Integer)
        final var gcdsSorted = new ArrayList<Integer>(gcds.size());
        {
            final double COVERAGE_RATE = 0.6D;
            LOG.info("Top GCDs (min: {}), by popularity (to {}% coverage rate)", MIN_GRID_TO_DETECT, Math.round(Math.rint(100D * COVERAGE_RATE)));
            final var T = (double)(gcds.values().stream().reduce(0, Integer::sum));
            final var n = new AtomicInteger();
            final var cut = new AtomicBoolean();
            gcds
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())) // sort by count desc
                .forEach(e -> {
                    final var gcd = e.getKey().intValue();
                    final int c = e.getValue();
                    final var p = n.addAndGet(c);
                    if (!cut.get()) {
                        LOG.info(String.format("%8d-->%5d (%3.5f)", gcd, c, p / T));
                        gcdsSorted.add(gcd);
                        if (Double.compare(COVERAGE_RATE, p / T) <= 0) {
                            cut.set(true); // basically this terminates the loop (or makes it do nothing for the remaining iterations)
                        }
                    }
                });
        }
        if (gcdsSorted.isEmpty()) {
            return NO_GRID;
        }

        // and take min GCD between each of those GCDs
        BigInteger bigGcdFinal = null;
        for (int i = 0; i < gcdsSorted.size(); ++i) {
            final var gcd = BigInteger.valueOf(gcdsSorted.get(i));
            if (Objects.isNull(bigGcdFinal)) {
                bigGcdFinal = gcd;
            } else {
                bigGcdFinal = bigGcdFinal.gcd(gcd);
            }
        }
        // This is what we're trying to calculate,
        // the value used for snap-to-grid.
        final int gcdFinal;
        if (isGrid(bigGcdFinal.intValue())) {
            gcdFinal = bigGcdFinal.intValue();
            LOG.info("(final) GCD of all those GCDs: {} <-----------FINAL GRID VALUE", gcdFinal);
            // log report on top GCDs to show if they are multiples
            // of the chosen GCD (which is good)
            LOG.info("Top 10 GCDs:");
            LOG.info("          M = is a multiple of final GCD");
            LOG.info("          XXXXX = is NOT a multiple of final GCD");
            for (int i = 0; i < Math.min(20,gcdsSorted.size()); ++i) {
                final var gcd = gcdsSorted.get(i);
                LOG.info(String.format("    %5d %s", gcd, (gcd % gcdFinal == 0) ? "M" : "XXXXX"));
            }
        } else {
            gcdFinal = gcdsSorted.getFirst();
            LOG.warn("Initial coordinates are not well aligned, choosing majority alignment: {}", gcdFinal);
        }

        return gcdFinal;
    }

    private static int calcOffset(final List<Long> xs, final int grid) {
        final var offs = new HashMap<Integer, Integer>(grid);
        for (final var x : xs) {
            for (int off = 0; off < grid; ++off) {
                if ((x-off) % grid == 0) { // x-off, not x+off
                    offs.merge(off, 1, Integer::sum);
                }
            }
        }
        if (offs.isEmpty()) {
            return NO_OFFSET;
        }



        final var offsSorted = new ArrayList<Integer>(offs.size());
        final var offsSortedCounts = new ArrayList<Integer>(offs.size());
        offs
            .entrySet()
            .stream()
            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
            .forEach(e -> {
                final var off = e.getKey();
                offsSorted.add(off);
                final var offCount = e.getValue();
                offsSortedCounts.add(offCount);
            });

        LOG.info("Top offsets:");
        LOG.info("   offset  count");
        for (int i = 0; i < Math.min(100,offsSorted.size()); ++i) {
            final var off = offsSorted.get(i);
            final var offCount = offsSortedCounts.get(i);
            LOG.info(String.format("    %5d   %5d", off, offCount));
        }
        final var offFinal = offsSorted.getFirst();
        LOG.info("Chose top offset value: {} <-----------FINAL GRID OFFSET VALUE", offFinal);

        return offFinal;
    }



    private void reportGridAlignmentOf(final List<Indi> indis) {
        reportPointGridAlignmentOf(indis);
        reportCoordinateGridAlignmentOf(indis);
    }

    private void reportCoordinateGridAlignmentOf(final List<Indi> indis) {
        final var xs = indis
                .stream()
                .map(Indi::laidOut)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Point2D::getX)
                .filter(c -> c < -0.1D || 0.1D < c)
                .toList();
        final var ys = indis
                .stream()
                .map(Indi::laidOut)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Point2D::getY)
                .filter(c -> c < -0.1D || 0.1D < c)
                .toList();
        final var xys = Stream.concat(xs.stream(), ys.stream()).toList();

        final var N = xys.size();
        int cAligned = 0;
        for (final var xy : xys) {
            final var xySnapped = snap(xy);
            final var eq = Math.abs(xySnapped-xy) < 0.1D;
            if (eq) {
                cAligned++;
            }
        }
        final var percentAligned = Math.round(Math.rint(
                (100D * cAligned) / N
        ));

        LOG.info("Count of non-zero single coordinates, X or Y: {}", N);
        LOG.info("Count of those coordinates initially aligned to calculated grid/offset: {} ({}%)", cAligned, percentAligned);
    }

    private void reportPointGridAlignmentOf(final List<Indi> indis) {
        final var xys = indis
            .stream()
            .map(Indi::laidOut)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(i -> {
                final var x = i.getX();
                final var y = i.getY();
                return
                    (x < -0.1D || 0.1D < x) ||
                    (y < -0.1D || 0.1D < y);
            })
            .toList();

        final var N = xys.size();
        int cAligned = 0;
        for (final var xy : xys) {
            final var xySnapped = snap(xy);
            if (xySnapped.equals(xy)) {
                cAligned++;
            }
        }
        final var percentAligned = Math.round(Math.rint(
            (100D * cAligned) / N
        ));

        LOG.info("Count of people with points not (0,0): {}", N);
        LOG.info("Count of those people initially aligned to calculated grid/offset: {} ({}%)", cAligned, percentAligned);
    }







    private static int getPref() {
        // TODO remove dependency on GenXyEditor, make it just Preferences
        return GenXyEditor.prefs().getInt("snapToGrid", DEFAULT_GRID);
    }

    private static void setPref(final int grid) {
        // TODO remove dependency on GenXyEditor, make it just Preferences
        GenXyEditor.prefs().putInt("snapToGrid", grid);
    }

    private static int parseIntSafe(final String s) {
        int ret = NO_GRID;
        if (Objects.nonNull(s) && !s.isBlank()) {
            try {
                ret = Math.clamp(Integer.parseInt(s), NO_GRID, MAX_GRID);
            } catch (final Throwable e) {
                LOG.warn("User entered invalid integer for snap-to-grid size: \"{}\"", s);
                LOG.warn("Integer.parseInt threw exception:", e);
            }
        }
        return ret;
    }
}
