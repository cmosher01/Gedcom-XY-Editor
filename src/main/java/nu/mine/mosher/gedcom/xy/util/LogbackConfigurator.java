package nu.mine.mosher.gedcom.xy.util;

import ch.qos.logback.classic.*;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.*;
import ch.qos.logback.core.*;
import ch.qos.logback.core.pattern.color.*;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.util.StatusPrinter;
import nu.mine.mosher.io.LogFiles;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

public class LogbackConfigurator extends ContextAwareBase implements Configurator {
    static {
        org.slf4j.bridge.SLF4JBridgeHandler.removeHandlersForRootLogger();
        org.slf4j.bridge.SLF4JBridgeHandler.install();
        java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.FINEST);
    }

    protected LogbackConfigurator() {
    }

    private static String file = "";

    @Override
    public void configure(final LoggerContext ctx) {
        addInfo("Logback configurator:     "+LogbackConfigurator.class.getCanonicalName());
        addInfo("application configurator: "+getClass().getCanonicalName());
        addInfo("application:              "+getClass().getEnclosingClass().getCanonicalName());
        buildLogFilePath();

        ctx.setName(getClass().getEnclosingClass().getCanonicalName());
        ctx.setPackagingDataEnabled(true);

        final Appender<ILoggingEvent> appender = buildAppender(ctx);

        final Logger LOG_ROOT = ctx.getLogger(Logger.ROOT_LOGGER_NAME);
        LOG_ROOT.setLevel(Level.TRACE);
        LOG_ROOT.addAppender(appender);

        StatusPrinter.print(ctx);
        System.out.flush();
        System.err.flush();
    }

    public static String getFilePath() {
        return file;
    }

    private Appender<ILoggingEvent> buildAppender(final LoggerContext ctx) {
        final FileAppender<ILoggingEvent> appender = new FileAppender<>();
        appender.setContext(ctx);
        appender.setFile(LogbackConfigurator.getFilePath());
        appender.setName("LogFile");
        appender.setImmediateFlush(true);

        HighlightingCompositeConverter.install();
        final PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(ctx);
        encoder.setPattern(pattern());
        encoder.setCharset(StandardCharsets.UTF_8);
        encoder.setOutputPatternAsHeader(true);
        encoder.start();

        appender.setEncoder(encoder);
        appender.start();
        return appender;
    }

    private void buildLogFilePath() {
        final SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd'T'HHmmssX");
        fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        final String ts = fmt.format(new Date());

        final File f = LogFiles.getLogFileOf(getClass().getEnclosingClass());
        final String tsname = ts+"_"+f.getName();

        LogbackConfigurator.file = Paths.get(f.getParent()).resolve(tsname).toString();
    }


    public static void testSubsystem() {
        org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(LogbackConfigurator.class);

        LOG.warn("================= TESTING LOGGING SUBSYSTEM =================");

        testSlf4j(LOG);
        testLog4j();
        testJcl();
        testJul();

        LOG.warn("================= LOGGING SUBSYSTEM TEST COMPLETE =================");
    }

    private static void testSlf4j(org.slf4j.Logger LOG) {
        final Throwable e = new Throwable("TEST EXAMPLE EXCEPTION (this is only a test)");
        e.fillInStackTrace();

        LOG.trace("testing TRACE level log");
        LOG.debug("testing DEBUG level log");
        LOG.info ("testing INFO  level log");
        LOG.warn ("testing WARN  level log");
        LOG.error("testing ERROR level log");
        LOG.warn ("testing stack trace log", e);
    }

    private static void testLog4j() {
        final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(LogbackConfigurator.class);
        logger.fatal("Testing org.apache.log4j FATAL log level: OK");
        logger.debug("Testing org.apache.log4j DEBUG log level.");
        logger.trace("Testing org.apache.log4j TRACE log level: "+
            "(This log message should be formatted according to the Logback configuration, not org.apache.log4j).");
    }

    private static void testJcl() {
        final org.apache.commons.logging.Log logger = org.apache.commons.logging.LogFactory.getLog(LogbackConfigurator.class);
        logger.fatal("Testing org.apache.commons.logging FATAL log level: OK");
        logger.trace("Testing org.apache.commons.logging TRACE log level: "+
            "(This log message should be formatted according to the Logback configuration, not org.apache.commons.logging).");
    }

    private static void testJul() {
        final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(LogbackConfigurator.class.getCanonicalName());
        logger.severe("Testing java.util.logging SEVERE log level: OK");
        logger.finest("Testing java.util.logging FINEST log level: "+
            "(This log message should be formatted according to the Logback configuration, not java.util.logging).");
    }



    protected String pattern() {
        return
            "%d{\"yyyy-MM-dd'T'HH:mm:ss.SSSXXX\",UTC} " +
            "%levelcolor(%-5p) " +
            "%gray([%t]) " +
            "%cyan(%c{36}#%M{18}) " +
            "%levelcolor(%replace(%msg){'\\p{Cntrl}',' '}){}%n";
    }



    private static final Map<Level, String> colors = Map.of(
        Level.ERROR, ANSIConstants.BOLD + ANSIConstants.RED_FG,
        Level.WARN, ANSIConstants.RED_FG,
        Level.INFO, ANSIConstants.DEFAULT_FG,
        Level.DEBUG, ANSIConstants.GREEN_FG,
        Level.TRACE, ANSIConstants.BOLD + ANSIConstants.BLACK_FG);

    public static class HighlightingCompositeConverter extends ForegroundCompositeConverterBase<ILoggingEvent> {
        public static void install() {
            PatternLayout.defaultConverterMap.put("levelcolor", HighlightingCompositeConverter.class.getName());
        }

        @Override
        public String getForegroundColorCode(final ILoggingEvent event) {
            return colors.getOrDefault(event.getLevel(), ANSIConstants.DEFAULT_FG);
        }
    }
}
