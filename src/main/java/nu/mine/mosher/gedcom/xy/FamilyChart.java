package nu.mine.mosher.gedcom.xy;

import javafx.geometry.Point2D;
import javafx.scene.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FamilyChart {
    private final List<Indi> indis;
    private final List<Fami> famis;
    private final Selection selection = new Selection();

    public FamilyChart(final List<Indi> indis, final List<Fami> famis) {
        this.indis = Collections.unmodifiableList(new ArrayList<>(indis));
        this.famis = Collections.unmodifiableList(new ArrayList<>(famis));
    }

    public void addGraphicsTo(final List<Node> addto) {
        this.famis.forEach(f -> f.addGraphicsTo(addto));
        this.indis.forEach(i -> i.addGraphicsTo(addto));
    }

    public void setFromOrig() {
        this.indis.forEach(i -> i.setSelection(selection));
        this.indis.forEach(Indi::calc);
        this.famis.forEach(Fami::calc);
        this.indis.forEach(Indi::setFromCoords);
    }

    public void clearSelection() {
        this.selection.clear();
    }

    static class Selection {
        private final Set<Indi> indis = new HashSet<>();
        private Point2D orig;

        public void clear() {
            indis.forEach(i -> i.select(false));
            indis.clear();
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
}
