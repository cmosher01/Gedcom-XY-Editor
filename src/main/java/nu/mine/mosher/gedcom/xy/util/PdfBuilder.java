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

package nu.mine.mosher.gedcom.xy.util;


import com.itextpdf.io.font.*;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.*;
import com.itextpdf.kernel.font.*;
import com.itextpdf.kernel.geom.*;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;
import javafx.geometry.*;
import javafx.scene.shape.Line;
import nu.mine.mosher.gedcom.GedcomIndiName;
import nu.mine.mosher.gedcom.xy.Metrics;
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

    private static final Insets MARGIN = new Insets(200.0d);

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

    public void addPerson(final Bounds bounds, GedcomIndiName name, String dates, String tagLine, final String refn) {
        drawRect(bounds);

        final var fsSmall = (float)this.metrics.getFontSizeSmall();

        final var rText = new ArrayList<Text>();
        final var tokens = name.tokenized();
        for (final var token : tokens) {
            switch (token) {
                case NULL -> {}
                case UNKNOWN -> rText.add(new Text("?").setFont(FONT));
                case SPACE -> rText.add(new Text(" ").setFont(FONT));
                case GIVEN0 -> rText.add(new Text(name.given0()).setFont(FONT_BOLD));
                case SUR -> rText.add(new Text(name.sur()).setFont(FONT));
                case GIVEN1 -> rText.add(new Text(name.given1()).setFont(FONT));
            }
        }
        if (!dates.isBlank()) {
            rText.add(new Text("\n"+dates).setFont(FONT).setFontSize(fsSmall));
        }
        if (!tagLine.isBlank()) {
            rText.add(new Text("\n"+tagLine).setFont(FONT).setFontSize(fsSmall));
        }

        drawText(bounds, rText.toArray(new Text[0]));
    }

    private void drawRect(Bounds bounds) {
        this.canvas
            .saveState()
            .setStrokeColor(COLOR_LINES)
            .setLineWidth(1.0f)
            .setFillColor(COLOR_PLAQUE_FILL)
            .roundRectangle((float)x(bounds.getMinX()), (float)y(bounds.getMaxY()), (float)bounds.getWidth(), (float)bounds.getHeight(), 4.0f)
            .fillStroke()
            .restoreState()
        ;
    }

    private void drawText(final Bounds bounds, final Text... rt) {
        final var DY = (float)this.metrics.getFontSize();

        final var p = new Paragraph()
            .setTextAlignment(TextAlignment.CENTER)
            .setFontKerning(FontKerning.YES)
            .setFontSize(DY)
            .setMultipliedLeading(0.9f);

        Arrays.stream(rt).forEach(p::add);

        final var ddy = 5*DY; // TODO why is this necessary?
        var rect = new Rectangle(
            (float)x(bounds.getMinX()),
            (float)y(bounds.getMinY()+bounds.getHeight()+ddy),
            (float)bounds.getWidth(),
            (float)bounds.getHeight()+ddy);

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
