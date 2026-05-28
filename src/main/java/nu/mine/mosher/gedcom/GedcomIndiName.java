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

import java.util.List;
import java.util.regex.*;

/**
 * <p>
 *     Handles GEDCOM style name, with slashes flanking surname.
 * </p>
 *
 * <p>
 *     This class handles the <code>NAME_PERSONAL</code> primitive element
 *     defined in The GEDCOM Standard, Release 5.5.1, p. 54.
 * </p>
 *
 *
 */



public record GedcomIndiName(String given0, String sur, String given1) {
/*
NAME_PERSONAL :=
[
    <NAME_TEXT> |
    /<NAME_TEXT>/ |
    <NAME_TEXT> /<NAME_TEXT>/ |
    /<NAME_TEXT>/ <NAME_TEXT> |
    <NAME_TEXT> /<NAME_TEXT>/ <NAME_TEXT>
]

Restated:  [T] [ "/" [T] "/"  [T] ]
For parsing purposes, we treat T as a string of length
at least one character, except "/".

Member variables represent: given0 "/" sur "/" given1
*/
    private static final Pattern PAT_NAME = Pattern.compile("(.*)/([^/]*?)/([^/]*?)");

    public static GedcomIndiName create(final String gedcomName) {
        final Matcher matcher = PAT_NAME.matcher(gedcomName);
        if (!matcher.matches()) {
            return new GedcomIndiName(gedcomName, "", "");
        }
        return new GedcomIndiName(
            matcher.group(1).strip(),
            matcher.group(2).strip(),
            matcher.group(3).strip());
    }

    public String tildeGiven() {
        if (!given0().isBlank() && !given1().isBlank()) {
            return given0()+" ~ "+given1();
        }
        if (!given0().isBlank()) {
            return given0();
        }
        if (!given1().isBlank()) {
            return "~ "+given1();
        }
        return "";
    }



    public String simple() {
        return (given0()+" "+sur()+" "+given1()).strip();
    }



    /*
    8 cases:
        0 s 1   items     result
        -----   -------   ------
        f f f   [empty]   "?"                         [***]
        f f t   // g1     "?" + " " + "?" + " " + g1  [***]
        f t f   /s/       "?" + " " + s
        f t t   /s/ g1    "?" + " " + s   + " " + g1
        t f f   g0        g0
        t f t   g0 // g1  g0  + " " + g1              [***]
        t t f   g0 /s/    g0  + " " + s
        t t t   g0 /s/ g1 g0  + " " + s   + " " + g1

    *** These cases are not technically allowed by the GEDCOM standard.
        (NAME_PERSONAL and NAME_TEXT each have a minimum length of one character.)
        We allow them and handle them correctly, anyway.
    */
    public enum Token {
        NULL,
        GIVEN0, SUR, GIVEN1,
        SPACE,
        UNKNOWN,
    }

    public List<Token> tokenized() {
        final List<Token> ret;

        final var g0 = !given0().isBlank();
        final var s  = !sur   ().isBlank();
        final var g1 = !given1().isBlank();

        if (!g0 && !s  && !g1) {
            ret = List.of(Token.UNKNOWN,Token.NULL,Token.NULL,Token.NULL,Token.NULL);
        } else if (!g0 && !s  &&  g1) {
            ret = List.of(Token.UNKNOWN,Token.SPACE,Token.UNKNOWN,Token.SPACE,Token.GIVEN1);
        } else if (!g0 &&  s  && !g1) {
            ret = List.of(Token.UNKNOWN,Token.SPACE,Token.SUR,Token.NULL,Token.NULL);
        } else if (!g0 &&  s  &&  g1) {
            ret = List.of(Token.UNKNOWN,Token.SPACE,Token.SUR,Token.SPACE,Token.GIVEN1);
        } else if ( g0 && !s  && !g1) {
            ret = List.of(Token.GIVEN0,Token.NULL,Token.NULL,Token.NULL,Token.NULL);
        } else if ( g0 && !s  &&  g1) {
            ret = List.of(Token.GIVEN0,Token.NULL,Token.NULL,Token.SPACE,Token.GIVEN1);
        } else if ( g0 &&  s  && !g1) {
            ret = List.of(Token.GIVEN0,Token.SPACE,Token.SUR,Token.NULL,Token.NULL);
        } else if ( g0 &&  s  &&  g1) {
            ret = List.of(Token.GIVEN0,Token.SPACE,Token.SUR,Token.SPACE,Token.GIVEN1);
        } else {
            throw new IllegalStateException();
        }

        return ret;
    }
}
