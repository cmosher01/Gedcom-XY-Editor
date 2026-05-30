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
import nu.mine.mosher.collection.TreeNode;
import nu.mine.mosher.gedcom.*;
import nu.mine.mosher.gedcom.date.*;
import nu.mine.mosher.gedcom.date.parser.*;
import org.slf4j.*;

import java.io.*;
import java.util.*;
import java.util.prefs.Preferences;

public final class FamilyChartBuilderGed {
    private static final Logger LOG = LoggerFactory.getLogger(FamilyChartBuilderGed.class);

    private FamilyChartBuilderGed() {
        throw new IllegalStateException("not intended to be instantiated");
    }

    public static FamilyChart create(final GedcomTree tree, final File original, boolean destroy, final Preferences prefs) {
        final Map<String, Indi> mapIdToIndi = new HashMap<>();

        final List<Indi> indis = buildIndis(tree, mapIdToIndi);
        final List<Fami> famis = buildFamis(tree, Collections.unmodifiableMap(mapIdToIndi));

        /*
        TODO: how to handle destroy?
         */
        new Layout(indis, famis).clean();

        fillMissingCoords(indis);

        final Metrics metrics = Metrics.buildMetricsFor(indis, famis, prefs);
        famis.forEach(f -> f.setMetrics(metrics));
        indis.forEach(i -> i.setMetrics(metrics));

        return new FamilyChart(tree, indis, famis, metrics, original);
    }

    private static List<Indi> buildIndis(final GedcomTree tree, final Map<String, Indi> mapIdToIndi) {
        final List<Indi> indis = new ArrayList<>();
        tree.getRoot().forEach(nodeIndi -> {
            if (nodeIndi.getObject().getTag().equals(GedcomTag.INDI)) {
                final Indi indi = buildIndi(nodeIndi);
                mapIdToIndi.put(indi.getId(), indi);
                indis.add(indi);
            }
        });
        LOG.info("Calculated {} individuals.", indis.size());
        return indis;
    }

    private static void fillMissingCoords(final List<Indi> indis) {
        final double x = indis.stream().map(Indi::laidOut).filter(Optional::isPresent).map(Optional::get).mapToDouble(Point2D::getX).min().orElse(0D);
        final double y = indis.stream().map(Indi::laidOut).filter(Optional::isPresent).map(Optional::get).mapToDouble(Point2D::getY).min().orElse(0D);
        final Point2D coordsTopLeftAfterLayout = new Point2D(x, y);
        indis.forEach(i -> i.fillMissingCoords(coordsTopLeftAfterLayout));
    }

    private static List<Fami> buildFamis(final GedcomTree tree, final Map<String, Indi> mapIdToIndi) {
        final List<Fami> famis = new ArrayList<>();
        tree.getRoot().forEach(nodeFami -> {
            if (nodeFami.getObject().getTag().equals(GedcomTag.FAM)) {
                final Fami fami = buildFami(nodeFami, Collections.unmodifiableMap(mapIdToIndi));
                famis.add(fami);
            }
        });
        LOG.info("Calculated {} families.", famis.size());
        return famis;
    }

    private static Indi buildIndi(final TreeNode<GedcomLine> nodeIndi) {
        final String xy = getChildValue(nodeIndi, "_XY");
        final Optional<Point2D> wxyOrig = Coords.toCoord(xy);
        // wxyOrig empty indicates that _XY either was not present, or was present but had an invalid format
        // In either of these two cases, when we save the new GEDCOM file, we want to ADD a new _XY record

        final String name = getChildValue(nodeIndi, "NAME");
        final String lifespan = getLifespan(getChildEventDate(nodeIndi, "BIRT"), getChildEventDate(nodeIndi, "DEAT"));
        final long birth = calcBirthForSort(getChildEventDate(nodeIndi, "BIRT"));
        final int sex = toSex(getChildValue(nodeIndi, "SEX"));
        final String id = nodeIndi.getObject().getID();
        final String birthplace = getChildEventPlace(nodeIndi, "BIRT");
        final String anyplace = getChildEventPlace(nodeIndi);
        final String tagline = birthplace.isBlank() ? anyplace : birthplace;

        if (wxyOrig.isEmpty()) {
            if (xy.isEmpty()) {
                LOG.warn("Missing _XY value, name={}", name);
            } else {
                LOG.warn("Invalid _XY value={},name={}", xy, name);
            }
        }


        return new Indi(nodeIndi, wxyOrig, id, "", name, lifespan, birth, tagline, sex);
    }

