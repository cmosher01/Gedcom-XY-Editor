package nu.mine.mosher.gedcom.xy;

import ch.qos.logback.classic.*;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Insets;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import nu.mine.mosher.gedcom.xy.util.*;
import org.slf4j.Logger;
import org.slf4j.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.prefs.Preferences;

public final class GenXyEditor {
    public static final String VERSION = GenXyEditor.class.getPackage().getImplementationVersion();

    private static Logger LOG;
    private static volatile Thread threadEventsAwt;
    private static volatile String arg0 = "";

    public static void main(final String... args) {
        try {
            initLogging();

            LOG.info("version: {}", VERSION);

            // TODO: handle args better
            if (0 < args.length) {
                arg0 = args[0];
            }

//            logFonts();

            initJdbc();
            SwingUtilities.invokeAndWait(GenXyEditor::initGui);

            if (Objects.nonNull(threadEventsAwt)) {
                LOG.info("Waiting for AWT thread to end...");
                threadEventsAwt.join();
                LOG.info("AWT thread ended.");
            }

            LOG.info("Exiting JavaFX platform...");
            Platform.exit();

            LOG.info("End of program.");
        } catch (final Throwable e) {
            logProgramTermination(e);
        }
    }

    private static void logFonts() {
        for (final Font font : GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts()) {
            LOG.info("Font: {}/{}/{}", font.getFontName(), font.getFamily(), font.getName());
        }
    }

    private static void initLogging() {
        LogbackConfigurator.testSubsystem();
        final LoggerContext ctx = (LoggerContext)LoggerFactory.getILoggerFactory();
        ctx.getLogger("sun.awt.X11").setLevel(Level.WARN);

        LOG = LoggerFactory.getLogger(GenXyEditor.class);
    }

    public static Preferences prefs() {
        return Preferences.userNodeForPackage(GenXyEditor.class);
    }

    public static File inDir() {
        final String def;
        final String other = prefs().get("outDir", "");
        if (!other.isEmpty()) {
            def = other;
        } else {
            def = "./";
        }
        return new File(prefs().get("inDir", def));
    }

    public static void inDir(final File dir) {
        prefs().put("inDir", dir.getAbsolutePath());
    }

    public static File outDir() {
        final String def;
        final String other = prefs().get("inDir", "");
        if (!other.isEmpty()) {
            def = other;
        } else {
            def = "./";
        }
        return new File(prefs().get("outDir", def));
    }

    public static void outDir(final File dir) {
        prefs().put("outDir", dir.getAbsolutePath());
    }

