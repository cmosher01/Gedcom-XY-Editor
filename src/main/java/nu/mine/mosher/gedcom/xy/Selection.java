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

import javafx.geometry.*;

import java.util.*;

/*
Note: drag() and nudge() are the two and only two actions
in the entire program that change the state of the tree.
*/
public class Selection {
    private final FamilyChart familyChart;
    private final Map<Indi,IndiMovement> indisSelected = new IdentityHashMap<>();
    private Point2D ptDraggedFrom;

    public Selection(final FamilyChart familyChart) {
        this.familyChart = familyChart;
    }



    public Bounds bounds() {
        // TODO refactor to use stream, and don't return null when selection is empty (use Optional)
        Bounds bounds = null;
        for (final var i : this.indisSelected.keySet()) {
            if (Objects.isNull(bounds)) {
                bounds = i.bounds();
            } else {
                bounds = FamilyChart.addBounds(bounds, i.bounds());
            }
        }
        return bounds;
    }

    public void clear() {
        this.indisSelected.keySet().forEach(i -> i.select(false));
        this.indisSelected.clear();
    }

    public void select(final Indi indi, final boolean select, final boolean updateStatus) {
        indi.select(select);
        if (select) {
            if (!this.indisSelected.containsKey(indi)) {
                this.indisSelected.put(indi, IndiMovement.orig(indi.xyUser()));
            }
        } else {
            this.indisSelected.remove(indi);
        }
        if (updateStatus) {
            familyChart.updateSelectStatus();
        }
    }

    public void beginDrag(final Point2D from) {
        dumpEvent("beginDrag: beg:");
        this.ptDraggedFrom = from;
        familyChart.updateSelectStatus();
        dumpEvent("beginDrag: end:");
    }


    public void drag(final Point2D to) {
        dumpEvent("     Drag: beg:");
        final var d = to.subtract(this.ptDraggedFrom);
        this.indisSelected.keySet().forEach(i -> i.dragWithSnap(d));
        familyChart.updateSelectStatus();
        dumpEvent("     Drag: end:");
    }

    public void endDrag() {
        dumpEvent("  endDrag: beg:");
        moveAndSaveForUndo();
        dumpEvent("  endDrag: end:");
    }

    public void moveAndSaveForUndo() {
        this.indisSelected.replaceAll((indi, move) -> move.moveTo(indi.xyUser()));
        copyToModTracker();
        this.indisSelected.replaceAll((indi, move) -> move.anchor());
    }

    private void copyToModTracker() {
        final var moves = new IdentityHashMap<>(this.indisSelected);
        moves.values().removeIf(IndiMovement::unmoved);
        if (!moves.isEmpty()) {
            familyChart.modificationTracker().make(new MoveIndis(moves));
        }
    }

    private void dumpEvent(final String name) {
//        System.out.printf("%15s selected=%04d\n", name, indisSelected.size());
    }

    public void nudge() {
        if (!this.indisSelected.isEmpty()) {
            //  Nudge:
            //  Move selection near (graphically on the chart) to their
            //  "closest" (relationship-wise) unselected relative.

            //  maxr = empty
            //  maxd = -INF
            //  for each indi S in the selection
            //      for each related indi (parent, sibling, spouse, child) R of S
            //          if R is not in the selection
            //              find distance d from S to R
            //              if (maxd < d)
            //                  maxR = R
            //                  maxd = d
            //  if maxR is present
            //      move selection to below maxR
            //      scroll display to center upon maxR

            var maxR = Optional.<Indi>empty();
            var maxd = Double.NEGATIVE_INFINITY;
            for (final var S : this.indisSelected.keySet()) {
                for (final var R : S.getRelatives()) {
                    if (!R.selected()) {
                        final var d = R.distanceFrom(S);
                        if (maxd < d) {
                            maxR = Optional.of(R);
                            maxd = d;
                        }
                    }
                }
            }
            // If "C" (capital letter variable) represents a set of (x,y) coordinates,
            // then "cx" and "cy" variables represent the x and y of C.
            if (maxR.isPresent()) {
                final var R = maxR.get();
                final var rx = R.xyUser().getX();
                final var ry = R.xyUser().getY();

                final var sx = bounds().getCenterX();
                final var sy = bounds().getCenterY();

                final var dx = rx - sx;
                final var dy = (ry + familyChart.metrics().getGenDistance()) - sy;
                final var d = new Point2D(dx, dy);
                this.indisSelected.keySet().forEach(i -> i.dragWithSnap(d));
                this.familyChart.updateSelectStatus();

                // scroll display to relative we just moved to
                // (not the group we just moved)
                final var to = R.xyUser();
                this.familyChart.scrollable().scrollTo(to);
            }
            moveAndSaveForUndo();
        }
    }

    public Optional<Point2D> center() {
        Optional<Point2D> ret = Optional.empty();
        if (!this.indisSelected.isEmpty()) {
            final var b = bounds();
            ret = Optional.of(new Point2D(b.getCenterX(), b.getCenterY()));
        }
        return ret;
    }

    public record IndiMovement(Point2D ptOrig, Point2D ptDest) {
        public static IndiMovement orig(final Point2D ptOrig) {
            return new IndiMovement(ptOrig, ptOrig);
        }
        public IndiMovement moveTo(final Point2D ptDest) {
            return new IndiMovement(ptOrig(), ptDest);
        }
        public IndiMovement anchor() {
            return orig(ptDest());
        }
        public boolean unmoved() {
            return Math.abs(ptDest().magnitude()-ptOrig().magnitude()) < 1.0e-3D;
        }
    }
}