    private static long calcBirthForSort(final String birt) {
        final DatePeriod db = toDateStrict(birt);
        final Date d = db.getStartDate().getApproxDay().asDate();
        if (d.getTime() == 0) {
            return 0L;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        return year*100L+month;
    }

    private static String getLifespan(final String birt, final String deat) {
        final var db = toDateLenient(birt);
        final var dd = toDateLenient(deat);
        if (db.equals("?") && dd.equals("?")) {
            return "";
        }
        return "("+db+"\u2013"+dd+")";
    }

    private static String toDateLenient(final String s) {
        String ret = "?";
        if (!s.isBlank()) {
            LOG.info("Parsing GEDCOM date string: \"{}\"", s);
            try {
                final DatePeriod period = new GedcomDateValueParser(new StringReader(s)).parse();
                if (period.equals(DatePeriod.UNKNOWN)) {
                    ret = "?";
                } else {
                    ret = dateString(period);
                }
            } catch (final ParseException e) {
                // use the date string, but make sure it's a reasonable size
                if (s.isBlank()) {
                    ret = "?";
                } else if (s.length() <= 16) {
                    ret = s;
                } else {
                    ret = s.substring(0, 16);
                }
                LOG.info("Invalid date. Cannot interpret, but will display as: \"{}\"", ret, e);
            }
        }
        return ret;
    }

    private static String dateString(final DatePeriod period) {
        // We are only interested in BIRTH and DEATH dates, which are not time periods
        // (i.e., not "FROM start TO end"), so here we just choose the START date.
        // (Not to be confused with "BET x AND y" or "AFT x" or "BEF y", which are acceptable.)
        final var range = period.getStartDate();
        final var yearEarliest = range.getEarliest().getYear();
        final var yearLatest = range.getLatest().getYear();
        final var yearApprox = (yearEarliest+yearLatest)/2;
        final int year;
        if (range.getEarliest().equals(YMD.getMinimum())) {
            year = yearLatest;
        } else if (range.getLatest().equals(YMD.getMaximum())) {
            year = yearEarliest;
        } else {
            year = yearApprox;
        }

        final String circa;
        if (range.isExact()) {
            if (range.getEarliest().isCirca() || range.getLatest().isCirca()) {
                circa = "c";
            } else {
                circa = "";
            }
        } else {
            circa = "c";
        }

        final int absYear;
        final String bce;
        if (year < 0) {
            absYear = -year;
            bce = "bce";
        } else {
            absYear = year;
            bce = "";
        }

        return String.format("%s%d%s", circa, absYear, bce);
    }

    private static int toSex(final String sex) {
        if (!sex.isEmpty()) {
            final char c = sex.toUpperCase().charAt(0);
            if (c == 'M') {
                return 1;
            }
            if (c == 'F') {
                return 2;
            }
        }
        return 0;
    }

    private static DatePeriod toDateStrict(final String sDate) {
        try {
            return new GedcomDateValueParser(new StringReader(sDate)).parse();
        } catch (final Exception e) {
            if (!sDate.isEmpty()) {
                LOG.warn("Error while parsing DATE={}", sDate, e);
            }
            return DatePeriod.UNKNOWN;
        }
    }

    private static String getChildEventDate(final TreeNode<GedcomLine> node, final String tag) {
        for (final TreeNode<GedcomLine> c : node) {
            if (c.getObject().getTagString().equals(tag)) {
                return getChildValue(c, "DATE");
            }
        }
        return "";
    }

    private static String getChildEventPlace(final TreeNode<GedcomLine> node, final String tag) {
        for (final TreeNode<GedcomLine> c : node) {
            if (c.getObject().getTagString().equals(tag)) {
                return getChildValue(c, "PLAC");
            }
        }
        return "";
    }

    private static String getChildEventPlace(final TreeNode<GedcomLine> node /* any event*/) {
        for (final TreeNode<GedcomLine> c : node) {
            final var p = getChildValue(c, "PLAC");
            if (!p.isBlank()) {
                return p;
            }
        }
        return "";
    }

    private static String getChildValue(final TreeNode<GedcomLine> node, final String tag) {
        for (final TreeNode<GedcomLine> c : node) {
            if (c.getObject().getTagString().equals(tag)) {
                return c.getObject().getValue().trim();
            }
        }
        return "";
    }

    private static Fami buildFami(final TreeNode<GedcomLine> nodeFami, final Map<String, Indi> mapIdToIndi) {
        final Fami fami = new Fami();
        for (final TreeNode<GedcomLine> c : nodeFami) {
            final GedcomLine child = c.getObject();
            final var indi = Optional.ofNullable(mapIdToIndi.get(child.getPointer()));
            switch (child.getTag())
            {
                case HUSB -> {
                    if (indi.isPresent()) {
                        fami.setHusb(indi.get());
                        indi.get().addAsSpouseTo(fami);
                    }
                }
                case WIFE -> {
                    if (indi.isPresent()) {
                        fami.setWife(indi.get());
                        indi.get().addAsSpouseTo(fami);
                    }
                }
                case CHIL -> {
                    if (indi.isPresent()) {
                        fami.addChild(indi.get());
                        indi.get().setAsChildTo(fami);
                    }
                }
            }
        }
        return fami;
    }
}
