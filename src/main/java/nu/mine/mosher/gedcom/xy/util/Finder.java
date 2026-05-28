/*
 *     Copyright © 2026, Christopher Alan Mosher, New York, New York, USA, <cmosher01@gmail.com>.
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

import nu.mine.mosher.gedcom.xy.*;

import java.util.*;

// TODO hardcoded to Indi; how should we make it generic?
// TODO alternative algorithm: upon initial find(), create circular Deque of all matches
public class Finder {
    private final List<Indi> indis;
    private final Scrollable scrollable;

    private String lastQuery = "";
    private int lastIndex = -1;



    public Finder(final List<Indi> indis, final Scrollable scrollable) {
        this.indis = indis;
        this.scrollable = scrollable;

        assert !isQuery(this.lastQuery);
    }



    public void find(final String query) {
        if (!isQuery(query)) {
            return;
        }
        setQuery(query);

        boolean found = false;
        int i = this.lastIndex;
        i = incToEnd(i);
        while (!found && 0 <= i && i < this.indis.size()) {
            if (matches(i)) {
                found = true;
            } else {
                i = incToEnd(i);
            }
        }
        if (found) {
            foundAt(i);
        } else {
            clear();
        }
    }

    public void next() {
        if (!isQuery(this.lastQuery)) {
            return;
        }

        boolean found = false;
        int i = this.lastIndex;
        i = incModulo(i);
        int sane = 1_000_000;
        while (!found && 0 <= --sane) {
            if (matches(i)) {
                found = true;
            } else {
                i = incModulo(i);
            }
        }
        if (found) {
            foundAt(i);
        } else {
            // should never happen
            clear();
        }
    }

    public void prev() {
        if (!isQuery(this.lastQuery)) {
            return;
        }

        boolean found = false;
        int i = this.lastIndex;
        i = decModulo(i);
        int sane = 1_000_000;
        while (!found && 0 <= --sane) {
            if (matches(i)) {
                found = true;
            } else {
                i = decModulo(i);
            }
        }
        if (found) {
            foundAt(i);
        } else {
            // should never happen
            clear();
        }
    }



    private void foundAt(final int i) {
        this.lastIndex = i;
        this.scrollable.scaleTo(2.0D);
        this.scrollable.scrollTo(this.indis.get(i).xyUser());
    }

    private void clear() {
        setQuery("");
        assert !isQuery(this.lastQuery);
    }

    private void setQuery(String query) {
        if (Objects.isNull(query)) {
            query = "";
        }
        this.lastQuery = query.strip().toLowerCase();
        this.lastIndex = -1;
    }

    private boolean matches(final int i) {
        final var indi = this.indis.get(i).nameSimple().toLowerCase();
        return indi.contains(this.lastQuery);
    }

    // returns -1 to indicate gone past end
    private int incToEnd(int i) {
        ++i;
        if (i <= 0) {
            return 0;
        }
        if (this.indis.size() <= i) {
            return -1;
        }
        return i;
    }

    private int incModulo(int i) {
        ++i;
        if (this.indis.size() <= i) {
            i = 0;
        }
        return i;
    }

    private int decModulo(int i) {
        if (i <= 0) {
            i = this.indis.size();
        }
        --i;
        return i;
    }



    private static boolean isQuery(final String query) {
        return Objects.nonNull(query) && !query.isBlank();
    }
}
