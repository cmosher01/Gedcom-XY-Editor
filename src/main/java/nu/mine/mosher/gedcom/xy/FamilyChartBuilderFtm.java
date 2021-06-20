package nu.mine.mosher.gedcom.xy;

import javafx.geometry.Point2D;
import org.slf4j.*;
import org.sqlite.SQLiteConfig;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

public class FamilyChartBuilderFtm {
    private static final Logger LOG = LoggerFactory.getLogger(FamilyChartBuilderFtm.class);

    public static FamilyChart create(final File fileFtm) throws IOException, SQLException {
        final List<Indi> indis;
        final List<Fami> famis;
        LOG.info("Opening SQLite FTM database file, read-only: {}", fileFtm.getCanonicalPath());
        final SQLiteConfig config = new SQLiteConfig();
        config.setReadOnly(true);
        try (final Connection conn = config.createConnection("jdbc:sqlite:"+ fileFtm.getCanonicalPath())) {

            final Map<String, Indi> mapIdToIndi = new HashMap<>();
            indis = buildIndis(conn, mapIdToIndi);
            famis = buildFamis(conn, Collections.unmodifiableMap(mapIdToIndi));
        }

        if (indis.stream().noneMatch(Indi::hadOriginalXY)) {
            LOG.info("No _XY coordinates found; laying out dropline chart automatically...");
            new Layout(indis, famis).cleanAll();
        }

        normalize(indis);

        final Metrics metrics = Metrics.buildMetricsFor(indis, famis);
        famis.forEach(f -> f.setMetrics(metrics));
        indis.forEach(i -> i.setMetrics(metrics));

        return new FamilyChart(null, indis, famis, metrics, fileFtm);
    }

    private static List<Indi> buildIndis(final Connection conn, final Map<String, Indi> mapIdToIndi) throws SQLException, IOException {
        final List<Indi> indis = new ArrayList<>();
        try (final PreparedStatement select = conn.prepareStatement(sqlIndi())) {
            try (final ResultSet rs = select.executeQuery()) {
                while (rs.next()) {
                    final Indi indi = buildIndi(rs);
                    mapIdToIndi.put(indi.getId(), indi);
                    indis.add(indi);
                }
            }
        }
        LOG.info("Calculated {} individuals.", indis.size());
        return indis;
    }

    private static Indi buildIndi(final ResultSet rs) throws SQLException {
        final String pkidPerson = rs.getString("pkidPerson");
        final String pkidFact = rs.getString("pkidFact");
        final String xy = rs.getString("xy");
        final Optional<Point2D> wxyOrig = Coords.toCoord(xy);
        final String name = rs.getString("name");
        final int sex = rs.getInt("sex");
        final String lifespan = getLifespan(rs.getString("birth"), rs.getString("death"));
        final long birth = calcBirthForSort(rs.getString("birth"));
        final Place birthplace = Place.fromFtmPlace(rs.getString("birthplace"));
        final Place anyplace = Place.fromFtmPlace(rs.getString("anyplace"));
        final Place tagline = birthplace.toString().isBlank() ? anyplace : birthplace;
        LOG.debug("read _XY fact from FTM file: {}, {}, {}", pkidPerson, xy, name);

        return new Indi(null, wxyOrig, pkidPerson, pkidFact, name, lifespan, birth, tagline.toString(), sex);
    }

    private static long calcBirthForSort(String birth) {
        final FtmDate d = FtmDate.fromFtmFactDate(birth);
        return d.ym();
    }

    private static String getLifespan(final String dateBirth, final String dateDeath) {
        final FtmDate db = FtmDate.fromFtmFactDate(dateBirth);
        final FtmDate dd = FtmDate.fromFtmFactDate(dateDeath);
        if (db.unknown() && dd.unknown()) {
            return "";
        }
        return db+"\u2013"+dd;
    }

    private static List<Fami> buildFamis(final Connection conn, final Map<String, Indi> mapIdToIndi) throws SQLException {
        final List<Fami> famis = new ArrayList<>();
        try (final PreparedStatement select = conn.prepareStatement(sqlFami())) {
            try (final ResultSet rs = select.executeQuery()) {
                int prev = -1;
                Fami fami = null;
                while (rs.next()) {
                    final int curr = rs.getInt("ID");
                    if (curr != prev) {
                        if (Objects.nonNull(fami)) {
                            famis.add(fami);
                        }
                        fami = new Fami();
                        fami.setHusb(mapIdToIndi.get(rs.getString("Person1ID")));
                        fami.setWife(mapIdToIndi.get(rs.getString("Person2ID")));
                        prev = curr;
                    }
                    fami.addChild(mapIdToIndi.get(rs.getString("PersonID")));
                }
                if (Objects.nonNull(fami)) {
                    famis.add(fami);
                }
            }
        }
        LOG.info("Calculated {} families.", famis.size());
        for (final Fami fami : famis) {
            StringBuilder sb = new StringBuilder(64);
            sb.append("p1=");
            final Optional<Indi> h = fami.getHusb();
            sb.append(h.isPresent() ? h.get().getId() : "[null]");
            final Optional<Indi> w = fami.getWife();
            sb.append(", p2=");
            sb.append(w.isPresent() ? w.get().getId() : "[null]");

            final List<Indi> rc = fami.getChildren();
            for (final Indi c : rc) {
                sb.append(", c=");
                sb.append(c.getId());
            }
            LOG.debug("family: {}", sb);
        }
        return famis;
    }

    private static void normalize(final List<Indi> indis) {
        final double x = indis.stream().map(Indi::laidOut).filter(Optional::isPresent).map(Optional::get).mapToDouble(Point2D::getX).min().orElse(0D);
        final double y = indis.stream().map(Indi::laidOut).filter(Optional::isPresent).map(Optional::get).mapToDouble(Point2D::getY).min().orElse(0D);
        final Point2D coordsTopLeftAfterLayout = new Point2D(x, y);
        indis.forEach(i -> i.fillMissing(coordsTopLeftAfterLayout));
    }

    private static String getRes(final String fileName) throws IOException {
        try (final InputStream is = FamilyChartBuilderFtm.class.getResourceAsStream(fileName)) {
            return new String(is.readAllBytes(), StandardCharsets.US_ASCII);
        }
    }

    private static String sqlIndi() throws IOException {
        return getRes("Indi.sql");
    }

    private static String sqlFami() {
        return
        "SELECT R.ID, R.Person1ID, R.Person2ID, C.PersonID "+
        "FROM Relationship AS R LEFT OUTER JOIN ChildRelationship AS C ON (C.RelationshipID = R.ID) "+
        "ORDER BY R.ID";
    }
}
