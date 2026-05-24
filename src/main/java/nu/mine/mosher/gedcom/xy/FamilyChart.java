/*
 *     Copyright © 2018-2026, Christopher Alan Mosher, New York, New York, USA, <cmosher01@gmail.com>.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package nu.mine.mosher.gedcom.xy;

import javafx.beans.property.*;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.input.KeyEvent;
import nu.mine.mosher.collection.TreeNode;
import nu.mine.mosher.gedcom.*;
import nu.mine.mosher.gedcom.xy.undo.ModificationTracker;
import nu.mine.mosher.gedcom.xy.util.*;
import org.slf4j.*;
import org.sqlite.SQLiteConfig;
import org.w3c.dom.Document;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class FamilyChart {
    private static final Logger LOG = LoggerFactory.getLogger(FamilyChart.class);

    private final Optional<File> fileOriginal;
    private final Optional<GedcomTree> tree;
    private final List<Indi> indis;
    private final List<Fami> famis;
    private final OtherChartGraphics others = new OtherChartGraphics();
    private final Metrics metrics;
    private final Selection selection = new Selection(this);
    private final StringProperty selectedNameProperty = new SimpleStringProperty();
    private Scrollable scrollable;
    private final ModificationTracker modtrack = new ModificationTracker();
    private final Jumper jumper = new Jumper();

    public FamilyChart(final GedcomTree tree, final List<Indi> indis, final List<Fami> famis, final Metrics metrics, final File fileOriginal) {
        this.fileOriginal = Optional.ofNullable(fileOriginal);
        this.tree = Optional.ofNullable(tree);
        this.indis = List.copyOf(indis);
        this.famis = List.copyOf(famis);
        this.metrics = metrics;
    }

    public void setScroller(final Scrollable scroller) {
        this.scrollable = scroller;
    }

    public void addGraphicsTo(final List<Node> addto) {
        this.others.addGraphicsTo(addto);
        this.famis.forEach(f -> f.addGraphicsTo(addto));
        this.indis.forEach(i -> i.addGraphicsTo(addto));
    }

    public void setFromOrig() {
        this.indis.forEach(i -> i.setItemsFromChart(this.selection));
        calc();
        this.indis.forEach(Indi::startCoordTracking);
    }

    public void calc() {
        this.others.calc(this.indis, this.metrics.colors());
        this.indis.forEach(Indi::calc);
        this.famis.forEach(Fami::calc);
    }

    public void clearSelection() {
        this.selection.clear();
        updateSelectStatus();
    }

    public void setSelectionFrom(double x, double y, double w, double h) {
        this.indis.forEach(i -> this.selection.select(i, i.intersects(x, y, w, h), false));
        updateSelectStatus();
    }

    void updateSelectStatus() {
        final long cSel = this.indis.stream().filter(Indi::selected).count();
        if (cSel <= 0) {
            this.selectedNameProperty.setValue("[nothing selected]");
        } else if (1 < cSel) {
            this.selectedNameProperty.setValue(String.format("[%d selected]", cSel));
        } else {
            final Optional<Indi> i = this.indis.stream().filter(Indi::selected).findAny();
            if (i.isPresent()) {
                final Point2D coords = i.get().coords();
                final Optional<Point2D> coordsOriginal = i.get().coordsOriginal();
                final String from;
                if (i.get().dirty()) {
                    if (coordsOriginal.isPresent()) {
                        from = String.format("(%.2f,%.2f) \u2192 ", coordsOriginal.get().getX(), coordsOriginal.get().getY());
                    } else {
                        from = "() \u2192 ";
                    }
                } else {
                    from = "";
                }
                final String tagline = i.get().getTagline();
                this.selectedNameProperty.setValue(String.format("[%s selected] %s(%.2f,%.2f) [%.2fx%.2f] %s", i.get().name(), from, coords.getX(), coords.getY(), i.get().width(), i.get().height(), tagline));
            } else {
                this.selectedNameProperty.setValue("[nothing selected]");
            }
        }
    }

    ModificationTracker modificationTracker() {
        return this.modtrack;
    }

    Scrollable scrollable() {
        return this.scrollable;
    }

    Metrics metrics() {
        return this.metrics;
    }

    public boolean isGedcomFile() {
        return this.tree.isPresent();
    }

    public void save() {
        try {
            trySave();
        } catch (final Throwable e) {
            LOG.error("Error occurred while trying to write _XY Fact to FTM database file", e);
        }
    }

    public void trySave() throws IOException, SQLException {
        if (this.fileOriginal.isEmpty()) {
            LOG.error("can't happen");
            return;
        }

        LOG.info("Opening SQLite FTM database file, for update: {}", this.fileOriginal.get().getCanonicalPath());
        try (final Connection conn = new SQLiteConfig().createConnection("jdbc:sqlite:"+this.fileOriginal.get().getCanonicalPath())) {
            final long pkidFactTypeXy = prepareDatabaseForFactTypeXy(conn);
            for (final Indi indi : this.indis) {
                if (indi.dirty()) {
                    indi.saveXyToFtm(conn, pkidFactTypeXy);
                }
            }
        }
    }

    private static long prepareDatabaseForFactTypeXy(final Connection conn) throws SQLException {
        if (!hasFactTypeXy(conn)) {
            LOG.warn("Database does not have a FactType for _XY; will add one now...");
            createFactTypeXy(conn);
        }
        try (final PreparedStatement select = conn.prepareStatement(
            "SELECT FactType.ID AS pkidFactTypeXY FROM FactType WHERE FactType.Abbreviation = '_XY'")) {
            try (final ResultSet rs = select.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("pkidFactTypeXY");
                }
            }
        }
        throw new SQLException("Could not find or create _XY FactType in FTM database tree file.");
    }

    private static void createFactTypeXy(final Connection conn) throws SQLException {
        long maxID = -1L;
        try (final PreparedStatement select = conn.prepareStatement(
            "SELECT MAX(FactType.ID) AS maxID FROM FactType")) {
            try (final ResultSet rs = select.executeQuery()) {
                if (rs.next()) {
                    maxID = rs.getLong("maxID");
                }
            }
        }
        LOG.debug("Max FactType ID: {}", maxID);

        long seqID = -1L;
        try (final PreparedStatement select = conn.prepareStatement(
            "SELECT seq AS seqID FROM sqlite_sequence WHERE name = 'FactType'")) {
            try (final ResultSet rs = select.executeQuery()) {
                if (rs.next()) {
                    seqID = rs.getLong("seqID");
                }
            }
        }
        LOG.debug("FactType sequence value: {}", seqID);

        // Sanity check: since we are adding a FactType here, make sure the sequence and the primary key
        // are what we expect them to be. Otherwise, bail out.
        if (maxID < 0L || seqID < 0L || maxID != seqID) {
            LOG.error("Unexpected values for FactType primary key/sequence: FactType.ID={}, seq={}", maxID, seqID);
            throw new SQLException("Unexpected values for FactType primary key/sequence; will not update database");
        }

        if (maxID < 1000L) {
            // Special logic here, for case where no custom FactTypes at all exist in the database.
            // Note: FTM rigs custom FactTypes so their IDs are greater than or equal to 1001.
            LOG.warn("No custom FactTypes were found in the database; will update FactType ID sequence to 1000.");
            try (final PreparedStatement update = conn.prepareStatement(
                "UPDATE sqlite_sequence SET seq = 1000 WHERE name = 'FactType'")) {
                update.executeUpdate();
            }
        }

        final var syncVersion = readSyncVersion(conn);
        try (final PreparedStatement insert = conn.prepareStatement(
            "INSERT INTO FactType(Name, ShortName, Abbreviation, FactClass, Tag, SyncVersion) " +
                "VALUES('_XY','_XY','_XY',263,'EVEN',?)")) {
            insert.setLong(1, syncVersion);
            insert.executeUpdate();
        }
    }

    private static boolean hasFactTypeXy(final Connection conn) throws SQLException {
        try (final PreparedStatement select = conn.prepareStatement(
            "SELECT COUNT(*) AS count FROM FactType WHERE FactType.Abbreviation = '_XY'")) {
            try (final ResultSet rs = select.executeQuery()) {
                if (rs.next()) {
                    return 0 < rs.getInt("count");
                }
            }
        }
        return false;
    }

    public static long readSyncVersion(final Connection conn) throws SQLException {
        try (final PreparedStatement select = conn.prepareStatement(
            "SELECT StringValue AS SyncVersion FROM Setting WHERE Name = 'SyncVersion'")) {
            try (final ResultSet rs = select.executeQuery()) {
                if (rs.next()) {
                    return Long.parseLong(rs.getString("SyncVersion"));
                }
            }
        }
        return 2L; // default starting SyncVersion value
    }

    public void saveAs(final File file) throws IOException {
        if (!this.tree.isPresent()) {
            LOG.error("Cannot call \"saveAs\" without a GEDCOM file.");
            return;
        }

        this.indis.stream().filter(Indi::dirty).forEach(Indi::saveXyToTree);
        tree.get().timestamp();
        Gedcom.writeFile(tree.get(), new BufferedOutputStream(new FileOutputStream(file)));
    }

    public void savePdf(final File fileToSaveAs) throws IOException {
        final long fontsize = Math.round(Math.rint(this.metrics.getFontSize()));

        try (final PdfBuilder builder = new PdfBuilder(this.metrics, fileToSaveAs, calculateSize())) {
            this.famis.forEach(i -> i.savePdf(builder));
            this.indis.forEach(i -> i.savePdf(builder));
        }
    }

    public void saveSvg(final File fileToSaveAs) throws ParserConfigurationException, TransformerException {
        final long fontsize = Math.round(Math.rint(this.metrics.getFontSize()));
        final SvgBuilder svg = new SvgBuilder(fontsize, calculateSize());

        this.famis.forEach(i -> i.saveSvg(svg));
        this.indis.forEach(i -> i.saveSvg(svg));

        saveDoc(svg.get(), fileToSaveAs);
    }

    private static void saveDoc(final Document document, final File fileToSaveAs) throws TransformerException {
        final Transformer transformer = TransformerFactory.newInstance().newTransformer();

        transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        transformer.transform(new DOMSource(document), new StreamResult(fileToSaveAs));
    }

    public void saveSkeleton(final boolean exportAll, final File file) throws IOException {
        final PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)));

        out.println("0 HEAD");
        out.println("1 CHAR UTF-8");
        out.println("1 GEDC");
        out.println("2 VERS 5.5.1");
        out.println("2 FORM LINEAGE-LINKED");
        out.println("1 SOUR _XY EDITOR");

        this.indis.stream().filter(i -> exportAll || i.dirty()).forEach(i -> {
            i.saveXyToTree();
            extractSkeleton(i.node(), out);
        });

        out.println("0 TRLR");

        if (out.checkError()) {
            LOG.error("ERROR exporting skeleton file, file={}", file);
        }
        out.close();
    }

    public boolean dirty() {
        return this.indis.stream().anyMatch(Indi::dirty);
    }

//    public void userNormalize() {
//        final double x = this.indis.stream().map(Indi::coords).mapToDouble(Point2D::getX).min().orElse(0D);
//        final double y = this.indis.stream().map(Indi::coords).mapToDouble(Point2D::getY).min().orElse(0D);
//        final Point2D coordsTopLeft = new Point2D(x, y);
//        this.indis.forEach(i -> i.userNormalize(coordsTopLeft));
//        updateSelectStatus();
//    }

    public void userClean() {
        new Layout(this.indis, this.famis).cleanUnplaced();
    }

    public List<Indi> indis() {
        return List.copyOf(this.indis);
    }

    public StringProperty selectedName() {
        return this.selectedNameProperty;
    }

    public Optional<File> originalFile() {
        return this.fileOriginal;
    }

    public Bounds calculateSize() {
        return this.indis.stream().map(Indi::bounds).reduce((b1, b2) -> {
            final double xMin = Math.min(b1.getMinX(), b2.getMinX());
            final double xMax = Math.max(b1.getMaxX(), b2.getMaxX());
            final double width = Math.abs(xMax-xMin);
            final double yMin = Math.min(b1.getMinY(), b2.getMinY());
            final double yMax = Math.max(b1.getMaxY(), b2.getMaxY());
            final double height = Math.abs(yMax-yMin);
            return new BoundingBox(xMin, yMin, width, height);
        }).get();
    }

    public void onKey(final KeyEvent t) {
        final String k = t.getText();

        if (k.startsWith("n")) {
            cmdNudge();
        } else if (k.startsWith("r")) {
            cmdReset();
        } else if (k.startsWith("c")) {
            cmdCenter();
        } else if (k.startsWith("f")) {
            cmdFit();
        } else if (k.startsWith("j")) {
            cmdJump();
        }
    }

    public void cmdNudge() {
        this.selection.nudge();
    }

    public void cmdReset() {
        // reset scale to 1:1
        this.scrollable.scaleTo();
        // TODO scaleTo seems to scroll to an arbitrary point,
        // so (for now) just center the chart:
        this.cmdCenter();
    }

    public void cmdCenter() {
        // scroll to chart center
        final var boundsChart = calculateSize();
        final var ptChartCenter = new Point2D(boundsChart.getCenterX(), boundsChart.getCenterY());
        this.scrollable.scrollTo(ptChartCenter);
    }

    public void cmdFit() {
        // resize chart to window
        final var boundsChart = calculateSize();
        this.scrollable.scaleToFit(boundsChart);
        // center chart
        cmdCenter();
    }

    public void cmdJump() {
        // checks if user changed the selection since last time
        this.jumper.selectionDelta(this.selection.center());

        final var jumpTo = this.jumper.userPressedJ(this.scrollable.center());
        jumpTo.ifPresent(to -> this.scrollable.scrollTo(to));
    }

    public void undo() {
        this.modtrack.undo();
    }

    public void redo() {
        this.modtrack.redo();
    }

    public boolean canUndo() {
        return this.modtrack.canUndo();
    }

    public boolean canRedo() {
        return this.modtrack.canRedo();
    }


    private static final Set<String> SKEL;

    static {
        SKEL = Set.of("NAME", "SEX", "REFN", "RIN", "_XY", "BIRT", "DEAT");
    }

    private static void extractSkeleton(final TreeNode<GedcomLine> indi, final PrintWriter out) {
        out.println(indi);
        for (final TreeNode<GedcomLine> c : indi) {
            if (SKEL.contains(c.getObject().getTagString())) {
                out.println(c);
                for (final TreeNode<GedcomLine> c2 : c) {
                    if (c2.getObject().getTag().equals(GedcomTag.DATE)) {
                        out.println(c2);
                    }
                }
            }
        }
    }


    public static Bounds addBounds(final Bounds b1, final Bounds b2) {
        final var minX = Math.min(b1.getMinX(), b2.getMinX());
        final var minY = Math.min(b1.getMinY(), b2.getMinY());
        final var maxX = Math.max(b1.getMaxX(), b2.getMaxX());
        final var maxY = Math.max(b1.getMaxY(), b2.getMaxY());
        return new BoundingBox(minX, minY, maxX - minX, maxY - minY);
    }
}
