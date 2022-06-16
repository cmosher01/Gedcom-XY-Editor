package nu.mine.mosher.gedcom.xy;



import javafx.application.Platform;
import nu.mine.mosher.gedcom.*;
import nu.mine.mosher.gedcom.exception.InvalidLevel;
import nu.mine.mosher.gedcom.xy.util.Version;
import org.slf4j.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.*;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandHandler {
    private static final Logger LOG = LoggerFactory.getLogger(CommandHandler.class);

    private final Frame frame;

    public CommandHandler(final Frame frame) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Not running on event dispatch thread.");
        }
        this.frame = frame;
    }



    public MenuBar buildMenuBar(final FamilyChart chart) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Not running on event dispatch thread.");
        }

        final Menu menuFile = new Menu("File");
        if (chart.isGedcomFile()) {
            final MenuItem cmdSaveAs = new MenuItem("Save As...");
            cmdSaveAs.setShortcut(new MenuShortcut(KeyEvent.VK_S));
            cmdSaveAs.addActionListener(e -> saveAs(chart));

            final MenuItem cmdExportDirty = new MenuItem("Export Changed As Skeletons...");
            cmdExportDirty.setShortcut(new MenuShortcut(KeyEvent.VK_K));
            cmdExportDirty.addActionListener(e -> exportSkel(chart, false));

            final MenuItem cmdExportAll = new MenuItem("Export ALL As Skeletons...");
            cmdExportAll.addActionListener(e -> exportSkel(chart, true));

            menuFile.add(cmdExportDirty);
            menuFile.add(cmdExportAll);
            menuFile.add(cmdSaveAs);
        } else {
            final MenuItem cmdSave = new MenuItem("Save");
            cmdSave.setShortcut(new MenuShortcut(KeyEvent.VK_S));
            cmdSave.addActionListener(e -> chart.save());

            menuFile.add(cmdSave);
        }

        final MenuItem cmdPdf = new MenuItem("Export as PDF");
        cmdPdf.addActionListener(e -> exportPdf(chart));
        menuFile.add(cmdPdf);

        final MenuItem cmdSvg = new MenuItem("Export as SVG");
        cmdSvg.addActionListener(e -> exportSvg(chart));
        menuFile.add(cmdSvg);

        menuFile.addSeparator();

        final MenuItem cmdQuit = new MenuItem("Exit");
        cmdQuit.addActionListener(e -> quitIfSafe(chart));
        menuFile.add(cmdQuit);



        final Menu menuEdit = new Menu("Edit");

        final MenuItem cmdNorm = new MenuItem("Normalize ALL Coordinates");
        cmdNorm.addActionListener(e -> normalize(chart));

        final MenuItem cmdSnap = new MenuItem("Snap To Grid Size...");
        cmdSnap.addActionListener(e -> snapToGrid(chart));

        menuEdit.add(cmdNorm);
        menuEdit.add(cmdSnap);




        final Menu menuHelp = new Menu("Help");

//        final MenuItem cmdHelp = new MenuItem("Help with GenXYEditor");
//        cmdHelp.setShortcut(new MenuShortcut(KeyEvent.VK_HELP));

        final MenuItem cmdAbout = new MenuItem("About GenXYEditor");
        cmdAbout.addActionListener(e -> showAboutBox());

