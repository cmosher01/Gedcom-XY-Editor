package nu.mine.mosher.gedcom.xy;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import nu.mine.mosher.collection.TreeNode;
import nu.mine.mosher.gedcom.GedcomLine;
import nu.mine.mosher.gedcom.date.DatePeriod;
import nu.mine.mosher.gedcom.xy.util.Dim2D;

import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.text.*;
import java.util.*;
import java.util.List;

import static java.awt.font.TextAttribute.*;
import static java.awt.image.BufferedImage.TYPE_BYTE_BINARY;
import static java.lang.Math.max;
import static java.util.Collections.singletonMap;

public class Indi {
    public static final String FONT_LOGICAL_NAME = "Garamond";
    public static final float FONT_POINT_SIZE = 9.0F;
    public static final int MAX_WIDTH_EMS = 16;
    public static final double TOP_MARGIN = 1.0;
    public static final double RIGHT_MARGIN = 3.0;
    public static final double BOTTOM_MARGIN = 3.0;
    public static final double LEFT_MARGIN = 3.0;
    public static final int DIM = 0x800;
    public static final String PFX_BIRTH = "\u200a*\u200a";
    public static final String PFX_DEATH = "\u2020";

    private final TreeNode<GedcomLine> node;
    private final String id;
    private final Point2D coordsOrig = new Point2D.Double(0, 0);
    private final Dim2D dim = new Dim2D(0, TOP_MARGIN);
    private final List<Line> lines = new ArrayList<>();
    private final int sex;
    private final DatePeriod birth;

    private final Circle gr;

    public int getSex() {
        return this.sex;
    }

    public DatePeriod getBirth() {
        return this.birth;
    }

    public void addGraphicsTo(List<Node> addto) {
        addto.add(this.gr);
    }

    public void setOnDrag(EventHandler<MouseEvent> drag2) {
        this.gr.setOnMouseDragged(drag2);
    }

    public void shiftOrig(OptionalDouble x, OptionalDouble y) {
        this.coordsOrig.setLocation(
            this.coordsOrig.getX() - (x.isPresent() ? x.getAsDouble() : 0.0D),
            this.coordsOrig.getY() - (y.isPresent() ? y.getAsDouble() : 0.0D));
    }

    private static class Line {
        private final String text;
        private final Dim2D off;
        private final String refn;

        private Line(final String text, final Dim2D off, /*final SvgBuilder.ClassAttr cls,*/ final String refn) {
            this.text = text;
            this.off = off;
            this.refn = refn;
        }

//        private void buildAtInto(Point2D at, SvgBuilder svg) {
//            svg.add(this.text, getPoint(at), Optional.of(this.cls), "refn", this.refn);
//        }
//
//        private Point2D getPoint(Point2D at) {
//            return new Point2D.Double(at.getX() + this.off.getWidth(), at.getY() + this.off.getHeight());
//        }
    }





    public Indi(final TreeNode<GedcomLine> node, final Point2D coords, String id, String name, DatePeriod birth, DatePeriod death, String refn, final int sex) {
        this.node = node;
        this.id = id;
        this.coordsOrig.setLocation(coords);
        this.sex = sex;

        /*
        We need to layout the lines of text (3 lines, but will be more
        if we need to wrap an overly long name), and also
        calculate the total width and height, based on the size of
        the drawn text. This will be highly dependent on the font,
        size, kerning, etc., etc. Even so, it may not exactly match
        the bounds on the final rendering display device.
        Do the best we can. Use Garamond font (reasonably pretty,
        free, and ubiquitous), 9 point, normal, with kerning and
        ligatures.
         */
        final Graphics2D g = buildGraphics();
        lines.clear();

        if (!name.isEmpty()) {
            appendToDisplay(g, name, /*CLS_NAME,*/ refn, this.dim, this.lines);
        }

        this.birth = birth;
        final String displayBirth = dateString(birth);
        if (!displayBirth.isEmpty()) {
            appendToDisplay(g, PFX_BIRTH + displayBirth, /*CLS_BIRTH,*/ refn, this.dim, this.lines);
        }
        final String displayDeath = dateString(death);
        if (!displayDeath.isEmpty()) {
            appendToDisplay(g, PFX_DEATH + displayDeath, /*CLS_DEATH,*/ refn, this.dim, this.lines);
        }

        this.dim.setSize(this.dim.getWidth(), this.dim.getHeight()+BOTTOM_MARGIN);


        gr = new Circle(10, javafx.scene.paint.Color.CORNSILK);
        gr.setStroke(Color.GREEN);
        gr.setStrokeWidth(1.2D);

    }

    private static String dateString(final DatePeriod date) {
        return date.getTabularString().toLowerCase();
    }

    public String getId() {
        return this.id;
    }

    public Point2D getCoordsOrig() {
        final Point2D p = new Point2D.Double();
        p.setLocation(this.coordsOrig);
        return p;
    }

    public void setFromCoords() {
        this.gr.relocate(this.coordsOrig.getX(), this.coordsOrig.getY());
    }

    public Circle getCircle() {
        return this.gr;
    }






    private static Graphics2D buildGraphics() {
        return configureGraphics(new BufferedImage(DIM, DIM, TYPE_BYTE_BINARY).createGraphics());
    }

    private static Graphics2D configureGraphics(final Graphics2D g) {
        g.setFont(Font.decode(FONT_LOGICAL_NAME).deriveFont(FONT_POINT_SIZE).deriveFont(getFontAttrs()));
        return g;
    }

    private static Map<TextAttribute, Integer> getFontAttrs() {
        final Map<TextAttribute, Integer> map = new HashMap<>();
        map.put(KERNING, KERNING_ON);
        map.put(LIGATURES, LIGATURES_ON);
        return Collections.unmodifiableMap(map);
    }

    private static void appendToDisplay(final Graphics2D g, final String str, /*final SvgBuilder.ClassAttr cls,*/ final String refn, final Dim2D dim, final List<Line> lines) {
        final double maxWidth = getMaxWidth(g);
        final LineBreakMeasurer breaker = new LineBreakMeasurer(getAttrStr(str, g), g.getFontRenderContext());

        int cur = 0;
        double dy = dim.getHeight();
        double maxw = dim.getWidth();
        while (breaker.getPosition() < str.length()) {
            final int next = breaker.nextOffset((float) maxWidth);
            final String line = str.substring(cur, next);
            cur = next;

            final TextLayout layout = breaker.nextLayout((float) maxWidth);
            final double dx = layout.isLeftToRight() ? LEFT_MARGIN : -RIGHT_MARGIN + -layout.getAdvance() + maxWidth;
            dy += layout.getAscent();

            lines.add(new Line(line, new Dim2D(dx, dy), /*cls,*/ refn));

            dy += layout.getDescent() + layout.getLeading();

            maxw = max(maxw, LEFT_MARGIN + layout.getAdvance() + RIGHT_MARGIN);
        }
        dim.setSize(maxw, dy);
    }

    private static double getMaxWidth(final Graphics2D g) {
        return MAX_WIDTH_EMS * g.getFontMetrics().stringWidth("M") - LEFT_MARGIN - RIGHT_MARGIN;
    }

    private static AttributedCharacterIterator getAttrStr(final String line, final Graphics2D g) {
        return new AttributedString(line, singletonMap(FONT, g.getFont())).getIterator();
    }
}
