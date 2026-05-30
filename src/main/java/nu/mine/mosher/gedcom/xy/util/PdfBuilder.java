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
import com.itextpdf.layout.Canvas;
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






    private final Metrics metrics;
    private final Point offset;
    private final PageSize sizePage;
    private final PdfDocument document;
    private final PdfCanvas canvas;
    private PdfFont fontRegular;
    private PdfFont fontBold;



    public PdfBuilder(final Metrics metrics, final File fileToSaveAs, final Bounds bounds) throws IOException {
        initFonts();
        if (Objects.isNull(this.fontBold)) {
            throw new IOException("Cannot create PDF files, because cannot load any fonts.");
        }

        this.metrics = metrics;

        final var margin = new Insets(this.metrics.margin());

        final var page = new BoundingBox(
            bounds.getMinX() - margin.getLeft(),
            bounds.getMinY() - margin.getTop(),
            bounds.getWidth() + margin.getLeft() + margin.getRight(),
            bounds.getHeight() + margin.getTop() + margin.getBottom());
        this.offset = new Point(page.getMinX(), page.getMinY());
        this.sizePage = new PageSize((float)page.getWidth(), (float)page.getHeight());

        final var writer = new PdfWriter(fileToSaveAs);
        this.document = new PdfDocument(writer);
        final var pdfPage = this.document.addNewPage(sizePage);
        // TODO large pages don't open in Acrobat Reader; this doesn't fix it:
//        pdfPage.put(PdfName.UserUnit, new PdfNumber(1000f));
        this.canvas = new PdfCanvas(pdfPage);
    }

    private void initFonts() {
        try {
            fontRegular = PdfBuilder.getFontRes("NotoSans-Regular.ttf");
            fontBold = PdfBuilder.getFontRes("NotoSans-Bold.ttf");
            LOG.info("Successfully loaded NotoSans font resources.");
            return;
        } catch (Exception e) {
            LOG.error("Error loading NotoSans font resource.", e);
            // continue
        }

        try {
            fontRegular = PdfFontFactory.createFont("/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf");
            fontBold = PdfFontFactory.createFont("/usr/share/fonts/truetype/noto/NotoSans-Bold.ttf");
            LOG.info("Successfully loaded NotoSans fonts from file system.");
            return;
        } catch (Exception e) {
            LOG.error("Error loading NotoSans fonts.", e);
            // continue
        }

        try {
            fontRegular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            LOG.info("Successfully loaded PDF Helvetica fonts.");
            return;
        } catch (Exception e) {
            LOG.error("Error loading PDF-standard Helvetica fonts.", e);
            // continue
        }

        try {
            fontRegular = PdfFontFactory.createFont();
            fontBold = fontRegular;
            LOG.info("Couldn't find any betters fonts, so using PDF default font.");
            return;
        } catch (Exception e) {
            LOG.error("Error loading PDF-standard default font.", e);
            //continue
        }

        LOG.error("CANNOT CREATE PDF FILES DUE TO ERROR FINDING ANY FONTS.");
    }

    public void close() {
        this.document.close();
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
        return x-this.offset.getX();
    }

    private double y(double y) {
        return this.sizePage.getHeight()-(y-this.offset.getY());
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
                case UNKNOWN -> rText.add(new Text("?").setFont(fontRegular));
                case SPACE -> rText.add(new Text(" ").setFont(fontRegular));
                case GIVEN0 -> rText.add(new Text(name.given0()).setFont(fontBold));
                case SUR -> rText.add(new Text(name.sur()).setFont(fontRegular));
                case GIVEN1 -> rText.add(new Text(name.given1()).setFont(fontRegular));
            }
        }
        if (!dates.isBlank()) {
            rText.add(new Text("\n"+dates).setFont(fontRegular).setFontSize(fsSmall));
        }
        if (!tagLine.isBlank()) {
            rText.add(new Text("\n"+tagLine).setFont(fontRegular).setFontSize(fsSmall));
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