    private static void initGui() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Not running on event dispatch thread.");
        }

        threadEventsAwt = Thread.currentThread();

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (final Throwable e) {
            LOG.warn("Error trying to set the look and feel; ignoring it.", e);
        }

        final JFrame frame = new JFrame("FX");
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.setSize(1920, 800);

        final JFXPanel fxPanel = new JFXPanel(); // this also initializes JavaFX toolkit
        Platform.setImplicitExit(false);

        final CommandHandler cmd = new CommandHandler(frame);

        // TODO allow multiple open documents
        // TODO remove specialized Open handling (just make it File/Open menu item)
        final boolean destroy = Objects.nonNull(GenXyEditor.arg0) && arg0.equals("--destroy-layout");
        final Optional<FamilyChart> chart = cmd.openFile(destroy);
        if (chart.isEmpty()) {
            cmd.quitApp();
            return;
        }

        cmd.setAboutHandler();
        cmd.setQuitHandler(chart.get());

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cmd.quitIfSafe(chart.get());
            }
        });

        frame.setMenuBar(cmd.buildMenuBar(chart.get()));

        frame.setTitle("Genealogy XY Editor - " + chart.get().originalFile().get().getAbsolutePath());
        frame.add(fxPanel);
        frame.setVisible(true);

        Platform.runLater(() -> fxPanel.setScene(new Scene(buildGui(chart.get()))));
    }


    private static final String CLASS_DRIVER_JDBC = "org.sqlite.JDBC";

    private static void initJdbc() throws ClassNotFoundException, SQLException {
        LOG.debug("loading JDBC driver: {}...", CLASS_DRIVER_JDBC);
        LOG.info("successfully loaded JDBC driver class: {}", Class.forName(CLASS_DRIVER_JDBC).getCanonicalName());

        final Driver driverJdbc = DriverManager.getDriver("jdbc:sqlite:");
        LOG.info("JDBC driver version: major={},minor={}", driverJdbc.getMajorVersion(), driverJdbc.getMinorVersion());

        final Optional<java.util.logging.Logger> jdbcLogger = Optional.ofNullable(driverJdbc.getParentLogger());
        if (jdbcLogger.isPresent()) {
            jdbcLogger.get().info("Logging via JDBC driver logger: " + jdbcLogger);
        } else {
            LOG.info("JDBC driver logger not found.");
        }
    }

    private static void logProgramTermination(final Throwable e) {
        Objects.requireNonNull(e);
        if (Objects.nonNull(LOG)) {
            LOG.error("Program terminating due to error:", e);
        } else {
            try {
                final Path pathTemp = Files.createTempFile(GenXyEditor.class.getName()+"-", ".log");
                e.printStackTrace(new PrintStream(new FileOutputStream(pathTemp.toFile()), true));
            } catch (final Throwable reallyBad) {
                e.printStackTrace();
                reallyBad.printStackTrace();
            }
        }
    }

    private static Parent buildGui(final FamilyChart chart) {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException();
        }

        final Pane canvas = new Pane();
        canvas.setBackground(new Background(new BackgroundFill(chart.metrics().colors().bg(), CornerRadii.EMPTY, Insets.EMPTY)));

        chart.addGraphicsTo(canvas.getChildren());

        final ZoomPane workspace = new ZoomPane(canvas);
        workspace.setOnMouseClicked(t -> {
            if (t.isStillSincePress()) {
                chart.clearSelection();
                t.consume();
            }
        });

        final ObjectProperty<Point2D> selectStart = new SimpleObjectProperty<>();
        final ObjectProperty<Rectangle> selector = new SimpleObjectProperty<>();

        canvas.addEventFilter(MouseEvent.MOUSE_PRESSED, t -> {
            if (t.isShiftDown()) {
                selectStart.set(new Point2D(t.getX(), t.getY()));
                final Rectangle sel = new Rectangle(t.getX(), t.getY(), 0D, 0D);
                sel.setFill(Color.TRANSPARENT);
                sel.setStrokeWidth(1.0D);
                sel.setStroke(chart.metrics().colors().selector());
                sel.getStrokeDashArray().addAll(3.0D);
                canvas.getChildren().add(sel);
                chart.setSelectionFrom(sel.getX(), sel.getY(), sel.getWidth(), sel.getHeight());
                selector.set(sel);
                t.consume();
            }
        });

        canvas.addEventFilter(MouseEvent.MOUSE_DRAGGED, t -> {
            if (selector.isNotNull().get()) {
                final Rectangle sel = selector.get();

                final double x = selectStart.get().getX();
                final double w = t.getX() - x;
                if (w < 0D) {
                    sel.setX(t.getX());
                    sel.setWidth(-w);
                } else {
                    sel.setWidth(w);
                }

                final double y = selectStart.get().getY();
                final double h = t.getY() - y;
                if (h < 0D) {
                    sel.setY(t.getY());
                    sel.setHeight(-h);
                } else {
                    sel.setHeight(h);
                }

                chart.setSelectionFrom(sel.getX(), sel.getY(), sel.getWidth(), sel.getHeight());

                t.consume();
            }
        });

        canvas.addEventFilter(MouseEvent.MOUSE_RELEASED, t -> {
            if (selector.isNotNull().get()) {
                canvas.getChildren().remove(selector.get());
                t.consume();
                selector.set(null);
            }
        });


        final HBox statusbar = buildStatusBar(chart);


        final BorderPane root = new BorderPane();
        root.setCenter(workspace);
        root.setBottom(statusbar);

        return root;
    }

    private static HBox buildStatusBar(FamilyChart chart) {
        final Text statusName = new Text();
        statusName.textProperty().bind(chart.selectedName());

        final Text statusVersion = new Text(VERSION);

        final Region ws = new Region();
        HBox.setHgrow(ws, Priority.ALWAYS);
        return new HBox(statusName, ws, statusVersion);
    }
}
