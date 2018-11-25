package nu.mine.mosher.gedcom.xy;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import nu.mine.mosher.gedcom.Gedcom;
import nu.mine.mosher.gedcom.GedcomTree;
import nu.mine.mosher.gedcom.xy.util.ZoomPane;

import java.io.BufferedInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.Objects;

public final class GenXyEditor extends Application {
    private boolean dragged;
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
                return;
            }
            final GedcomTree tree;
            try {
                tree = Gedcom.readFile(new BufferedInputStream(Files.newInputStream(fileToOpen.toPath())));
                final FamilyChart chart = FamilyChartBuilder.create(tree);

                final StringBuilder sb = new StringBuilder(100);
                tree.getRoot().getFirstChildOrNull().appendStringDeep(sb, true);
                System.err.println("loaded gedcom with header:");
                System.err.println(sb.toString());

                stage.setScene(new Scene(buildGui(stage, chart), 640, 480));
                stage.show();
            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    private Parent buildGui(final Stage stage, final FamilyChart chart) {
        final Group canvas = new Group();

        chart.setFromOrig();
        chart.addGraphicsTo(canvas.getChildren());

        final ZoomPane workspace = new ZoomPane(canvas);
        workspace.setOnMouseClicked(t -> {
            if (!workspace.consumeScroll()) {
                chart.clearSelection();
            }
        });

        final BorderPane root = new BorderPane();
        root.setTop(buildMenuBar(stage));
        root.setCenter(workspace);

        return root;
    }

    private static MenuBar buildMenuBar(final Stage stage) {
        final MenuItem cmdSaveAs = new MenuItem("Save as...");
        cmdSaveAs.setOnAction((event) -> {
            final FileChooser fileChooser = new FileChooser();
            fileChooser.showSaveDialog(stage);
        });

        final MenuItem cmdExport = new MenuItem("Export skeleton as...");
        cmdExport.setOnAction((event) -> {
            final FileChooser fileChooser = new FileChooser();
            fileChooser.setInitialFileName(".skel.ged");
            fileChooser.showSaveDialog(stage);
        });

        final MenuItem cmdQuit = new MenuItem("Quit");
        cmdQuit.setMnemonicParsing(true);
        cmdQuit.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN));
        cmdQuit.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                Platform.exit();
            }
        });

        final Menu menuFile = new Menu("File");
        menuFile.getItems().addAll(cmdExport, new SeparatorMenuItem(), cmdSaveAs, new SeparatorMenuItem(), cmdQuit);

        final MenuBar mbar = new MenuBar();
        mbar.setUseSystemMenuBar(false);
        mbar.getMenus().add(menuFile);
        return mbar;
    }
}
