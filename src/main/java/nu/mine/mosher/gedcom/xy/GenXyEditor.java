package nu.mine.mosher.gedcom.xy;

import ch.qos.logback.classic.*;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.input.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
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
/*
gedcom-xy-editor GUI user input/actions:

keypresses
	"command keys": as defined in menus
	N: "nudge"
		Moves the Selection nearer to the nearest non-selected parent,
		silbling, spouse, or child, of anyone in the Selection)
	R: "reset"
		Sets scale 1:1
	C: "center"
		Scrolls to the center of the chart
	F: "fit"
		Zooms to fit the whole chart on the screen.
	J: "jump" TODO
		Scrolls back ("jumps") to the current selection
		Subsequent jumps toggle between the current location and the selection.

mouse in content area:
	hover
		over an individual:
			hand pointing cursor
		otherwise:
			arrow cursor

	click
		if on an individual:
			toggle that individual into, or out off, the Selection
		otherwise:
			clear the Selection

	drag
		an individual in the Selection:
			moves all individuals in the Selection
		an individual not in the Selection:
			adds that individuals to the Selection and
			moves all individuals in the Selection
		not on an individual (i.e., the background of the canvas):
			visually move the entire canvas around under the window

	[SHIFT]drag:
		select all and only the individuals that intersect the rectange
		described by click-position and current-mouse-position


	scroll:
		visually zooms the canvas in or out
			pivoting at the mouse position
			within min/max limits






GUI node hierarchy:

frame:JFrame
fxPanel:JFXPanel
scene:Scene
root:BorderPane
viewport:Pane
workspace:Scroller:Pane
canvas:Pane
plaque:StackPane



custom event handling:

keyPressed -> scene:Scene -> chart:FamilyChart

[none for root:BorderPane]

mouseMoved/Dragged -> viewport:StackPane -> (update status bar coordinates)

mouseClicked -> workspace:Scroller -> chart.clearSelection
mousePressed/Dragged[T]/Released,scroll -> workspace:Scroller (scale/translate)

FILTER: [SHIFT]mousePressed/Dragged/Released -> canvas:Pane -> (rectangular select multiple)

mouseEntered/Exited -> plaque:StackPane
mouseClicked -> plaque:StackPane -> (just consumed)
mousePressed/Dragged[T]/Released -> plaque:StackPane -> drag selection
 */
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

        final var screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final int w = (int)Math.round(Math.rint(0.80D * screenSize.getWidth ()));
        final int h = (int)Math.round(Math.rint(0.80D * screenSize.getHeight()));
        frame.setSize(w, h);
        frame.setLocationRelativeTo(null);

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
//        final Pane canvas = new Pane();
//        canvas.setBackground(new Background(new BackgroundFill(Color.DARKGREEN, CornerRadii.EMPTY, Insets.EMPTY)));
//        canvas.setBorder(new Border(new BorderStroke(
//            Color.GREEN,
//            BorderStrokeStyle.DASHED,
//            CornerRadii.EMPTY,
//            new BorderWidths(3D)
//        )));

        // This helps with the scrolling problem when dragging people out of bounds:
        canvas.setPrefSize(0D,0D);

//        canvas.setMinSize(409D,409D);
//        canvas.setPrefSize(50_000D,50_000D);
//        canvas.setMaxSize(Double.MAX_VALUE,Double.MAX_VALUE);
        chart.addGraphicsTo(canvas.getChildren());

//        canvas.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
//        canvas.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
//        canvas.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

//        final var canvaswrapper = new BorderPane(canvas);
//        canvaswrapper.setCenter(canvas);

//        final var workspace = new ZoomPane(canvas, chart.boundary());
        final var workspace = Scroller.create(canvas);
        clipToChildren(workspace);
//        workspace.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
//        workspace.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
//        workspace.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
//        workspace.setBackground(new Background(new BackgroundFill(Color.DARKRED, CornerRadii.EMPTY, Insets.EMPTY)));
//        workspace.setBorder(new Border(new BorderStroke(
//                Color.RED,
//                BorderStrokeStyle.DASHED,
//                CornerRadii.EMPTY,
//                new BorderWidths(3D)
//        )));
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


        final StatusBar sb = StatusBar.create(chart);

        final var viewport = new StackPane(workspace);
//        viewport.setCenter(workspace);
        clipToChildren(viewport);
//        viewport.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        viewport.setMinSize(0,0);
//        viewport.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
//        viewport.setBorder(new Border(new BorderStroke(
//                Color.BLUE,
//                BorderStrokeStyle.DASHED,
//                CornerRadii.EMPTY,
//                new BorderWidths(3D)
//        )));
        viewport.setOnMouseMoved(e -> updateStatusBar(sb,workspace,canvas,e));
        viewport.setOnMouseDragged(e -> updateStatusBar(sb,workspace,canvas,e));

        final BorderPane root = new BorderPane();
        root.setCenter(viewport);
        root.setBottom(sb);

        return root;
    }

    private static void dumpBounds(final Region r) {
        final var bounds = new BoundingBox(r.getLayoutX(), r.getLayoutY(), r.getWidth(), r.getHeight());
        System.out.println("bounds: "+bounds);
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

    private static void clipToChildren(final Pane pane) {
        final var clip = new Rectangle();
        clip.widthProperty().bind(pane.widthProperty());
        clip.heightProperty().bind(pane.heightProperty());
        pane.setClip(clip);
    }
}
