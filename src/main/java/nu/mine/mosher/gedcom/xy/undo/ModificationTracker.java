/*
    Copyright © 2026, Christopher Alan Mosher, New York, New York, USA, <cmosher01@gmail.com>.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package nu.mine.mosher.gedcom.xy.undo;

import java.util.*;

/**
 * Modification tracker, for undo/redo functionality.
 * Not thread-safe.
 * "make" means to make the modification.
 * "undo" means to undo (i.e., reverse) the modification.
 * "redo" means to re-make a previously undone modification.
 */
public class ModificationTracker {
    public interface Modification {
        void make();
        void undo();
        default void redo() {
            make();
        }
    }



    private final Queue<Modification> undoables = Collections.asLifoQueue(new LinkedList<>());
    private final Queue<Modification> redoables = Collections.asLifoQueue(new LinkedList<>());



    public void make(final Modification c) {
        c.make();
        this.undoables.add(c);
        this.redoables.clear();
    }

    public boolean canUndo() {
        return !this.undoables.isEmpty();
    }

    public void undo() {
        if (canUndo()) {
            final Modification c = undoables.remove();
            c.undo();
            this.redoables.add(c);
        }
    }

    public boolean canRedo() {
        return !this.redoables.isEmpty();
    }

    public void redo() {
        if (canRedo()) {
            final Modification c = redoables.remove();
            c.redo();
            this.undoables.add(c);
        }
    }
}