//        menuHelp.add(cmdHelp);
//        menuHelp.addSeparator();
        menuHelp.add(cmdAbout);



        final MenuBar mbar = new MenuBar();
        mbar.add(menuFile);
        mbar.add(menuEdit);
        mbar.setHelpMenu(menuHelp);
        return mbar;
    }


    private void normalize(final FamilyChart chart) {
        final int response = JOptionPane.showConfirmDialog(
            frame,
            "This will normalize the coordinates of ALL people.",
            "Normalize ALL coordinates",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE);
        if (response == JOptionPane.OK_OPTION) {
            chart.userNormalize();
        }
    }

    private void snapToGrid(final FamilyChart chart) {
        final Optional<String> result = Optional.ofNullable(JOptionPane.showInputDialog(
            frame,
            "Snap to grid current size is " + chart.metrics().grid() + ". Change to:",
            chart.metrics().grid()));
        result.ifPresent(s -> chart.metrics().setGrid(s));
    }

    private void saveAs(final FamilyChart chart) {
        final FileDialog fd = new FileDialog(frame, "Genealogy XY Editor - Save as new genealogy file", FileDialog.SAVE);
        fd.setDirectory(GenXyEditor.outDir().getPath());
        if (chart.originalFile().isPresent()) {
            fd.setFile(chart.originalFile().get().getName());
        }
        fd.setVisible(true);

        final String d = fd.getDirectory();
        final String f = fd.getFile();
        fd.dispose();

        if (Objects.isNull(f) || Objects.isNull(d)) {
            return;
        }
        final File fileToSaveAs = new File(d,f);
        GenXyEditor.outDir(fileToSaveAs.getParentFile());
        try {
            chart.saveAs(fileToSaveAs);
        } catch (final IOException x) {
            // TODO: this is not nice
            LOG.error("An error occurred while trying to save file, file={}", fileToSaveAs, x);
        }
    }


    public boolean quitIfSafe(final FamilyChart chart) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Not running on event dispatch thread.");
        }
        boolean safe = isSafeToExit(chart);
        if (safe) {
            quitApp();
        }
        return safe;
    }

    public boolean isSafeToExit(final FamilyChart chart) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Not running on event dispatch thread.");
        }
        boolean safe = false;
        if (chart.dirty()) {
            final int response = JOptionPane.showConfirmDialog(
                frame,
                "Your unsaved changes will be DISCARDED.",
                "DISCARD CHANGES",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
            if (response == JOptionPane.OK_OPTION) {
                LOG.warn("User confirmed discarding changes:");
                chart.indis().stream().filter(Indi::dirty).forEach(Indi::logDiscard);
                safe = true;
            }
        } else {
            safe = true;
        }
        if (safe) {
            LOG.info("Stopping program due to user request.");
        }
        return safe;
    }

    public void quitApp() {
        SwingUtilities.invokeLater(frame::dispose);
    }


    private void exportPdf(final FamilyChart chart) {
        final FileDialog fd = new FileDialog(frame, "Genealogy XY Editor - Export PDF file", FileDialog.SAVE);
        fd.setDirectory(GenXyEditor.outDir().getPath());
        if (chart.originalFile().isPresent()) {
            fd.setFile(pdfNameOf(chart.originalFile().get().getName()));
        } else {
            fd.setFile(".pdf");
        }

        fd.setVisible(true);

        final String d = fd.getDirectory();
        final String f = fd.getFile();
        fd.dispose();

        if (Objects.isNull(f) || Objects.isNull(d)) {
            return;
        }
        final File fileToSaveAs = new File(d,f);
        GenXyEditor.outDir(fileToSaveAs.getParentFile());
        try {
            chart.savePdf(fileToSaveAs);
        } catch (final Exception e) {
            LOG.error("An error occurred while trying to save file, file={}", fileToSaveAs, e);
        }
    }

    private void exportSvg(final FamilyChart chart) {
        final FileDialog fd = new FileDialog(frame, "Genealogy XY Editor - Export SVG file", FileDialog.SAVE);
        fd.setDirectory(GenXyEditor.outDir().getPath());
        if (chart.originalFile().isPresent()) {
            fd.setFile(svgNameOf(chart.originalFile().get().getName()));
        } else {
            fd.setFile(".svg");
        }

        fd.setVisible(true);

        final String d = fd.getDirectory();
        final String f = fd.getFile();
        fd.dispose();

        if (Objects.isNull(f) || Objects.isNull(d)) {
            return;
        }
        final File fileToSaveAs = new File(d,f);
        GenXyEditor.outDir(fileToSaveAs.getParentFile());
        try {
            chart.saveSvg(fileToSaveAs);
        } catch (final Exception e) {
            LOG.error("An error occurred while trying to save file, file={}", fileToSaveAs, e);
        }
    }

    private void exportSkel(final FamilyChart chart, final boolean exportAll) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Not running on event dispatch thread.");
        }
        final FileDialog fd = new FileDialog(frame, "Genealogy XY Editor - Export skeleton genealogy file", FileDialog.SAVE);
        fd.setDirectory(GenXyEditor.outDir().getPath());
        if (chart.originalFile().isPresent()) {
            fd.setFile(skelNameOf(chart.originalFile().get().getName()));
        } else {
            fd.setFile(".skel.ged");
        }
        fd.setVisible(true);

        final String d = fd.getDirectory();
        final String f = fd.getFile();
        fd.dispose();

        if (Objects.isNull(f) || Objects.isNull(d)) {
            return;
        }
        final File fileToSaveAs = new File(d,f);
        GenXyEditor.outDir(fileToSaveAs.getParentFile());
        try {
            chart.saveSkeleton(exportAll, fileToSaveAs);
        } catch (final IOException e) {
            // TODO: this is not nice
            LOG.error("An error occurred while trying to save file, file={}", fileToSaveAs, e);
        }
    }

    private static String skelNameOf(final String name) {
        return name.replaceFirst(".ged$", ".skel.ged");
    }

    private static String pdfNameOf(final String name) {
        return name.replaceFirst(".ged$", ".pdf").replaceFirst(".ftm$", ".pdf");
    }

    private static String svgNameOf(final String name) {
        return name.replaceFirst(".ged$", ".svg").replaceFirst(".ftm$", ".svg");
    }

    public Optional<FamilyChart> openFile() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Not running on event dispatch thread.");
        }
        final Optional<File> fileToOpen = chooseFileToOpen();
        if (fileToOpen.isEmpty()) {
            return Optional.empty();
        }

        return readChartFromFile(fileToOpen.get());
    }

    private Optional<FamilyChart> readChartFromFile(final File fileToOpen) {
        final FamilyChart[] chart = new FamilyChart[1];
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                chart[0] = tryReadChartFromFile(fileToOpen);
            } catch (final Throwable e) {
                LOG.error("unexpected error while reading from file", e);
                // TODO better error handling
            } finally {
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return Optional.ofNullable(chart[0]);
    }

    private static FamilyChart tryReadChartFromFile(final File fileToOpen) throws IOException, InvalidLevel, SQLException {
        final FamilyChart chart;

        final String filetype = filetypeOf(fileToOpen);
        if (filetype.equalsIgnoreCase("GED")) {
            final GedcomTree tree = Gedcom.readFile(new BufferedInputStream(Files.newInputStream(fileToOpen.toPath())));
            chart = FamilyChartBuilderGed.create(tree, fileToOpen);
        } else {
            chart = FamilyChartBuilderFtm.create(fileToOpen);
        }

        chart.setFromOrig();

        return chart;
    }

    private Optional<File> chooseFileToOpen() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Not running on event dispatch thread.");
        }

        final FileDialog fd = new FileDialog(frame, "Genealogy XY Editor - Open genealogy file", FileDialog.LOAD);
        fd.setDirectory(GenXyEditor.inDir().getPath());
        fd.setVisible(true);

        final String d = fd.getDirectory();
        final String f = fd.getFile();
        fd.dispose();

        if (Objects.isNull(f) || Objects.isNull(d)) {
            LOG.warn("User cancelled opening file.");
            return Optional.empty();
        }
        final File fileToOpen = new File(d,f);
        GenXyEditor.inDir(fileToOpen.getParentFile());
        return Optional.of(fileToOpen);
    }

    public void setAboutHandler() {
        if (Desktop.isDesktopSupported()) {
            final Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.APP_ABOUT)) {
                desktop.setAboutHandler(e -> showAboutBox());
            }
        }
    }

    public void setQuitHandler(final FamilyChart chart) {
        if (Desktop.isDesktopSupported()) {
            final Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
                desktop.setQuitHandler((e, r) -> {
                    r.cancelQuit();
                    quitIfSafe(chart);
                });
            }
        }
    }

    private void showAboutBox() {
        SwingUtilities.invokeLater(() ->
            JOptionPane.showMessageDialog(
                frame,
                "Genealogy XY Editor\n\n" +
                    "Version " + Version.version(GenXyEditor.class.getPackage()) + "\n" +
                    "Log file: "+ GenXyEditor.LogConfig.getFilePath() + "\n\n" +
                    "Copyright © 2000–2020, Christopher Alan Mosher, Shelton, Connecticut, USA, <cmosher01@gmail.com>.",
                "Genealogy XY Editor",
                JOptionPane.INFORMATION_MESSAGE));
    }



    private static final Pattern patFiletype = Pattern.compile("^.*\\.(.*)$");
    private static String filetypeOf(final File file) {
        final Matcher matcher = patFiletype.matcher(file.getName());
        if (!matcher.matches()) {
            return "";
        }
        final String ft = matcher.group(1);
        return Objects.isNull(ft) ? "" : ft;
    }
}
