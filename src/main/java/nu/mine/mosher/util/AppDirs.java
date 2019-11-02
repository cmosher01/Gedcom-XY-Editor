package nu.mine.mosher.util;

import net.harawata.appdirs.AppDirsFactory;
import org.slf4j.*;

import java.io.File;
import java.nio.file.*;
import java.util.regex.*;

/**
 * net.harawata:appdirs
 */
public class AppDirs {
    public static <T> AppDirs of(Class<T> cls) {
        return new AppDirs(cls);
    }

    public Path data() {
        return Paths.get(dirs.getUserDataDir(this.cls.getName(), null, null, true));
    }

    public Path config() {
        return Paths.get(dirs.getUserConfigDir(this.cls.getName(), null, null, true));
    }

    public Path cache() {
        return Paths.get(dirs.getUserCacheDir(this.cls.getName(), null, null));
    }

    public Path log() {
        return Paths.get(dirs.getUserLogDir(this.cls.getName(), null, null));
    }

    public File logFile() {
        final String name = hyphen(this.cls.getSimpleName())+".log";
        final Path path = log().resolve(name);
        final File file = path.toFile();
        file.getParentFile().mkdirs();
        return file;
    }

    private static final Pattern camel = Pattern.compile("(?=[A-Z0-9])");

    private static String hyphen(String s) {
        final Matcher matcher = camel.matcher(s);
        s = matcher.replaceAll("-");
        s = s.toLowerCase();
        if (s.startsWith("-")) {
            s = s.substring(1);
        }
        return s;
    }

    public void dump() {
        final Logger LOG = LoggerFactory.getLogger(AppDirs.class);
        LOG.debug("data dir: {}", data());
        LOG.debug("config dir: {}", config());
        LOG.debug("cache dir: {}", cache());
        LOG.debug("log dir: {}", log());
    }



    private final net.harawata.appdirs.AppDirs dirs = AppDirsFactory.getInstance();
    private final Class<?> cls;

    private AppDirs(final Class<?> cls) {
        this.cls = cls;
    }
}
