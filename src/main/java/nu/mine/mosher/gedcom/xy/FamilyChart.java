package nu.mine.mosher.gedcom.xy;

import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;

import java.util.*;

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
        this.famis.forEach(Fami::calc);
        this.indis.forEach(Indi::setFromCoords);
    }
}
