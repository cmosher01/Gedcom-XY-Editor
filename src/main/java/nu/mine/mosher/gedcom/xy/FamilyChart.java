package nu.mine.mosher.gedcom.xy;

import javafx.beans.property.*;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import nu.mine.mosher.collection.TreeNode;
import nu.mine.mosher.gedcom.*;
import org.slf4j.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class FamilyChart {
    private static final Logger LOG = LoggerFactory.getLogger(FamilyChart.class);

    private final Optional<File> fileOriginal;
    private final GedcomTree tree;
    private final List<Indi> indis;
    private final List<Fami> famis;
    private final Metrics metrics;
    private final Selection selection = new Selection();
    private StringProperty selectedNameProperty = new SimpleStringProperty();

    public FamilyChart(final GedcomTree tree, final List<Indi> indis, final List<Fami> famis, final Metrics metrics) {
        this(tree, indis, famis, metrics, null);
    }

    public FamilyChart(final GedcomTree tree, final List<Indi> indis, final List<Fami> famis, final Metrics metrics, final File fileOriginal) {
        this.fileOriginal = Optional.ofNullable(fileOriginal);
        this.tree = tree;
        this.indis = Collections.unmodifiableList(new ArrayList<>(indis));
        this.famis = Collections.unmodifiableList(new ArrayList<>(famis));
        this.metrics = metrics;
    }

    public void addGraphicsTo(final List<Node> addto) {
        this.famis.forEach(f -> f.addGraphicsTo(addto));
        this.indis.forEach(i -> i.addGraphicsTo(addto));
    }

    public void setFromOrig() {
        this.indis.forEach(i -> i.setSelection(this.selection));
        this.indis.forEach(Indi::calc);
        this.famis.forEach(Fami::calc);
        this.indis.forEach(Indi::startCoordTracking);
    }

    public void clearSelection() {
        this.selection.clear();
        updateSelectStatus();
    }

    public void setSelectionFrom(double x, double y, double w, double h) {
        this.indis.forEach(i -> {
            this.selection.select(i, i.intersects(x, y, w, h), false);
        });
        updateSelectStatus();
    }

    private void updateSelectStatus() {
        final long cSel = this.indis.stream().filter(Indi::selected).count();
        if (cSel <= 0) {
            this.selectedNameProperty.setValue("[nothing selected]");
        } else if (1 < cSel) {
            this.selectedNameProperty.setValue(String.format("[%d selected]", cSel));
        } else {
            final Optional<Indi> i = this.indis.stream().filter(Indi::selected).findAny();
            if (i.isPresent()) {
                final Point2D coords = i.get().coords();
                final Optional<Point2D> coordsOriginal = i.get().coordsOriginal();
                final String from;
                if (i.get().dirty()) {
                    if (coordsOriginal.isPresent()) {
                        from = String.format("(%.2f,%.2f) \u2192 ", coordsOriginal.get().getX(), coordsOriginal.get().getY());
                    } else {
                        from = "() \u2192 ";
                    }
                } else {
                    from = "";
                }
                this.selectedNameProperty.setValue(String.format("[%s selected] %s(%.2f,%.2f) [%.2fx%.2f]", i.get().name(), from, coords.getX(), coords.getY(), i.get().width(), i.get().height()));
            } else {
                this.selectedNameProperty.setValue("[nothing selected]");
            }
        }
    }

    public Metrics metrics() {
        return this.metrics;
    }

    public void saveAs(final File file) throws IOException {
        this.indis.stream().filter(Indi::dirty).forEach(Indi::saveXyToTree);
        tree.timestamp();
        Gedcom.writeFile(tree, new BufferedOutputStream(new FileOutputStream(file)));
    }

    public void saveSkeleton(final boolean exportAll, final File file) throws IOException {
        final PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)));

        out.println("0 HEAD");
        out.println("1 CHAR UTF-8");
        out.println("1 GEDC");
        out.println("2 VERS 5.5.1");
        out.println("2 FORM LINEAGE-LINKED");
        out.println("1 SOUR _XY EDITOR");

        this.indis.stream().filter(i -> exportAll || i.dirty()).forEach(i -> {
            i.saveXyToTree();
            extractSkeleton(i.node(), out);
        });

        out.println("0 TRLR");

        if (out.checkError()) {
            LOG.error("ERROR exporting skeleton file, file={}", file);
        }
        out.close();
    }

    public boolean dirty() {
        return this.indis.stream().anyMatch(Indi::dirty);
    }

    public void userNormalize() {
        final double x = this.indis.stream().map(Indi::coords).mapToDouble(Point2D::getX).min().orElse(0D);
        final double y = this.indis.stream().map(Indi::coords).mapToDouble(Point2D::getY).min().orElse(0D);
        final Point2D coordsTopLeft = new Point2D(x, y);
        this.indis.forEach(i -> i.userNormalize(coordsTopLeft));
        updateSelectStatus();
    }

    public List<Indi> indis() {
        return Collections.unmodifiableList(new ArrayList<>(this.indis));
    }

    public StringProperty selectedName() {
        return this.selectedNameProperty;
    }

    public Optional<File> originalFile() {
        return fileOriginal;
    }

    public class Selection {
        private final Set<Indi> indis = new HashSet<>();
        private Point2D orig;

        public void clear() {
            this.indis.forEach(i -> i.select(false));
            this.indis.clear();
        }

        public void select(final Indi indi, final boolean select, final boolean updateStatus) {
            indi.select(select);
            if (select) {
                this.indis.add(indi);
            } else {
                this.indis.remove(indi);
            }
            if (updateStatus) {
                updateSelectStatus();
            }
        }

        public void beginDrag(final Point2D orig) {
            this.orig = orig;
            updateSelectStatus();
        }

        public void drag(final Point2D to) {
            this.indis.forEach(i -> i.drag(to.subtract(this.orig)));
            updateSelectStatus();
        }
    }



    private static final Set<String> SKEL;

    static {
        final Set<String> s = new HashSet<>();
        s.add("NAME");
        s.add("SEX");
        s.add("REFN");
        s.add("RIN");
        s.add("_XY");
        s.add("BIRT");
        s.add("DEAT");
        SKEL = Collections.unmodifiableSet(s);
    }

    private static void extractSkeleton(final TreeNode<GedcomLine> indi, final PrintWriter out) {
        out.println(indi);
        for (final TreeNode<GedcomLine> c : indi) {
            if (SKEL.contains(c.getObject().getTagString())) {
                out.println(c);
                for (final TreeNode<GedcomLine> c2 : c) {
                    if (c2.getObject().getTag().equals(GedcomTag.DATE)) {
                        out.println(c2);
                    }
                }
            }
        }
    }
}
