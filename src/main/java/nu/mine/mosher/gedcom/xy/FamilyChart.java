package nu.mine.mosher.gedcom.xy;

import javafx.beans.property.*;
import javafx.geometry.*;
import javafx.scene.Node;
import nu.mine.mosher.collection.TreeNode;
import nu.mine.mosher.gedcom.*;
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

public class FamilyChart {
    private static final Logger LOG = LoggerFactory.getLogger(FamilyChart.class);

    private final Optional<File> fileOriginal;
    private final Optional<GedcomTree> tree;
    private final List<Indi> indis;
    private final List<Fami> famis;
    private final Metrics metrics;
    private final Selection selection = new Selection();
    private final StringProperty selectedNameProperty = new SimpleStringProperty();

    public FamilyChart(final GedcomTree tree, final List<Indi> indis, final List<Fami> famis, final Metrics metrics, final File fileOriginal) {
        this.fileOriginal = Optional.ofNullable(fileOriginal);
        this.tree = Optional.ofNullable(tree);
        this.indis = List.copyOf(indis);
        this.famis = List.copyOf(famis);
        this.metrics = metrics;
    }

    public void addGraphicsTo(final List<Node> addto) {
        this.famis.forEach(f -> f.addGraphicsTo(addto));
        this.indis.forEach(i -> i.addGraphicsTo(addto));
    }

    public void setFromOrig() {
        this.indis.forEach(i -> i.setSelection(this.selection));
        this.indis.forEach(Indi::calc);
        this.famis.forEach(Fami::calc);
        this.indis.forEach(Indi::startCoordTracking);
    }

    public void clearSelection() {
        this.selection.clear();
        updateSelectStatus();
    }

    public void setSelectionFrom(double x, double y, double w, double h) {
        this.indis.forEach(i -> this.selection.select(i, i.intersects(x, y, w, h), false));
        updateSelectStatus();
    }

    private void updateSelectStatus() {
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

    public Metrics metrics() {
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
        try (final Connection conn = new SQLiteConfig().createConnection("jdbc:sqlite:"+ this.fileOriginal.get().getCanonicalPath())) {
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
        final SvgBuilder svg = new SvgBuilder(fontsize);

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

    public void userNormalize() {
        final double x = this.indis.stream().map(Indi::coords).mapToDouble(Point2D::getX).min().orElse(0D);
        final double y = this.indis.stream().map(Indi::coords).mapToDouble(Point2D::getY).min().orElse(0D);
        final Point2D coordsTopLeft = new Point2D(x, y);
        this.indis.forEach(i -> i.userNormalize(coordsTopLeft));
        updateSelectStatus();
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

    public Point2D calculateSize() {
        final Bounds bounds = this.indis.stream().map(Indi::bounds).reduce((b1, b2) -> {
            final double xMin = Math.min(b1.getMinX(), b2.getMinX());
            final double xMax = Math.max(b1.getMaxX(), b2.getMaxX());
            final double width = Math.abs(xMax-xMin);
            final double yMin = Math.min(b1.getMinY(), b2.getMinY());
            final double yMax = Math.max(b1.getMaxY(), b2.getMaxY());
            final double height = Math.abs(yMax-yMin);
            return new BoundingBox(xMin, yMin, width, height);
        }).get();
        return new Point2D(bounds.getWidth(), bounds.getHeight());
    }

    public class Selection {
        private final Set<Indi> indis = new HashSet<>();
        private Point2D orig;

        public void clear() {
            this.indis.forEach(i -> i.select(false));
            this.indis.clear();
        }

        public void select(final Indi indi, final boolean select, final boolean updateStatus) {
            indi.select(select);
            if (select) {
                this.indis.add(indi);
            } else {
                this.indis.remove(indi);
            }
            if (updateStatus) {
                updateSelectStatus();
            }
        }

        public void beginDrag(final Point2D orig) {
            this.orig = orig;
            updateSelectStatus();
        }

        public void drag(final Point2D to) {
            this.indis.forEach(i -> i.drag(to.subtract(this.orig)));
            updateSelectStatus();
        }
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
}
