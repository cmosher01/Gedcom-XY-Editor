package nu.mine.mosher.gedcom.xy.util;


import com.itextpdf.io.font.*;
import com.itextpdf.io.font.constants.*;
import com.itextpdf.kernel.colors.*;
import com.itextpdf.kernel.font.*;
import com.itextpdf.kernel.geom.*;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;
import javafx.geometry.*;
import javafx.scene.shape.Line;
import nu.mine.mosher.gedcom.xy.*;
import org.slf4j.*;

import java.io.*;
import java.util.*;

public class PdfBuilder implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(PdfBuilder.class);

    private static final javafx.scene.paint.Color SOL_PLAQUE_FILL = Solarized.BASE3;
    private static final Color COLOR_PLAQUE_FILL = new DeviceRgb(
        (float)SOL_PLAQUE_FILL.getRed(), (float)SOL_PLAQUE_FILL.getGreen(), (float)SOL_PLAQUE_FILL.getBlue());
    private static final javafx.scene.paint.Color SOL_LINES = Solarized.BASE00;
    private static final Color COLOR_LINES = new DeviceRgb(
        (float)SOL_LINES.getRed(), (float)SOL_LINES.getGreen(), (float)SOL_LINES.getBlue());

    private static final Insets MARGIN = new Insets(100.0d);

    private static PdfFont FONT;
    private static PdfFont FONT_BOLD;
    private static PdfFont FONT_ITALIC;
    static {
        initFonts();
    }
    private static void initFonts()
    {
        try {
            FONT = PdfBuilder.getFontRes("NotoSans-Regular.ttf");
            FONT_BOLD = PdfBuilder.getFontRes("NotoSans-Bold.ttf");
            LOG.info("Successfully loaded NotoSans font resources.");
            return;
        } catch (Exception e) {
            LOG.error("Error loading NotoSans font resource.", e);
            // continue
        }
        try {
            FONT = PdfFontFactory.createFont("/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf");
            FONT_BOLD = PdfFontFactory.createFont("/usr/share/fonts/truetype/noto/NotoSans-Bold.ttf");
//            FONT_ITALIC = PdfFontFactory.createFont("/usr/share/fonts/truetype/noto/NotoSans-Italic.ttf");
            return;
        } catch (Exception e) {
            LOG.error("Error loading NotoSans fonts.", e);
            // continue
        }
        try {
            FONT = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            FONT_BOLD = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
//            FONT_ITALIC = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);
            return;
        } catch (Exception e) {
            LOG.error("Error loading PDF-standard Helvetica fonts.", e);
            // continue
        }
        try {
            /* FONT_ITALIC = */ FONT_BOLD = FONT = PdfFontFactory.createFont();
            return;
        } catch (Exception e) {
            LOG.error("Error loading PDF-standard default font.", e);
            throw new RuntimeException(e);
        }
    }



    private final Metrics metrics;
    private final Point poffset;
    private final PageSize psize;
    private final PdfDocument pdfdoc;
    private final PdfCanvas canvas;



    public PdfBuilder(Metrics metrics, File fileToSaveAs, Bounds bounds) throws IOException {
        this.metrics = metrics;

        final var page = new BoundingBox(
            bounds.getMinX() - MARGIN.getLeft(),
            bounds.getMinY() - MARGIN.getTop(),
            bounds.getWidth() + MARGIN.getLeft() + MARGIN.getRight(),
            bounds.getHeight() + MARGIN.getTop() + MARGIN.getBottom());
        this.poffset = new Point(page.getMinX(), page.getMinY());
        this.psize = new PageSize((float)page.getWidth(), (float)page.getHeight());

        final var writer = new PdfWriter(fileToSaveAs);
        this.pdfdoc = new PdfDocument(writer);
        final var pdfPage = this.pdfdoc.addNewPage(psize);
        this.canvas = new PdfCanvas(pdfPage);
    }

    public void close() {
        this.pdfdoc.close();
    }



    public void addLine(final Line line) {
        if (Objects.isNull(line)) {
            return;
        }

        this.canvas
            .saveState()
            .setStrokeColor(COLOR_LINES)
            .setLineWidth(0.5f)
            .moveTo(x(line.getStartX()), y(line.getStartY()))
            .lineTo(x(line.getEndX()), y(line.getEndY()))
            .stroke()
            .restoreState()
        ;
    }

    private double x(double x) {
        return x-this.poffset.getX();
    }

    private double y(double y) {
        return this.psize.getHeight()-(y-this.poffset.getY());
    }

    public void addPhantom(final Bounds bounds) {
        drawRect(bounds);
        drawText(bounds, new Text("\u00A0?\u00A0"));
    }

    public void addPerson(final Bounds bounds, String nameGiven, String nameSur, String dates, String tagLine, final String refn) {
        drawRect(bounds);

        Text tG, tS;
        if (nameGiven.isBlank() && nameSur.isBlank()) {
            tG = new Text("?").setFont(FONT_BOLD);
            tS = new Text("\n").setFont(FONT);
        } else {
            if (nameGiven.isBlank()) {
                nameGiven = "?";
            }
            if (nameSur.isBlank()) {
                nameSur = "?";
            }
            tG = new Text(nameGiven).setFont(FONT_BOLD);
            tS = new Text("\u00A0" + nameSur+"\n").setFont(FONT);
        }

        if (!dates.isBlank()) {
            dates += "\n";
        }
        var tD = new Text(dates).setFont(FONT);

        if (!tagLine.isBlank()) {
            tagLine += "\n";
        }
        var tT = new Text(tagLine).setFont(FONT);

        drawText(bounds, tG, tS, tD, tT);
    }

    private void drawRect(Bounds bounds) {
        this.canvas
            .saveState()
            .setStrokeColor(COLOR_LINES)
            .setLineWidth(0.5f)
            .setFillColor(COLOR_PLAQUE_FILL)
            .setExtGState(new PdfExtGState().setFillOpacity(0.7f))
            .roundRectangle((float)x(bounds.getMinX()), (float)y(bounds.getMaxY()), (float)bounds.getWidth(), (float)bounds.getHeight(), 3.0f)
            .fillStroke()
            .restoreState()
        ;
    }

    private void drawText(final Bounds bounds, final Text... rt) {
        final var DY = (float)this.metrics.getFontSize();

        final var p = new Paragraph()
            .setTextAlignment(TextAlignment.CENTER)
            .setFontKerning(FontKerning.YES)
            .setFontSize(DY);

        Arrays.stream(rt).forEach(p::add);

        var rect = new Rectangle(
            (float)x(bounds.getMinX()),
            (float)y(bounds.getMaxY())-DY,
            (float)bounds.getWidth(),
            (float)bounds.getHeight()+DY);

        try (final var ch = new Canvas(this.canvas, rect)) {
            ch.add(p);
        }
    }



    private static PdfFont getFontRes(final String fileName) throws IOException {
        final byte[] f = getRes(fileName);
        final FontProgram fp = FontProgramFactory.createFont(f);
        return PdfFontFactory.createFont(fp);
    }

    private static byte[] getRes(final String fileName) throws IOException {
        try (final InputStream is = PdfBuilder.class.getResourceAsStream(fileName)) {
            return Objects.requireNonNull(is).readAllBytes();
        }
    }
}
