package nu.mine.mosher.gedcom.xy;

import javafx.scene.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FamilyChart {
    private final List<Indi> indis;
    private final List<Fami> famis;

    public FamilyChart(final List<Indi> indis, final List<Fami> famis) {
        this.indis = Collections.unmodifiableList(new ArrayList<>(indis));
        this.famis = Collections.unmodifiableList(new ArrayList<>(famis));
    }

    public void addGraphicsTo(final List<Node> addto) {
        this.famis.forEach(f -> f.addGraphicsTo(addto));
        this.indis.forEach(i -> i.addGraphicsTo(addto));
    }

    public void setFromOrig() {
        this.indis.forEach(Indi::calc);
        this.famis.forEach(Fami::calc);
        this.indis.forEach(Indi::setFromCoords);
    }
}
