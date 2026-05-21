package nu.mine.mosher.gedcom.xy;

import ch.qos.logback.classic.*;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Point2D;
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

// TODO convert to javafx.application.Application
public final class GenXyEditor {
    public static final String VERSION = GenXyEditor.class.getPackage().getImplementationVersion();

    private static Logger LOG;
    private static volatile Thread threadEventsAwt;
    private static volatile String arg0 = "";

    private static final String TITLE = "Genealogy XY Editor";

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
        final var chart = cmd.openFile(destroy);
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

        if (chart.get().originalFile().isPresent()) {
            frame.setTitle(TITLE + " - " + chart.get().originalFile().get().getAbsolutePath());
        } else {
            frame.setTitle(TITLE);
        }
        frame.add(fxPanel);
        frame.setVisible(true);


        Platform.runLater(() -> {
            final var scene = new Scene(buildGui(chart.get()));
            scene.setOnKeyPressed(t -> {
                chart.get().onKey(t);
                t.consume();
            });
            fxPanel.setScene(scene);
            // TODO why can't I ever get any zoom events?
//            scene.setOnRotate(t -> {
//                System.out.println("Rotate!");
//                t.consume();
//            });
//            scene.setOnZoom(t -> {
//                //workspace.zoomTowards(Math.exp(/*ZOOM_INTENSITY **/ t.getZoomFactor()), new Point2D(t.getSceneX(), t.getSceneY()));
//                System.out.println("ZOOM!");
//                t.consume();
//            });
//            scene.addEventFilter(ZoomEvent.ANY, t -> System.out.println("ZOOM FILTER"));

        });
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
                lastResortLog(e, reallyBad);
            }
        }
    }

    @SuppressWarnings("CallToPrintStackTrace")
    private static void lastResortLog(final Throwable e, final Throwable reallyBad) {
        e.printStackTrace();
        reallyBad.printStackTrace();
    }

    private static Parent buildGui(final FamilyChart chart) {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException();
        }

        final Pane canvas = new Pane();

//        canvas.setBackground(new Background(new BackgroundFill(chart.metrics().colors().bg(), CornerRadii.EMPTY, Insets.EMPTY)));
//        canvas.setBorder(new Border(new BorderStroke(
//            Color.DARKSLATEBLUE,
//            BorderStrokeStyle.DASHED,
//            CornerRadii.EMPTY,
//            new BorderWidths(3D)
//        )));

        // This helps with the scrolling problem when dragging people out of bounds:
        canvas.setPrefSize(0D,0D);

        chart.addGraphicsTo(canvas.getChildren());


//        final var workspace = new ZoomPane(canvas, chart.boundary());
        final var workspace = Scroller.create(canvas);
//        workspace.setScrollBarPolicy(GesturePane.ScrollBarPolicy.ALWAYS);
//        workspace.setFitMode(GesturePane.FitMode.FIT);
//        workspace.setGestureEnabled(true);
//        workspace.setBindScale(true);
//        workspace.setTranslateX(canvas.getBoundsInLocal().getMinX());
//        workspace.setTranslateY(canvas.getBoundsInLocal().getMinY());
//        workspace.setMaxSize(canvas.getBoundsInLocal().getWidth(),canvas.getBoundsInLocal().getHeight());
//        workspace.setScrollMode(GesturePane.ScrollMode.PAN);
//        workspace.setMinScale(1e-2D);
//        workspace.setMaxScale(1e+2D);
//        workspace.zoomTo(0.01D, Point2D.ZERO);

        chart.setWorkspace(workspace);
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
//                dumpPoint("pressed", t);
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
            } else {
//                dumpPoint("pressed [nop]", t);
            }
        });

        canvas.addEventFilter(MouseEvent.MOUSE_DRAGGED, t -> {
            if (selector.isNotNull().get()) {
//                dumpPoint("dragged", t);
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
            } else {
//                dumpPoint("dragged [nop]", t);
            }
        });

        canvas.addEventFilter(MouseEvent.MOUSE_RELEASED, t -> {
            if (selector.isNotNull().get()) {
//                dumpPoint("released", t);
                canvas.getChildren().remove(selector.get());
                t.consume();
                selector.set(null);
            } else {
//                dumpPoint("released [nop]", t);
            }
        });



        final StatusBar sb = buildStatusBar(chart);

        final var viewport = new Pane(workspace);
        clipToChildren(viewport);
        viewport.setMinSize(0,0);
        viewport.setOnMouseMoved(e -> updateStatusBar(sb,workspace,canvas,e));
        viewport.setOnMouseDragged(e -> updateStatusBar(sb,workspace,canvas,e));




        final BorderPane root = new BorderPane();
        root.setCenter(viewport);
        root.setBottom(sb);

        return root;
    }

//    private static void dumpPoint(final String name, final MouseEvent event) {
//        final var c_e = new Point2D(event.getSceneX(), event.getSceneY());
//        System.out.printf("rctselect: %8s  c=(%7.1f,%7.1f)\n",
//                name, c_e.getX(), c_e.getY());
//    }

    private static void updateStatusBar(final StatusBar sb, final Pane workspace, final Pane canvas, final MouseEvent e) {
        final var v_p = new Point2D(e.getX(), e.getY());
        final var w_p = workspace.parentToLocal(v_p);
        final var c_p = canvas.parentToLocal(w_p);
        sb.updateViewPort(v_p);
        sb.updateVpToCv(c_p);
    }

    private static StatusBar buildStatusBar(FamilyChart chart) {
        return new StatusBar();
    }

        // TODO
    private static HBox buildStatusBarORIGINAL(FamilyChart chart) {
        final Text statusName = new Text();
        statusName.textProperty().bind(chart.selectedName());

        final Text statusVersion = new Text(VERSION);

        final Region ws = new Region();
        HBox.setHgrow(ws, Priority.ALWAYS);
        return new HBox(statusName, ws, statusVersion);
    }

    private static void clipToChildren(final Pane pane) {
        final var clip = new Rectangle();
        clip.widthProperty().bind(pane.widthProperty());
        clip.heightProperty().bind(pane.heightProperty());
        pane.setClip(clip);
    }
}
