package nu.mine.mosher.gedcom.xy;

import javafx.geometry.Point2D;
import nu.mine.mosher.collection.TreeNode;
import nu.mine.mosher.gedcom.GedcomLine;
import nu.mine.mosher.gedcom.GedcomTag;
import nu.mine.mosher.gedcom.GedcomTree;
import nu.mine.mosher.gedcom.date.DatePeriod;
import nu.mine.mosher.gedcom.date.parser.GedcomDateValueParser;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class FamilyChartBuilder {
    private static final double MAX_NATURAL_DISTANCE = 1000.0D;

    private FamilyChartBuilder() {
        throw new IllegalStateException("not intended to be instantiated");
    }

    public static FamilyChart create(final GedcomTree tree) {
        final Map<String, Indi> mapIdToIndi = new HashMap<>();
        final List<Indi> indis = buildIndis(tree, mapIdToIndi);
        final List<Fami> famis = buildFamis(tree, Collections.unmodifiableMap(mapIdToIndi));
        final Metrics metrics = metrics(indis, famis);
        famis.forEach(f -> f.setMetrics(metrics));
        indis.forEach(i -> i.setMetrics(metrics));
        layout(indis, famis, metrics);
        normalize(indis);
        return new FamilyChart(tree, indis, famis, metrics);
    }

    private static Metrics metrics(List<Indi> indis, List<Fami> famis) {
        final double marlen = famis.stream().mapToDouble(Fami::getMarrDistance).filter(d -> d < MAX_NATURAL_DISTANCE).average().orElse(0D);
        final double genlen = famis.stream().mapToDouble(Fami::getGenDistance).filter(d -> d < MAX_NATURAL_DISTANCE).average().orElse(0D);
        return new Metrics(marlen, genlen);
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
        System.err.println(String.format("Calculated %d individuals.", indis.size()));
        return indis;
    }

    private static void normalize(final List<Indi> indis) {
        final double x = indis.stream().map(Indi::getCoords).mapToDouble(Point2D::getX).min().orElse(0D);
        final double y = indis.stream().map(Indi::getCoords).mapToDouble(Point2D::getY).min().orElse(0D);
        indis.forEach(i -> i.shiftOrig(x, y));
    }

    private static List<Fami> buildFamis(final GedcomTree tree, final Map<String, Indi> mapIdToIndi) {
        final List<Fami> famis = new ArrayList<>();
        tree.getRoot().forEach(nodeFami -> {
            if (nodeFami.getObject().getTag().equals(GedcomTag.FAM)) {
                final Fami fami = buildFami(nodeFami, Collections.unmodifiableMap(mapIdToIndi));
                famis.add(fami);
            }
        });
        System.err.println(String.format("Calculated %d families.", famis.size()));
        return famis;
    }

    private static Indi buildIndi(final TreeNode<GedcomLine> nodeIndi) {
        final String xyval = getChildValue(nodeIndi, "_XY");
        final Optional<Point2D> coords = toCoord(xyval);
        final GedcomLine lineIndi = nodeIndi.getObject();
        final String name = toName(getChildValue(nodeIndi, "NAME"));
        final DatePeriod birth = toDate(getChildEventDate(nodeIndi, "BIRT"));
        final DatePeriod death = toDate(getChildEventDate(nodeIndi, "DEAT"));
        final String refn = getChildValue(nodeIndi, "REFN");
        final int sex = toSex(getChildValue(nodeIndi, "SEX"));
        final String id = lineIndi.getID();

        if (xyval.isEmpty()) {
            System.err.println("WARNING: missing _XY for: " + name);
        }

        return new Indi(nodeIndi, coords.orElse(Point2D.ZERO), id, name, birth, death, refn, sex);
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

    private static Optional<Point2D> toCoord(final String xy) {
        if (Objects.isNull(xy) || xy.isEmpty()) {
            return Optional.empty();
        }
        final double[] r = Arrays.stream(xy.split("\\s+")).mapToDouble(FamilyChartBuilder::parseCoord).toArray();
        if (r.length != 2) {
            System.err.println("Could not parse _XY: " + xy);
            return Optional.empty();
        }
        return Optional.of(new Point2D(r[0], r[1]));
    }

    private static double parseCoord(final String s) {
        if (Objects.isNull(s) || s.isEmpty()) {
            return 0;
        }
        try {
            return Double.parseDouble(s);
        } catch (final NumberFormatException ignore) {
            System.err.println("Could not parse value from _XY tag: " + s);
            return 0;
        }
    }

    private static String toName(final String name) {
        return name.replaceAll("/", "");
    }

    private static DatePeriod toDate(final String date) {
        try {
            return new GedcomDateValueParser(new StringReader(date)).parse();
        } catch (final Exception e) {
            if (!date.isEmpty()) {
                System.err.println("Error while parsing DATE: \"" + date + "\"");
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

    private static String getChildValue(final TreeNode<GedcomLine> node, final String tag) {
        for (final TreeNode<GedcomLine> c : node) {
            if (c.getObject().getTagString().equals(tag)) {
                return c.getObject().getValue();
            }
        }
        return "";
    }

    private static Fami buildFami(final TreeNode<GedcomLine> nodeFami, final Map<String, Indi> mapIdToIndi) {
        final Fami fami = new Fami();
        for (final TreeNode<GedcomLine> c : nodeFami) {
            final GedcomLine child = c.getObject();
            switch (child.getTag()) {
                case HUSB:
                    fami.setHusb(mapIdToIndi.get(child.getPointer()));
                    break;
                case WIFE:
                    fami.setWife(mapIdToIndi.get(child.getPointer()));
                    break;
                case CHIL:
                    fami.addChild(mapIdToIndi.get(child.getPointer()));
                    break;
            }
        }
        return fami;
    }

    private static void layout(final List<Indi> indis, final List<Fami> famis, final Metrics metrics) {
        /* don't layout if _XY was found on anyone */
        for (final Indi indi : indis) {
            if (indi.getCoords().magnitude() != 0) {
                return;
            }
        }
        System.err.println("No _XY coordinates found; laying out dropline chart automatically...");
        new Layout(indis, famis, metrics).cleanAll();
    }
}
