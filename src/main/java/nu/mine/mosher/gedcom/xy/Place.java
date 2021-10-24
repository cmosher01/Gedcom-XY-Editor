package nu.mine.mosher.gedcom.xy;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Taken from Ftm-Web-View

public class Place
{
    private static final Logger LOG =  LoggerFactory.getLogger(Place.class);

    private final List<String> hierarchy;
    private final String description;

    private Place(List<String> hierarchy, String description) {
        this.hierarchy = hierarchy;
        this.description = description;
    }

    @Override
    public String toString() {
        return this.description;
    }

    public static Place fromFtmPlace(final String s) {
        final Place place = new Builder(s).build();
        LOG.debug("FtmPlace=\"{}\" --> \"{}\"", s, place);
        return place;
    }

    @Override
    public boolean equals(final Object object) {
        if (!(object instanceof Place)) {
            return false;
        }
        final Place that = (Place)object;
        return this.hierarchy.equals(that.hierarchy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.hierarchy);
    }












    private static class Builder {
        private String description;
        private final List<String> hierarchy = new ArrayList<>(5);

        public Builder(final String description) {
            if (Objects.isNull(description) || description.isBlank()) {
                this.description = "";
            } else {
                // default value if any parsing fails:
                this.description = String.format("\u201C%s\u201D", description);
                // try to parse
                try {
                    parseDescription(description);
                } catch (final Throwable e) {
                    LOG.warn("unknown place name format for {}", description, e);
                }
            }
        }

        public Place build() {
            return new Place(hierarchy, description);
        }


        /*
        Family Tree Maker does some encoding within the place name column.

        There are two main types of place names.

        Both types start with a slash.

        Both types end with

               ... [CODE] / [LATITUDE] / [LONGITUDE]

        where CODE could be negative (which is obviously a flag that represents something), and
        LATITUDE/LONGITUDE are coordinates, in radians.

        Immediately preceding this will be either a slash or a vertical bar, which
        is used as an indicator of the main type of the place name, and dictates the
        format of the rest of the string preceding that:


        "/" Slashed place names are in this format:

            / [p0] / [p1] / [p2] / [p3] / [p4] / ...

        where p0 through p4 are place names in a hierarchy.
        The parts of "resolved" places are at the end.


        "|" Vertical bar place names are in this format:

            / [name] | ...

         */


        /*
                /Hamilton, Madison, New York, USA|/0.7474722/-1.318502
                /Place, Name w/some slash/es | and, vertical | bars|//
         */
        private static final Pattern FTM_PLACE_WITH_VERTICALBAR = Pattern.compile("^/(?<name>.*)\\|(?<code>[^/|]*?)/(?<lat>[^/|]*?)/(?<lon>[^/|]*?)$");

        /*
                /Room 401, Flint Hall, Syracuse University/Syracuse/Onondaga/New York/USA/11269/0.7513314/-1.329023
                /another place / with slashes | and  bars, but, resolved, in///Connecticut/USA/-9//

            Use the first capture group ("name") from the first pattern as input to the second pattern:
        */
        private static final Pattern FTM_PLACE_WITH_SLASH = Pattern.compile("^/(?<name>.*)/(?<code>[^/|]*?)/(?<lat>[^/|]*?)/(?<lon>[^/|]*?)$");

        private static final Pattern FTM_PLACE_HIERARCHICAL = Pattern.compile("^(?<p0>.*)/(?<p1>[^/|]*?)/(?<p2>[^/|]*?)/(?<p3>[^/|]*?)/(?<p4>[^/|]*?)$");


        private void parseDescription(final String description) {
            //-------------------------------------------------------
            {
                final Matcher withBar = FTM_PLACE_WITH_VERTICALBAR.matcher(description);
                if (withBar.matches()) {
                    parseAndAddHierarchy(withBar.group("name"));

                    buildDescription();
                    return;
                }
            }
            //-------------------------------------------------------
            {
                final Matcher withSlash = FTM_PLACE_WITH_SLASH.matcher(description);
                if (withSlash.matches()) {
                    final Matcher hier = FTM_PLACE_HIERARCHICAL.matcher(withSlash.group("name"));
                    if (hier.matches()) {
                        parseAndAddHierarchy(hier.group("p0"));
                        addHierarchy(hier.group("p1"));
                        addHierarchy(hier.group("p2"));
                        addHierarchy(hier.group("p3"));
                        addHierarchy(hier.group("p4"));
                    } else {
                        parseAndAddHierarchy(withSlash.group("name"));
                    }

                    buildDescription();
                }
            }
            //-------------------------------------------------------
        }

        private void buildDescription() {
            this.description = String.join(", ", this.hierarchy);
        }

        private void parseAndAddHierarchy(final String csvParts) {
            Arrays.stream(csvParts.split(",")).
                map(String::trim).
                forEach(this::addHierarchy);
        }

        private void addHierarchy(String part) {
            if (!part.isBlank()) {
                this.hierarchy.add(part);
            }
        }
    }
}
