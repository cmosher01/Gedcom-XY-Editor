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

package nu.mine.mosher.gedcom;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestGedcomIndiName {
    @Test
    void nominal() {
        final var s = "Christopher Alan /Mosher/";
        t("Christopher Alan", "Mosher", "", s);
    }

    @Test
    void blank() {
        t("", "", "", "");
    }

    @Test
    void noSlashes() {
        t("This is a name", "", "", "This is a name");
    }

    @Test
    void II() {
        t("William", "Jones", "II", "William /Jones/ II");
    }

    @Test
    void loneSurname() {
        t("", "Smith", "", " / Smith / ");
    }

    @Test
    void singleSlash() {
        t("Mal/formed", "", "", "Mal/formed");
    }

    @Test
    void longname() {
        final var in = "Johann Gambolputty /de von Ausfern Schplenden Schlitter Crasscrenbon Fried/ of Ulm";
        t(
            "Johann Gambolputty",
            "de von Ausfern Schplenden Schlitter Crasscrenbon Fried",
            "of Ulm",
            in);
    }

    private void t(final String expG0, final String expS, final String expG1, final String input) {
        final var uut = GedcomIndiName.create(input);
        assertEquals(expG0, uut.given0());
        assertEquals(expS , uut.sur   ());
        assertEquals(expG1, uut.given1());
    }
}
