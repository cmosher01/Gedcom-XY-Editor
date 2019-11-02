package nu.mine.mosher.gedcom.xy;

import javafx.application.*;
import javafx.beans.property.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.*;
import nu.mine.mosher.gedcom.*;
import nu.mine.mosher.gedcom.xy.util.ZoomPane;
import nu.mine.mosher.util.AppDirs;
import org.slf4j.*;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.prefs.Preferences;

public final class GenXyEditor extends Application {
    public static void main(final String... args) {
        try {
            initLogging();
            launch(args);
        } catch (final Throwable e) {
            logProgramTermination(e);
        }
    }

    public static Preferences prefs() {
        return Preferences.userNodeForPackage(GenXyEditor.class);
    }

    private static File inDir() {
        return new File(prefs().get("inDir", "./"));
    }

    private static void inDir(final File dir) {
        prefs().put("inDir", dir.getAbsolutePath());
    }

    private static File outDir() {
        return new File(prefs().get("outDir", "./"));
    }

    private static void outDir(final File dir) {
        prefs().put("outDir", dir.getAbsolutePath());
    }

    @Override
    public void start(final Stage stage) {
        stage.setTitle("GEDCOM _XY Editor");

        Platform.runLater(() -> {
            final FileChooser fileChooser = new FileChooser();
            fileChooser.setInitialDirectory(inDir());
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("GEDCOM files", "*.ged"),
                new FileChooser.ExtensionFilter("all files", "*.*"));
            final File fileToOpen = fileChooser.showOpenDialog(null);
            if (Objects.isNull(fileToOpen)) {
                LOG.warn("User cancelled opening file. Program will exit.");
                Platform.exit();
                return;
            }
            inDir(fileToOpen.getParentFile());
            try {
                final GedcomTree tree = Gedcom.readFile(new BufferedInputStream(Files.newInputStream(fileToOpen.toPath())));
                final FamilyChart chart = FamilyChartBuilder.create(tree, fileToOpen);
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



    private static Logger LOG;

    private static void initLogging() throws FileNotFoundException {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.FINEST);

        final AppDirs DIRS = AppDirs.of(GenXyEditor.class);
        final PrintStream log = new PrintStream(new FileOutputStream(DIRS.logFile(), true), true);
        System.setErr(log);
        System.setOut(log);

        LOG = LoggerFactory.getLogger(GenXyEditor.class);
        LOG.info("Program starting.");
    }

    private static void logProgramTermination(final Throwable e) {
        if (Objects.nonNull(LOG)) {
            LOG.error("Program terminating due to error:", e);
        } else {
            try {
                final Path pathTemp = Files.createTempFile("GEDCOM-XY-EDITOR-", ".tmp");
                System.setErr(new PrintStream(new FileOutputStream(pathTemp.toFile()), true));
                e.printStackTrace();
            } catch (final Throwable reallyBad) {
                reallyBad.printStackTrace();
            }
        }
    }

    private static boolean exitIfSafe(final Stage stage, final FamilyChart chart) {
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
            LOG.info("Stopping program due to user request.");
            stage.setOnCloseRequest(null);
            stage.close();
            Platform.exit();
        }
        return safe;
    }

    private static Parent buildGui(final Stage stage, final FamilyChart chart) {
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

    private static MenuBar buildMenuBar(final Stage stage, final FamilyChart chart) {
        final MenuItem cmdSaveAs = new MenuItem("Save As...");
        cmdSaveAs.setMnemonicParsing(true);
        cmdSaveAs.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN));
        cmdSaveAs.setOnAction(t -> {
            final FileChooser fileChooser = new FileChooser();
            fileChooser.setInitialDirectory(outDir());
            if (chart.originalFile().isPresent()) {
                fileChooser.setInitialFileName(chart.originalFile().get().getName());
            }
            final File file = fileChooser.showSaveDialog(stage);
            if (Objects.isNull(file)) {
                return;
            }
            outDir(file.getParentFile());
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

    private static void exportSkel(final Stage stage, final FamilyChart chart, final boolean exportAll) {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(outDir());
        if (chart.originalFile().isPresent()) {
            fileChooser.setInitialFileName(skelNameOf(chart.originalFile().get().getName()));
        } else {
            fileChooser.setInitialFileName(".skel.ged");
        }
        final File file = fileChooser.showSaveDialog(stage);
        if (Objects.isNull(file)) {
            return;
        }
        outDir(file.getParentFile());
        try {
            chart.saveSkeleton(exportAll, file);
        } catch (final IOException e) {
            // TODO: this is not nice
            LOG.error("An error occurred while trying to save file, file={}", file, e);
        }
    }

    private static String skelNameOf(String name) {
        return name.replaceFirst(".ged$", ".skel.ged");
    }

    private static boolean mac() {
        return os().startsWith("mac") || os().startsWith("darwin");
    }

    private static String os() {
        return System.getProperty("os.name", "unknown").toLowerCase();
    }
}
