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

import java.util.*;

public final class MathUtil {
    @Deprecated
    private MathUtil() {
        throw new UnsupportedOperationException();
    }

    public static double median(final List<Double> rn){
        final var values = new ArrayList<>(rn);
        values.sort(null);

        final double ret;
        final int n = values.size();
        if (n == 0) {
            ret = 0.0D;
        } else {
            final int h = n / 2;
            if (n % 2 == 1) {
                ret = values.get(h);
            } else {
                ret = (values.get(h-1) + values.get(h)) / 2.0D;
            }
        }
        return ret;
    }
}
