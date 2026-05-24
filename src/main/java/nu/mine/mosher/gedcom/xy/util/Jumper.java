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

package nu.mine.mosher.gedcom.xy.util;

/*
state:
    S = the current selection (center):Point2D
    J = the last saved jump point:Point2D
    curr pos = current scroll position (always exists)
user actions:
    s1 = user creates or adds to selection (so S exists)
    s0 = user clears selection (so S doesn't exist)
    j  = user presses the "J" key
operations:
    set J (saves current position to last saved jump point)
    JUMP TO Q: jump to point Q (J or S) (scrolls the canvas)
    NOP = no operation
    GOTO "Q" = transition to state named "Q"

state transitions:
    "sj": S doesn't exist, J doesn't exist   <-----(STARTING STATE)
        s0: NOP; GOTO "sj" (no transition)
        s1: NOP; GOTO "Sj"
        j : set J to curr pos; GOTO "sJ"
    "Sj": S exists, J doesn't exist
        s0: NOP; GOTO "sj"
        s1: NOP; GOTO "Sj" (no transition)
        j : set J to curr pos; JUMP TO S; GOTO "SJ"
    "sJ": S doesn't exist, J exists
        s0: NOP; GOTO "sJ" (no transition)
        s1: NOP; GOTO "SJ"
        j : (get curr pos); JUMP TO J; set J to curr pos; GOTO "sJ" (no transition)
    "SJ: S exists, J exists
        s0: NOP; GOTO "sJ"
        s1: NOP; GOTO "SJ" (no transition)
        j : (get curr pos);
            JUMP TO: if J = curr pos then S else J;
            set J to curr pos;
            GOTO "SJ" (no transition)
 */


import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import javafx.geometry.Point2D;

import static nu.mine.mosher.gedcom.xy.util.Jumper.State.*;

public class Jumper {
    enum State {
        sj, Sj, sJ, SJ
    }

    private static final Point2D NO_POINT = new Point2D(Double.NaN, Double.NaN);
    private static final double EPSILON = 0.06D;

    private final AtomicReference<Point2D> J = new AtomicReference<>(NO_POINT);
    private final AtomicReference<Point2D> S = new AtomicReference<>(NO_POINT);

    private State state = sj;

    // We don't get notified when the user changes the selection,
    // so we need to check the which action the user did to the
    // selection before pressing "J" key.
    public void selectionDelta(final Optional<Point2D> selection) {
        if (selection.isPresent()) {
            this.S.set(selection.get());
            userAddedToSelection();
        } else {
            this.S.set(NO_POINT);
            userClearedSelection();
        }
    }

    // s0 = user clears selection (so S doesn't exist)
    private void userClearedSelection() {
        switch (this.state) {
            case sj -> {
                // NOP; GOTO "sj" (no transition)
            }
            case Sj -> {
                // NOP; GOTO "sj"
                this.state = sj;
            }
            case sJ -> {
                // NOP; GOTO "sJ" (no transition)
            }
            case SJ -> {
                // NOP; GOTO "sJ"
                this.state = sJ;
            }
        }
    }

    // s1 = user creates or adds to selection (so S exists)
    private void userAddedToSelection() {
        switch (this.state) {
            case sj -> {
                // NOP; GOTO "Sj"
                this.state = Sj;
            }
            case Sj -> {
                // NOP; GOTO "Sj" (no transition)
            }
            case sJ -> {
                // NOP; GOTO "SJ"
                this.state = SJ;
            }
            case SJ -> {
                // NOP; GOTO "SJ" (no transition)
            }
        }
    }

    // j  = user presses the "J" key
    // Returns a point to the caller, who is responsible
    // for scrolling to that point (if it exists)
    public Optional<Point2D> userPressedJ(final Point2D ptCurrPos) {
        Optional<Point2D> ret = Optional.empty();
        switch (this.state) {
            case sj -> {
                // set J to curr pos; GOTO "sJ"
                this.J.set(ptCurrPos);
                this.state = sJ;
            }
            case Sj -> {
                // set J to curr pos; JUMP TO S; GOTO "SJ"
                this.J.set(ptCurrPos);
                ret = Optional.of(this.S.get());
                this.state = SJ;
            }
            case sJ -> {
                // (get curr pos); JUMP TO J; set J to curr pos; GOTO "sJ" (no transition)
                ret = Optional.of(this.J.get());
                this.J.set(ptCurrPos);
            }
            case SJ -> {
                // (get curr pos);
                // JUMP TO: if curr pos = S then J else S;
                // set J to curr pos;
                // GOTO "SJ" (no transition)
                final var to = (ptEqual(ptCurrPos,this.S.get())) ? this.J.get() : this.S.get();
                this.J.set(ptCurrPos);
                ret = Optional.of(to);
            }
        }
        return ret;
    }

    private static boolean ptEqual(final Point2D a, final Point2D b) {
        return Math.abs(a.getX()-b.getX()) < EPSILON && Math.abs(a.getY()-b.getY()) < EPSILON;
    }
}
