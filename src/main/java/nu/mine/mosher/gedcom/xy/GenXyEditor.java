package nu.mine.mosher.gedcom.xy;

import javafx.application.Application;
import javafx.application.Platform;
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
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
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
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import static org.slf4j.simple.SimpleLogger.DEFAULT_LOG_LEVEL_KEY;

public final class GenXyEditor extends Application {
    private static Logger LOG;
    static {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        Platform.setImplicitExit(false);
    }

    private Optional<Rectangle> selector = Optional.empty();
    private Point2D ptSelStart;

    public static void main(final String... args) {
        if (Arrays.stream(args).anyMatch(c -> c.equals("-q"))) {
            System.setProperty(DEFAULT_LOG_LEVEL_KEY, "off");
        } else if (Arrays.stream(args).anyMatch(c -> c.equals("-v"))) {
            System.setProperty(DEFAULT_LOG_LEVEL_KEY, "debug");
        } else if (Arrays.stream(args).anyMatch(c -> c.equals("-vv"))) {
            System.setProperty(DEFAULT_LOG_LEVEL_KEY, "trace");
        }
        LOG = LoggerFactory.getLogger(GenXyEditor.class);
        Application.launch();
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
            final GedcomTree tree;
            try {
                tree = Gedcom.readFile(new BufferedInputStream(Files.newInputStream(fileToOpen.toPath())));
                final FamilyChart chart = FamilyChartBuilder.create(tree);
                chart.setFromOrig();

                stage.setOnCloseRequest(t -> {
                    if (!exitIfSafe(stage, chart)) {
                        t.consume();
                    }
                });

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
            final Alert alert = new Alert(Alert.AlertType.WARNING, "Your UNSAVED changes will be DISCARDED.", ButtonType.OK, ButtonType.CANCEL);
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

        canvas.addEventFilter(MouseEvent.MOUSE_PRESSED, t -> {
            if (t.isShiftDown()) {
                this.ptSelStart = new Point2D(t.getX(), t.getY());
                final Rectangle sel = new Rectangle(t.getX(), t.getY(), 0D, 0D);
                sel.setFill(Color.TRANSPARENT);
                sel.setStrokeWidth(1.0D);
                sel.setStroke(chart.metrics().colorSelectionChooser());
                sel.getStrokeDashArray().addAll(3.0D);
                canvas.getChildren().add(sel);
                chart.setSelectionFrom(sel.getX(), sel.getY(), sel.getWidth(), sel.getHeight());
                selector = Optional.of(sel);
                t.consume();
            }
        });

        canvas.addEventFilter(MouseEvent.MOUSE_DRAGGED, t -> {
            if (selector.isPresent()) {
                final Rectangle sel = selector.get();

                final double x = this.ptSelStart.getX();
                final double w = t.getX() - x;
                if (w < 0D) {
                    sel.setX(t.getX());
                    sel.setWidth(-w);
                } else {
                    sel.setWidth(w);
                }

                final double y = this.ptSelStart.getY();
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
            if (this.selector.isPresent()) {
                canvas.getChildren().remove(this.selector.get());
                t.consume();
                this.selector = Optional.empty();
            }
        });

        final BorderPane root = new BorderPane();
        root.setTop(buildMenuBar(stage, chart));
        root.setCenter(workspace);

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
            dialog.setContentText("Snap to grid current size is "+chart.metrics().grid()+". Change to:");
            final Optional<String> result = dialog.showAndWait();
            result.ifPresent(s -> chart.metrics().setGrid(s));
        });

        final Menu menuEdit = new Menu("Edit");
        menuEdit.getItems().addAll(cmdNorm, cmdSnap);



        final MenuBar mbar = new MenuBar();
        mbar.setUseSystemMenuBar(false);
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
        return System.getProperty("os.name","unknown").toLowerCase();
    }
}
