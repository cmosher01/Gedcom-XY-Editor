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

import nu.mine.mosher.gedcom.xy.undo.ModificationTracker;

import java.util.Map;



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
