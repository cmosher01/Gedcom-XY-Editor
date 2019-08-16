package nu.mine.mosher.gedcom.xy;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import nu.mine.mosher.gedcom.Gedcom;
import nu.mine.mosher.gedcom.GedcomTree;
import nu.mine.mosher.gedcom.xy.util.ZoomPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

public final class GenXyEditor extends Application {
    private static final String DEFAULT_LOG_LEVEL_KEY = "org.slf4j.simpleLogger.defaultLogLevel"; // from org.slf4j.simple.SimpleLogger

    private static Logger LOG;

    @Override
    public void init() {
        setLogLevel("warn");
        getParameters().getUnnamed().forEach(GenXyEditor::processArg);
        initLogging();
        testLogging();
    }

    private static void initLogging() {
        LOG = LoggerFactory.getLogger(GenXyEditor.class);

        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    private static void testLogging() {
        logTestStatus("BEGIN");
        LOG.error("Test: log level error");
        LOG.warn("Test: log level warn");
        LOG.info("Test: log level info");
        LOG.debug("Test: log level debug");
        LOG.trace("Test: log level trace");
        java.util.logging.Logger.getGlobal().severe("Testing java.util.logging handling.");
        logTestStatus("COMPLETE");
    }

    private static void logTestStatus(String status) {
        if (System.getProperty(DEFAULT_LOG_LEVEL_KEY, "").equals("off")) {
            return;
        }
        System.err.println(now()+" Logger test: "+status);
    }

    private static String now() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new Date());
    }

    /*
     *  Mapping of command line option to slf4j logging level:
     *  -q off
     *  [default] error
     *  [default] warn
     *  -v info
     *  -vv debug
     *  -vvv trace
     */
    private static void processArg(final String arg) {
        switch (arg) {
            case "-vvv":
                setLogLevel("trace");
                break;
            case "-vv":
                setLogLevel("debug");
                break;
            case "-v":
                setLogLevel("info");
                break;
            case "-q":
                setLogLevel("off");
                break;
        }
    }

    private static void setLogLevel(final String levelName) {
        System.setProperty(DEFAULT_LOG_LEVEL_KEY, levelName);
    }

    @Override
    public void start(final Stage stage) {
        stage.setTitle("GEDCOM _XY Editor");

        Platform.runLater(() -> {
            final FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("GEDCOM files", "*.ged"),
                    new FileChooser.ExtensionFilter("all files", "*.*"));
            final File fileToOpen = fileChooser.showOpenDialog(null);
            if (Objects.isNull(fileToOpen)) {
                LOG.warn("User cancelled opening file. Program will exit.");
                Platform.exit();
                return;
            }
            try {
                final GedcomTree tree = Gedcom.readFile(new BufferedInputStream(Files.newInputStream(fileToOpen.toPath())));
                final FamilyChart chart = FamilyChartBuilder.create(tree);
                chart.setFromOrig();

                stage.setOnCloseRequest(t -> {
                    if (!exitIfSafe(stage, chart)) {
                        t.consume();
                    }
                });
                Platform.setImplicitExit(false);

                stage.setScene(new Scene(buildGui(stage, chart), 1920, 800));
                stage.show();
            } catch (final Exception e) {
                Platform.exit();
                throw new IllegalStateException(e);
            }
        });
    }

    private boolean exitIfSafe(final Stage stage, final FamilyChart chart) {
        boolean safe = false;
        if (chart.dirty()) {
            final Alert alert = new Alert(Alert.AlertType.WARNING, "Your unsaved changes will be DISCARDED.", ButtonType.OK, ButtonType.CANCEL);
            alert.setTitle("Changes will be discarded");
            final Optional<ButtonType> response = alert.showAndWait();
            if (response.isPresent() && response.get() == ButtonType.OK) {
                LOG.warn("User confirmed discarding changes:");
                chart.indis().stream().filter(Indi::dirty).forEach(Indi::logDiscard);
                safe = true;
            }
        } else {
            safe = true;
        }
        if (safe) {
            stage.setOnCloseRequest(null);
            stage.close();
            Platform.exit();
        }
        return safe;
    }

    private Parent buildGui(final Stage stage, final FamilyChart chart) {
        final Pane canvas = new Pane();
        canvas.setBackground(new Background(new BackgroundFill(chart.metrics().colorBg(), CornerRadii.EMPTY, Insets.EMPTY)));

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
                sel.setStroke(chart.metrics().colorSelectionChooser());
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

        final Text statusName = new Text();
        statusName.textProperty().bind(chart.selectedName());

        final HBox statusbar = new HBox();
        statusbar.getChildren().add(statusName);

        final BorderPane root = new BorderPane();
        root.setTop(buildMenuBar(stage, chart));
        root.setCenter(workspace);
        root.setBottom(statusbar);

        return root;
    }

    private MenuBar buildMenuBar(final Stage stage, final FamilyChart chart) {
        final MenuItem cmdSaveAs = new MenuItem("Save As...");
        cmdSaveAs.setMnemonicParsing(true);
        cmdSaveAs.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN));
        cmdSaveAs.setOnAction(t -> {
            final FileChooser fileChooser = new FileChooser();
            final File file = fileChooser.showSaveDialog(stage);
            if (Objects.isNull(file)) {
                return;
            }
            try {
                chart.saveAs(file);
            } catch (final IOException e) {
                // TODO: this is not nice
                LOG.error("An error occurred while trying to save file, file={}", file, e);
            }
        });

        final MenuItem cmdExportDirty = new MenuItem("Export Changed As Skeletons...");
        cmdExportDirty.setMnemonicParsing(true);
        cmdExportDirty.setAccelerator(new KeyCodeCombination(KeyCode.K, KeyCombination.SHORTCUT_DOWN));
        cmdExportDirty.setOnAction(t -> {
            exportSkel(stage, chart, false);
        });

        final MenuItem cmdExportAll = new MenuItem("Export ALL As Skeletons...");
        cmdExportAll.setMnemonicParsing(true);
        cmdExportAll.setAccelerator(new KeyCodeCombination(KeyCode.K, KeyCombination.SHORTCUT_DOWN));
        cmdExportAll.setOnAction(t -> {
            exportSkel(stage, chart, true);
        });

        final Menu menuFile = new Menu("File");
        menuFile.getItems().addAll(cmdExportDirty, cmdExportAll, new SeparatorMenuItem(), cmdSaveAs);

        /* Mac platform provides its own Quit on the system menu */
        if (!mac()) {
            final MenuItem cmdQuit = new MenuItem("Quit");
            cmdQuit.setMnemonicParsing(true);
            cmdQuit.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN));
            cmdQuit.setOnAction(t -> exitIfSafe(stage, chart));
            menuFile.getItems().addAll(new SeparatorMenuItem(), cmdQuit);
        }


        final MenuItem cmdNorm = new MenuItem("Normalize ALL Coordinates");
        cmdNorm.setOnAction(t -> {
            final Alert alert = new Alert(Alert.AlertType.INFORMATION, "This will normalize the coordinates of all people.", ButtonType.OK, ButtonType.CANCEL);
            alert.setTitle("Normalize ALL Coordinates");
            alert.setHeaderText(null);
            final Optional<ButtonType> response = alert.showAndWait();
            if (response.isPresent() && response.get() == ButtonType.OK) {
                chart.userNormalize();
            }
        });

        final MenuItem cmdSnap = new MenuItem("Snap To Grid Size...");
        cmdSnap.setOnAction(t -> {
            final TextInputDialog dialog = new TextInputDialog();
            dialog.setContentText("Snap to grid current size is " + chart.metrics().grid() + ". Change to:");
            final Optional<String> result = dialog.showAndWait();
            result.ifPresent(s -> chart.metrics().setGrid(s));
        });

        final Menu menuEdit = new Menu("Edit");
        menuEdit.getItems().addAll(cmdNorm, cmdSnap);


        final MenuBar mbar = new MenuBar();
        mbar.useSystemMenuBarProperty().set(true);
        mbar.getMenus().addAll(menuFile, menuEdit);
        return mbar;
    }

    private void exportSkel(final Stage stage, final FamilyChart chart, final boolean exportAll) {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName(".skel.ged");
        final File file = fileChooser.showSaveDialog(stage);
        if (Objects.isNull(file)) {
            return;
        }
        try {
            chart.saveSkeleton(exportAll, file);
        } catch (final IOException e) {
            // TODO: this is not nice
            LOG.error("An error occurred while trying to save file, file={}", file, e);
        }
    }

    public boolean mac() {
        return os().startsWith("mac") || os().startsWith("darwin");
    }

    public static String os() {
        return System.getProperty("os.name", "unknown").toLowerCase();
    }
}
