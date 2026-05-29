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

import java.io.File;
import java.util.Objects;
import java.util.regex.*;

public class FileUtil {
    @Deprecated
    private FileUtil() {
        throw new UnsupportedOperationException();
    }

    private static final Pattern patFiletype = Pattern.compile("^.*\\.(.*)$");

    public static String filetypeOf(final File file) {
        final Matcher matcher = patFiletype.matcher(file.getName());
        if (!matcher.matches()) {
            return "";
        }
        final String ft = matcher.group(1);
        return Objects.isNull(ft) ? "" : ft;
    }
}
