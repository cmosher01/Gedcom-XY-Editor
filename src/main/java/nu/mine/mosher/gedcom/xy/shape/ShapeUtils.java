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

package nu.mine.mosher.gedcom.xy.shape;

import javafx.geometry.*;

public final class ShapeUtils {
    @Deprecated
    private ShapeUtils() {
        throw new UnsupportedOperationException();
    }

    public static Bounds addBounds(final Bounds b1, final Bounds b2) {
        final var minX = Math.min(b1.getMinX(), b2.getMinX());
        final var minY = Math.min(b1.getMinY(), b2.getMinY());
        final var maxX = Math.max(b1.getMaxX(), b2.getMaxX());
        final var maxY = Math.max(b1.getMaxY(), b2.getMaxY());
        return new BoundingBox(minX, minY, maxX - minX, maxY - minY);
    }
}
