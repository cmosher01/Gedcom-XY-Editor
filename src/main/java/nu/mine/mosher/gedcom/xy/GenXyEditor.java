package nu.mine.mosher.gedcom.xy;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Optional;

public final class GenXyEditor extends Application {
    static {
        Platform.setImplicitExit(false);
    }

    private Optional<Rectangle> selector = Optional.empty();
    private Point2D ptSelStart;

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
                Platform.exit();
                return;
            }
            final GedcomTree tree;
            try {
                tree = Gedcom.readFile(new BufferedInputStream(Files.newInputStream(fileToOpen.toPath())));
                final FamilyChart chart = FamilyChartBuilder.create(tree);
                chart.setFromOrig();

                stage.setOnCloseRequest(t -> {
                    if (!exitIfSafe(chart)) {
                        t.consume();
                    }
                });

                stage.setScene(new Scene(buildGui(stage, chart), 640, 480));
                stage.show();
            } catch (final Exception e) {
                Platform.exit();
                throw new IllegalStateException(e);
            }
        });
    }

    private boolean exitIfSafe(final FamilyChart chart) {
        boolean safe = false;
        if (chart.dirty()) {
            final Alert alert = new Alert(Alert.AlertType.WARNING, "Your UNSAVED changes will be DISCARDED.", ButtonType.OK, ButtonType.CANCEL);
            alert.setTitle("Changes will be discarded");
            final Optional<ButtonType> response = alert.showAndWait();
            if (response.isPresent() && response.get() == ButtonType.OK) {
                safe = true;
            }
        } else {
            safe = true;
        }
        if (safe) {
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
            if (!workspace.consumeScroll()) {
                chart.clearSelection();
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
                workspace.consumeScroll();
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
            try {
                chart.saveAs(file);
            } catch (final IOException e) {
                // TODO: this is not nice
                e.printStackTrace();
            }
        });

        final MenuItem cmdExport = new MenuItem("Export Skeleton As...");
        cmdExport.setMnemonicParsing(true);
        cmdExport.setAccelerator(new KeyCodeCombination(KeyCode.K, KeyCombination.SHORTCUT_DOWN));
        cmdExport.setOnAction(t -> {
            final FileChooser fileChooser = new FileChooser();
            fileChooser.setInitialFileName(".skel.ged");
            final File file = fileChooser.showSaveDialog(stage);
            try {
                chart.saveSkeleton(file);
            } catch (final IOException e) {
                // TODO: this is not nice
                e.printStackTrace();
            }
        });

        final MenuItem cmdQuit = new MenuItem("Quit");
        cmdQuit.setMnemonicParsing(true);
        cmdQuit.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN));
        cmdQuit.setOnAction(t -> exitIfSafe(chart));

        final Menu menuFile = new Menu("File");
        menuFile.getItems().addAll(cmdExport, new SeparatorMenuItem(), cmdSaveAs, new SeparatorMenuItem(), cmdQuit);

        final MenuBar mbar = new MenuBar();
        mbar.setUseSystemMenuBar(false);
        mbar.getMenus().add(menuFile);
        return mbar;
    }
}
