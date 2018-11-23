package nu.mine.mosher.gedcom.xy;

import javafx.application.*;
import javafx.beans.binding.Bindings;
import javafx.event.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.stage.*;
import nu.mine.mosher.gedcom.*;
import nu.mine.mosher.gedcom.xy.util.ZoomPane;

import java.io.*;
import java.nio.file.Files;
import java.util.Objects;

public final class GenXyEditor extends Application {
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

    private static class GrIndi {
        private final Indi indi;
        public GrIndi(Indi indi) {
            this.indi = indi;
        }
    }

    private Parent buildGui(final Stage stage, final FamilyChart chart) {
        final Pane canvas = new Pane();
        canvas.setStyle("-fx-border-color: firebrick");
        canvas.setMaxSize(5000.0D, 5000.0D);

        chart.setOnDrag(drag2());
        chart.setFromOrig();
        chart.addGraphicsTo(canvas.getChildren());

        final ZoomPane workspace = new ZoomPane(canvas);
        workspace.setStyle("-fx-background-color: white;");

        final BorderPane root = new BorderPane();
        root.setTop(buildMenuBar(stage));
        root.setCenter(workspace);

        return root;
    }

    private EventHandler<MouseEvent> drag2() {
        return t -> {
            t.consume();
            Node n = (Node)t.getSource();
            n.setLayoutX(n.getLayoutX() + t.getX());
            n.setLayoutY(n.getLayoutY() + t.getY());
        };
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
