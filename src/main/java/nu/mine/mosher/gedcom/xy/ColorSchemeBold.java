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

import javafx.scene.paint.Color;
import nu.mine.mosher.gedcom.xy.util.Solarized;

public class ColorSchemeBold implements ColorScheme {
    @Override
    public boolean bold() { return true; }

    @Override
    public Color bg() {
        return Color.BEIGE;
    }

    @Override
    public Color lines() {
        return Color.BLACK;
    }

    @Override
    public Color linesSel() {
        return Solarized.MAGENTA;
    }

    @Override
    public Color indiBg() {
        return Color.WHITE;
    }

    @Override
    public Color indiText() {
        return Color.BLACK;
    }

    @Override
    public Color indiBorder() {
        return Color.DARKGRAY;
    }

    @Override
    public Color indiBorderDirty() {
        return Solarized.BLUE;
    }

    @Override
    public Color indiSelBg() {
        return Solarized.BASE2;
    }

    @Override
    public Color indiSelText() {
        return Solarized.MAGENTA;
    }

    @Override
    public Color selector() {
        return Solarized.MAGENTA;
    }
}
