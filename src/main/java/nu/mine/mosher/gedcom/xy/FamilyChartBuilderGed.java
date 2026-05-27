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
import nu.mine.mosher.gedcom.date.DatePeriod;
import nu.mine.mosher.gedcom.date.parser.GedcomDateValueParser;
import org.slf4j.*;

import java.io.*;
import java.util.*;

public final class FamilyChartBuilderGed {
    private static final Logger LOG = LoggerFactory.getLogger(FamilyChartBuilderGed.class);

    private FamilyChartBuilderGed() {
        throw new IllegalStateException("not intended to be instantiated");
    }

    public static FamilyChart create(final GedcomTree tree, final File original) {
        final Map<String, Indi> mapIdToIndi = new HashMap<>();

        final List<Indi> indis = buildIndis(tree, mapIdToIndi);
        final List<Fami> famis = buildFamis(tree, Collections.unmodifiableMap(mapIdToIndi));

        // TODO add destroy (like in FamilyChartBuilderFtm)
//        if (indis.stream().noneMatch(Indi::hadOriginalXY)) {
//            LOG.info("No _XY coordinates found; laying out dropline chart automatically...");
            new Layout(indis, famis).clean();
//        }

        fillMissingCoords(indis);

        final Metrics metrics = Metrics.buildMetricsFor(indis, famis);
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
        final String value_XY = getChildValue(nodeIndi, "_XY");
        final Optional<Point2D> wxyOrig = Coords.toCoord(value_XY);
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
            if (value_XY.isEmpty()) {
                LOG.warn("Missing _XY value, name={}", name);
            } else {
                LOG.warn("Invalid _XY value={},name={}", value_XY, name);
            }
        }


        return new Indi(nodeIndi, wxyOrig, id, "", name, lifespan, birth, tagline, sex);
    }

    private static long calcBirthForSort(String birt) {
        final DatePeriod db = toDate(birt);
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

    private static String getLifespan(String birt, String deat) {
        final DatePeriod db = toDate(birt);
        final DatePeriod dd = toDate(deat);
        if (db.equals(DatePeriod.UNKNOWN) && dd.equals(DatePeriod.UNKNOWN)) {
            return "";
        }
        return dateString(db)+"\u2013"+dateString(dd);
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

    private static DatePeriod toDate(final String date) {
        try {
            return new GedcomDateValueParser(new StringReader(date)).parse();
        } catch (final Exception e) {
            if (!date.isEmpty()) {
                LOG.warn("Error while parsing DATE={}", date, e);
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
