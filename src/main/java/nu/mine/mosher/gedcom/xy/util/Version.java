package nu.mine.mosher.gedcom.xy.util;

import java.net.URL;
import java.util.jar.*;

public class Version {
    public static String version(final Package pkg) {
        final String urlManifest = String.format("jrt:/%s/META-INF/MANIFEST.MF", pkg.getName());
        try {
            final Manifest manifest = new Manifest(new URL(urlManifest).toURI().toURL().openStream());
            return manifest.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION);
        } catch (final Throwable e) {
            return "UNOFFICIAL VERSION";
        }
    }
}
