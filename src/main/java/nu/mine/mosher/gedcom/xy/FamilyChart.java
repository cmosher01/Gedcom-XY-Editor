package nu.mine.mosher.gedcom.xy;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import nu.mine.mosher.collection.TreeNode;
import nu.mine.mosher.gedcom.Gedcom;
import nu.mine.mosher.gedcom.GedcomLine;
import nu.mine.mosher.gedcom.GedcomTag;
import nu.mine.mosher.gedcom.GedcomTree;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FamilyChart {
    private final GedcomTree tree;
    private final List<Indi> indis;
    private final List<Fami> famis;
    private final Metrics metrics;
    private final Selection selection = new Selection();

    public FamilyChart(final GedcomTree tree, final List<Indi> indis, final List<Fami> famis, final Metrics metrics) {
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
        this.indis.forEach(Indi::setFromCoords);
    }

    public void clearSelection() {
        this.selection.clear();
    }

    public void setSelectionFrom(double x, double y, double w, double h) {
        this.indis.forEach(i -> {
            this.selection.select(i, i.intersects(x,y,w,h));
        });
    }

    public Metrics metrics() {
        return this.metrics;
    }

    public void saveAs(final File file) throws IOException {
        this.indis.stream().filter(Indi::dirty).forEach(Indi::saveXyToTree);
        tree.timestamp();
        Gedcom.writeFile(tree, new BufferedOutputStream(new FileOutputStream(file)));
    }

    public void saveSkeleton(final File file) throws IOException {
        final PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)));

        out.println("0 HEAD");
        out.println("1 CHAR UTF-8");
        out.println("1 GEDC");
        out.println("2 VERS 5.5.1");
        out.println("2 FORM LINEAGE-LINKED");
        out.println("1 SOUR _XY EDITOR");

        this.indis.stream().filter(Indi::dirty).forEach(i -> {
            i.saveXyToTree();
            extractSkeleton(i.node(), out);
        });

        out.println("0 TRLR");

        if (out.checkError()) {
            System.err.println("ERROR exporting skeleton file.");
        }
        out.close();
    }

    public boolean dirty() {
        return this.indis.stream().anyMatch(Indi::dirty);
    }

    static class Selection {
        private final Set<Indi> indis = new HashSet<>();
        private Point2D orig;

        public void clear() {
            this.indis.forEach(i -> i.select(false));
            this.indis.clear();
        }
        public void select(final Indi indi, final boolean select) {
            indi.select(select);
            if (select) {
                this.indis.add(indi);
            } else {
                this.indis.remove(indi);
            }
        }
        public void beginDrag(final Point2D orig) {
            this.orig = orig;
        }
        public void drag(final Point2D to) {
            this.indis.forEach(i -> i.drag(to.subtract(this.orig)));
        }
    }



    private static final Set<String> SKEL = Set.of("NAME", "SEX", "REFN", "RIN", "_XY", "BIRT", "DEAT");

    private static void extractSkeleton(final TreeNode<GedcomLine> indi, final PrintWriter out) {
        out.println(indi);
        for (final TreeNode<GedcomLine> c : indi) {
            final String tag = c.getObject().getTagString();
            if (SKEL.contains(tag)) {
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
