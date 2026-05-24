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

import javafx.geometry.*;
import nu.mine.mosher.gedcom.xy.shape.ShapeUtils;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TestBounds {
    final Bounds b_0011 = b(0,0,1,1);
    final Bounds b_5511 = b(5,5,1,1);
    final Bounds b_0066 = b(0,0,6,6);

    @Test
    void fixtures() {
        final var actual = ShapeUtils.addBounds(b_0011, b_5511);
        assertEquals(b_0066, actual);
    }

    @Test
    void existing() {
        final var actual = uutExisting(List.of(b_0011, b_5511));
        assertEquals(b_0066, actual);
    }

    @Test
    void proposed() {
        final var actual = uutProposed(List.of(b_0011, b_5511));
        assertEquals(b_0066, actual);
    }




    public Bounds uutExisting(final Collection<Bounds> rb) {
        Bounds bounds = null;
        for (final var i : rb) {
            if (Objects.isNull(bounds)) {
                bounds = i;
            } else {
                bounds = ShapeUtils.addBounds(bounds, i);
            }
        }
        return bounds;
    }

    public Bounds uutProposed(final Collection<Bounds> rb) {
        return rb.stream().reduce(ShapeUtils::addBounds).get();
    }

    private static Bounds b(final double x, final double y, final double w, final double h) {
        return new BoundingBox(x,y,w,h);
    }
}
