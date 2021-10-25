package nu.mine.mosher.gedcom.xy.util;

import javafx.geometry.*;
import javafx.scene.shape.Line;
import org.w3c.dom.*;

import javax.xml.parsers.*;
import java.util.Objects;

public class SvgBuilder {
    private static final String W3C_SVG_NS_URI = "http://www.w3.org/2000/svg";

    private final Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    private final long fontsize;
    private final Element svg = this.doc.createElementNS(W3C_SVG_NS_URI, "svg");

    public SvgBuilder(long fontsize) throws ParserConfigurationException {
        this.fontsize = fontsize;

        this.doc.appendChild(svg);

        final Element style = this.doc.createElementNS(W3C_SVG_NS_URI, "style");
        style.setTextContent(
            "\n" +
                "svg { font-family: 'noto sans'; font-size: "+ this.fontsize +"pt; }\n" +
                "text { font-size: 80%; }\n" +
                "rect { stroke: skyblue; stroke-width: 0.5px; fill: beige; }\n" +
                "line { stroke: skyblue; stroke-width: 0.5px; }\n" +

                ".person { fill: dimgray; }\n" +
                ".nameGiven { font-weight: bold; }\n" +
                ".date { fill: darkgray; font-size: 65%; }\n" +
                ".tagline { fill: darkgray; font-size: 65%; }\n"
        );
        this.svg.appendChild(style);
    }

    public Document get() {
        return this.doc;
    }



    public void addLine(final Line line) {
        if (Objects.isNull(line)) {
            return;
        }

        final Element e = this.doc.createElementNS(W3C_SVG_NS_URI, "line");

        e.setAttribute("x1", Double.toString(line.getStartX()));
        e.setAttribute("y1", Double.toString(line.getStartY()));

        e.setAttribute("x2", Double.toString(line.getEndX()));
        e.setAttribute("y2", Double.toString(line.getEndY()));

        this.svg.appendChild(e);
    }

    public void addPerson(final Bounds bounds, String nameGiven, String nameSur, final String dates, final String tagLine, final String refn) {
        final Element eRect = this.doc.createElementNS(W3C_SVG_NS_URI, "rect");
        eRect.setAttribute("x", Double.toString(bounds.getMinX()));
        eRect.setAttribute("y", Double.toString(bounds.getMinY()));
        eRect.setAttribute("width", Double.toString(bounds.getWidth()));
        eRect.setAttribute("height", Double.toString(bounds.getHeight()));
        eRect.setAttribute("rx", "2"); // TODO don't hardcode 2 for rounded corners
        eRect.setAttribute("ry", "2");
        eRect.setAttribute("id", refn);
        this.svg.appendChild(eRect);

        if (nameGiven.isBlank() && nameSur.isBlank()) {
            nameSur = "?";
        } else if (!nameGiven.isBlank() && !nameSur.isBlank()) {
            nameSur = "\u00A0" + nameSur;
        }

        addText(bounds, nameGiven, nameSur, dates, tagLine, refn, "");
    }

    private void addText(final Bounds bounds, String nameGiven, String nameSur, final String dates, final String tagLine, final String refn, final String mask) {
        final Element eText = this.doc.createElementNS(W3C_SVG_NS_URI, "text");
        eText.setAttribute("class", "person"+mask);
        eText.setAttribute("data-refn", refn);
        eText.setAttribute("text-anchor", "middle");
        eText.setAttribute("shape-inside", "url(#"+refn+")");
        eText.setAttribute("dy", ""+this.fontsize);

        final Element eNameGiven = this.doc.createElementNS(W3C_SVG_NS_URI, "tspan");
        eNameGiven.setAttribute("class", "nameGiven"+mask);
        if (!nameGiven.isBlank()) {
            eNameGiven.setTextContent(nameGiven);
        }
        eText.appendChild(eNameGiven);

        final Element eNameSur = this.doc.createElementNS(W3C_SVG_NS_URI, "tspan");
        eNameSur.setAttribute("class", "nameSur"+mask);
        if (!nameSur.isBlank()) {
            eNameSur.setTextContent(nameSur);
        }
        eText.appendChild(eNameSur);

        final Element eDate = this.doc.createElementNS(W3C_SVG_NS_URI, "tspan");
        eDate.setAttribute("class", "date"+mask);
        eDate.setAttribute("x", Double.toString(bounds.getMinX()));
        eDate.setAttribute("dy", ""+this.fontsize);
        eDate.setTextContent(dates);
        eText.appendChild(eDate);

        final Element eTagLine = this.doc.createElementNS(W3C_SVG_NS_URI, "tspan");
        eTagLine.setAttribute("class", "tagline"+mask);
        eTagLine.setAttribute("x", Double.toString(bounds.getMinX()));
        eTagLine.setAttribute("dy", ""+this.fontsize);
        eTagLine.setTextContent(tagLine);
        eText.appendChild(eTagLine);

        this.svg.appendChild(eText);
    }


    private static void addPointTo(final Point2D pt, final Element e, final String sfx) {
        e.setAttribute("x" + sfx, Double.toString(pt.getX()));
        e.setAttribute("y" + sfx, Double.toString(pt.getY()));
    }

    private static void addOffset(final double x, final double dy, final Element e) {
        e.setAttribute("x", Double.toString(x));
        e.setAttribute("dy", Double.toString(dy));
    }
}
