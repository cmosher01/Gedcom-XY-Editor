package nu.mine.mosher.gedcom.xy.util;


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
import nu.mine.mosher.gedcom.xy.Metrics;

import java.io.*;
import java.util.Objects;

public class PdfBuilder implements AutoCloseable {
    private static final javafx.scene.paint.Color SOL_PLAQUE_FILL = Solarized.BASE3;
    private static final Color COLOR_PLAQUE_FILL = new DeviceRgb(
        (float)SOL_PLAQUE_FILL.getRed(), (float)SOL_PLAQUE_FILL.getGreen(), (float)SOL_PLAQUE_FILL.getBlue());
    private static final javafx.scene.paint.Color SOL_LINES = Solarized.BASE00;
    private static final Color COLOR_LINES = new DeviceRgb(
        (float)SOL_LINES.getRed(), (float)SOL_LINES.getGreen(), (float)SOL_LINES.getBlue());

    private static final PdfFont FONT;
    private static final PdfFont FONT_BOLD;
    private static final PdfFont FONT_ITALIC;
    static {
        try {
            FONT = PdfFontFactory.createFont("/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf");
            FONT_BOLD = PdfFontFactory.createFont("/usr/share/fonts/truetype/noto/NotoSans-Bold.ttf");
            FONT_ITALIC = PdfFontFactory.createFont("/usr/share/fonts/truetype/noto/NotoSans-Italic.ttf");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



    private final Metrics metrics;
    private final PageSize psize;
    private final PdfWriter writer;
    private final PdfDocument pdfdoc;
    private final PdfPage pdfPage;
    private final PdfCanvas canvas;
    private final float margin;



    public PdfBuilder(Metrics metrics, File fileToSaveAs, Point2D size) throws IOException {
        this.metrics = metrics;
        this.margin = (float)(2.0*this.metrics.getWidthMax());
        this.psize = new PageSize((float)(size.getX()+2.0*this.margin), (float)(size.getY()+2.0*this.margin));
        this.writer = new PdfWriter(fileToSaveAs);
        this.pdfdoc = new PdfDocument(this.writer);
        this.pdfPage = this.pdfdoc.addNewPage(psize);
        this.canvas = new PdfCanvas(this.pdfPage);
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
        return this.margin+x;
    }

    private double y(double y) {
        return this.psize.getHeight()-(this.margin+y);
    }

    public void addPhantom(final Bounds bounds) {
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
        final var p = new Paragraph()
            .setTextAlignment(TextAlignment.CENTER)
            .setMultipliedLeading(0.85f)
            .setFontKerning(FontKerning.YES)
            .setFontSize((float)this.metrics.getFontSize())
            .add(new Text("\u00A0?\u00A0"))
        ;
        final float FUDGE_Y = 2.0f;
        final float FUDGE_X = 0f;

        var rect = new Rectangle(
            (float)x(bounds.getMinX())-FUDGE_X,
            (float)y(bounds.getMaxY())-FUDGE_Y,
            (float)bounds.getWidth()+FUDGE_X,
            (float)bounds.getHeight()+FUDGE_Y);

        try (final var ch = new Canvas(this.canvas, rect)) {
            ch.add(p);
        }
    }

    public void addPerson(final Bounds bounds, String nameGiven, String nameSur, String dates, String tagLine, final String refn) {
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

        final var p = new Paragraph()
            .setTextAlignment(TextAlignment.CENTER)
            .setMultipliedLeading(0.85f)
            .setFontKerning(FontKerning.YES)
            .setFontSize((float)this.metrics.getFontSize())
            .add(tG)
            .add(tS)
            .add(tD)
            .add(tT)
        ;

        final float FUDGE_Y = 2.0f;
        final float FUDGE_X = 0f;

        var rect = new Rectangle(
            (float)x(bounds.getMinX())-FUDGE_X,
            (float)y(bounds.getMaxY())-FUDGE_Y,
            (float)bounds.getWidth()+FUDGE_X,
            (float)bounds.getHeight()+FUDGE_Y);

        try (final var ch = new Canvas(this.canvas, rect)) {
            ch.add(p);
        }
    }
}
