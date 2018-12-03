package nu.mine.mosher.gedcom.xy;

import javafx.geometry.Point2D;
import nu.mine.mosher.collection.TreeNode;
import nu.mine.mosher.gedcom.GedcomLine;
import nu.mine.mosher.gedcom.GedcomTag;
import nu.mine.mosher.gedcom.GedcomTree;
import nu.mine.mosher.gedcom.date.DatePeriod;
import nu.mine.mosher.gedcom.date.parser.GedcomDateValueParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class FamilyChartBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(FamilyChartBuilder.class);

    private FamilyChartBuilder() {
        throw new IllegalStateException("not intended to be instantiated");
    }

    public static FamilyChart create(final GedcomTree tree) {
        final Map<String, Indi> mapIdToIndi = new HashMap<>();

        final List<Indi> indis = buildIndis(tree, mapIdToIndi);
        final List<Fami> famis = buildFamis(tree, Collections.unmodifiableMap(mapIdToIndi));

        if (indis.stream().noneMatch(Indi::hadOriginalXY)) {
            LOG.info("No _XY coordinates found; laying out dropline chart automatically...");
            new Layout(indis, famis).cleanAll();
        }

        normalize(indis);

        final Metrics metrics = Metrics.buildMetricsFor(indis, famis);
        famis.forEach(f -> f.setMetrics(metrics));
        indis.forEach(i -> i.setMetrics(metrics));

        return new FamilyChart(tree, indis, famis, metrics);
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

    private static void normalize(final List<Indi> indis) {
        final double x = indis.stream().map(Indi::laidOut).filter(Optional::isPresent).map(Optional::get).mapToDouble(Point2D::getX).min().orElse(0D);
        final double y = indis.stream().map(Indi::laidOut).filter(Optional::isPresent).map(Optional::get).mapToDouble(Point2D::getY).min().orElse(0D);
        final Point2D coordsTopLeftAfterLayout = new Point2D(x, y);
        indis.forEach(i -> i.fillMissing(coordsTopLeftAfterLayout));
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
        final Optional<Point2D> wxyOrig = toCoord(value_XY);
        // wxyOrig empty indicates that _XY either was not present, or was present but had an invalid format
        // In either of these two cases, when we save the new GEDCOM file, we want to ADD a new _XY record

        final String name = toName(getChildValue(nodeIndi, "NAME"));
        final DatePeriod birth = toDate(getChildEventDate(nodeIndi, "BIRT"));
        final DatePeriod death = toDate(getChildEventDate(nodeIndi, "DEAT"));
        final String refn = getChildValue(nodeIndi, "REFN");
        final int sex = toSex(getChildValue(nodeIndi, "SEX"));
        final String id = nodeIndi.getObject().getID();

        if (!wxyOrig.isPresent()) {
            if (value_XY.isEmpty()) {
                LOG.warn("Missing _XY value, name={}", name);
            } else {
                LOG.warn("Invalid _XY value={},name={}", value_XY, name);
            }
        }

        return new Indi(nodeIndi, wxyOrig, id, name, birth, death, refn, sex);
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

    /**
     * parse given value of _XY line.
     * If unrecognized format, treat as unknown tag (leave it alone), return empty
     * @param xy
     * @return
     */
    private static Optional<Point2D> toCoord(final String xy) {
        if (Objects.isNull(xy) || xy.isEmpty()) {
            return Optional.empty();
        }

        final String[] fields = xy.split("\\s+");
        if (fields.length != 2) {
            return Optional.empty();
        }

        final Optional<Double> x = parseCoord(fields[0]);
        if (!x.isPresent()) {
            return Optional.empty();
        }

        final Optional<Double> y = parseCoord(fields[1]);
        if (!y.isPresent()) {
            return Optional.empty();
        }

        return Optional.of(new Point2D(x.get(), y.get()));
    }

    private static Optional<Double> parseCoord(final String s) {
        if (Objects.isNull(s) || s.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Double.parseDouble(s));
        } catch (final NumberFormatException ignore) {
            return Optional.empty();
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
}
