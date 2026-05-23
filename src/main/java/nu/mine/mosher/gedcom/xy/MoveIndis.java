package nu.mine.mosher.gedcom.xy;

import nu.mine.mosher.gedcom.xy.undo.ModificationTracker;

import java.util.*;



public class MoveIndis implements ModificationTracker.Modification {
    private final Map<Indi, Selection.IndiMovement> indis;

    public MoveIndis(final Map<Indi, Selection.IndiMovement> indis) {
        this.indis = Map.copyOf(indis);
    }

    @Override
    public void make() {
        // nothing to do, because any modifications were
        // already made, while the user was dragging
        this.indis.forEach(MoveIndis::dumpEvent);
    }

    private static void dumpEvent(final Indi indi, final Selection.IndiMovement move) {
//        System.out.printf("mods: make: orig=(%7.1f,%7.1f)  dest=(%7.1f,%7.1f)  indi=%s\n",
//            move.ptOrig().getX(), move.ptOrig().getY(), move.ptDest().getX(), move.ptDest().getY(), indi.name());
    }

    @Override
    public void undo() {
        this.indis.forEach((indi,move) -> indi.moveTo(move.ptOrig()));
    }

    @Override
    public void redo() {
        this.indis.forEach((indi,move) -> indi.moveTo(move.ptDest()));
    }
}
