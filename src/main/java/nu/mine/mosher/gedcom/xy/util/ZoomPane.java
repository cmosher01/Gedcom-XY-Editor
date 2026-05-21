package nu.mine.mosher.gedcom.xy.util;

import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import nu.mine.mosher.gedcom.xy.*;
import nu.mine.mosher.gedcom.xy.shape.Position;

import java.util.Objects;

import static nu.mine.mosher.gedcom.xy.ChartBoundary.PADDING;


/**
 * From Stack Overflow.
 * Copyright © 2017, by Dániel Hári, haridaniel0@gmail.com, Budapest, Hungary.
 * Changes copyright © 2018, by Christopher A. Mosher, cmosher01@gmail.com, Shelton, Connecticut, USA.
 */
// NOT USED anymore; replaced by Scroller
public final class ZoomPane extends ScrollPane implements Scrollable {
    private static final double ZOOM_INTENSITY = 1.0e-3D;
//    private static final double ZOOM_INTENSITY = 0.003D;

    private final Pane target;
    private final Node zoomNode;
    private final VBox content;
    private final ChartBoundary chartBoundary;

    private double scale = 1.0D;



    public ZoomPane(final Pane target, final ChartBoundary boundary) {
        this.target = target;

        this.zoomNode = new Group(target);

        this.content = new VBox(this.zoomNode);

        this.chartBoundary = boundary;

        this.content.setAlignment(Pos.CENTER);
        this.content.setOnScroll(t -> {
            final var mouse = toCanvas(new Point2D(t.getX(), t.getY()));
            zoomTowards(Math.exp(ZOOM_INTENSITY * t.getDeltaY()), mouse);
            t.consume();
        });
        this.content.setOnMouseMoved(t -> {
            final var mouse = toCanvas(new Point2D(t.getX(), t.getY()));
            dumpPoint(mouse, "move");
//            dumpBoundsXAndTransforms(mouse);
        });
        setContent(this.content);
        setFitToWidth(true);
        setFitToHeight(true);

        setPannable(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setVbarPolicy(ScrollBarPolicy.NEVER);
//        setHbarPolicy(ScrollBarPolicy.ALWAYS);
//        setVbarPolicy(ScrollBarPolicy.ALWAYS);

        updateScale();

        // This removes the "offset" of the origin of the chart, effectively
        // making the scrolling region congruent with the entire chart (I believe).
        // It definitely does remove the transformation on the zoomNode.
        // I thought this would make it less confusing, but it just messes everything up.
//        this.zoomNode.setTranslateX(this.zoomNode.getBoundsInLocal().getMinX());
//        this.zoomNode.setTranslateY(this.zoomNode.getBoundsInLocal().getMinY());
    }

    private Point2D toCanvas(final Point2D coordContent) {
        return this.target.parentToLocal(this.zoomNode.parentToLocal(coordContent));
//        return this.target.parentToLocal(coordContent);
//        return this.zoomNode.parentToLocal(coordContent);
//        return coordContent;
    }






    @Override
    public void scrollTo(final Point2D position) {
//        final var boundsScrollingRegion = this.target.getBoundsInLocal();
//        final var boundsViewport = this.getViewportBounds();
//
//        final var hr = boundsViewport.getWidth()/2/boundsScrollingRegion.getWidth();
//        final var vr = boundsViewport.getHeight()/2/boundsScrollingRegion.getHeight();
//
//        final var h =
//            Math.abs(position.getX()-boundsScrollingRegion.getMinX())
//            /
//            Math.abs(boundsScrollingRegion.getWidth());
//        final var v =
//            Math.abs(position.getY()-boundsScrollingRegion.getMinY())
//            /
//            Math.abs(boundsScrollingRegion.getWidth());
//
//        final var hv = new Position(h-hr,v-vr);
        final var hv = posScrollBarsFor(position);
        final var p = hv.within(getHmin(), getHmax(), getVmin(), getVmax());

        this.setHvalue(p.h());
        this.setVvalue(p.v());
        dumpPoint(position, "autojump (after)");
    }

    private Position posScrollBarsFor(final Point2D p) {
        final var canvas = this.target.getBoundsInLocal();
        final var h = Math.clamp((p.getX()-canvas.getMinX()) / canvas.getWidth(), 0.0D, 1.0D);
        final var v = Math.clamp((p.getY()-canvas.getMinY()) / canvas.getHeight(), 0.0D, 1.0D);

        return new Position(h,v);
    }

    private void updateScale() {
        this.target.setScaleX(this.scale);
        this.target.setScaleY(this.scale);
    }

    private void zoomTowards(final double zoom, final Point2D mouse) {
        final var scaleNew = this.scale * zoom;
        if (scaleNew < 1e-2D || 1e+2D < scaleNew) {
            // limit of zoom in/out
            System.out.println("hit min/max zoom");
            System.out.println("----------");
            return;
        }

        if (Math.abs(scaleNew-scale) < 1e-5D) {
            System.out.println("too small change in zoom factor, ignoring");
            System.out.println("----------");
            return;
        }

        dumpPoint(mouse, "scroll (before)");
//            dumpBoundsXAndTransforms(mouse);

        final var view = calcView();
        final var centerOld = new Point2D(view.getCenterX(), view.getCenterY());
        final var offsetOld = mouse.subtract(centerOld);
        final var offsetNew = offsetOld.multiply(0.2D/zoom);
        final var centerNew = mouse.subtract(offsetNew);


//        final double h = getHvalue() * (this.zoomNode.getLayoutBounds().getWidth() - getViewportBounds().getWidth());
//        final double v = getVvalue() * (this.zoomNode.getLayoutBounds().getHeight() - getViewportBounds().getHeight());

        if (scaleNew < this.scale) {
            System.out.println("zooming out (smaller scale)");
        } else {
            System.out.println("zooming in (larger scale)");
        }

        this.scale = scaleNew;
        updateScale();
        layout(); // refresh ScrollPane scroll positions & target bounds
        System.out.println("----------");
        scrollTo(centerNew);




        // calculate adjustment of scroll position (pixels)
//        final Point2D adjustment = this.target.getLocalToParentTransform().deltaTransform(mouseLocal.multiply(zoom));
//        final Point2D adjustment = mouseLocal;
//        final Point2D adjustment = Point2D.ZERO;
// TODO Fix this: when zooming into mouse position, it's a little off, and the amount varies by tree



        // convert back to [0, 1] range
        // (too large/small values are automatically corrected by ScrollPane)
//        setHvalue((h + adjustment.getX()) / (this.zoomNode.getBoundsInLocal().getWidth() - getViewportBounds().getWidth()));
//        setVvalue((v + adjustment.getY()) / (this.zoomNode.getBoundsInLocal().getHeight() - getViewportBounds().getHeight()));
//        scrollTo(adjustment);
    }

    private void dumpPoint(final Point2D pt, final String move) {
        final var boundsView = calcView(); //view of the scrolling region (canvas/chart) visible via the viewport, in _XY coordinates
        final var ptViewCenter = new Point2D(boundsView.getCenterX(), boundsView.getCenterY());
        final var posMouseOffCenter = pt.subtract(ptViewCenter);

        System.out.printf(" point ( x, y) = (%6.1f,%6.1f) %s\n", pt.getX(), pt.getY(), move);
        System.out.printf("center ( x, y) = (%6.1f,%6.1f)\n", ptViewCenter.getX(), ptViewCenter.getY());
        System.out.printf("offset (dx,dy) = (%6.1f,%6.1f)\n", posMouseOffCenter.getX(), posMouseOffCenter.getY());
        System.out.printf("scrollbar(h,v) = (%5.3f,%5.3f)\n", getHvalue(), getVvalue());
        System.out.printf("scale          = %10.6f\n", this.scale);
        System.out.println("----------");
    }

    private void dumpBoundsXAndTransforms(final Point2D mouse) {
        final var boundsView = calcView(); //view of the scrolling region (canvas/chart) visible via the viewport, in _XY coordinates
        final var ptViewCenter = new Point2D(boundsView.getCenterX(), boundsView.getCenterY());
        final var posMouseOffCenter = mouse.subtract(ptViewCenter);

        System.out.printf(" mouse ( x, y) = (%6.1f,%6.1f)\n", mouse.getX(), mouse.getY());
        System.out.printf("center ( x, y) = (%6.1f,%6.1f)\n", ptViewCenter.getX(), ptViewCenter.getY());
        System.out.printf("offset (dx,dy) = (%6.1f,%6.1f)\n", posMouseOffCenter.getX(), posMouseOffCenter.getY());
//        System.out.printf("scrollbars (h,v) = (%5.3f,%5.3f)\n", getHvalue(), getVvalue());
        System.out.println();
        System.out.printf("scale = %10.6f\n", this.scale);
        System.out.println();

        final var boundsZoomPane = this.getBoundsInLocal();
        dumpBoundsX(boundsZoomPane, "ZoomPane", this);
        final var boundsViewport = this.getViewportBounds();
        dumpBoundsX(boundsViewport, "Viewport", this);

//        final var vp_vpXmin = new Point2D(boundsViewport.getMinX(), 0D);
//        final var can_vpXMin = toCanvas(vp_vpXmin);
//        final var vp_vpXmax = new Point2D(boundsViewport.getMaxX(), 0D);
//        final var can_vpXMax = toCanvas(vp_vpXmax);
//        final var can_vpWidth = can_vpXMax.getX()-can_vpXMin.getX();
//        final var can_vpBounds = new BoundingBox(can_vpXMin.getX(), 0D, 0D, can_vpWidth, 1D, 1D);
//        dumpBoundsX(can_vpBounds, "ViewportToCanvas", null);

//
//        final var vp_xVpStartMin = 0.0D; // by definition
//        final var vp_xVpCurrentMin = boundsViewport.getMinX();
//        final var vp_xVpCurrentMinFixed = vp_cVpFix(vp_xVpStartMin, vp_xVpCurrentMin);
//
//        final var vp_dxVpWidth = boundsViewport.getWidth();
//        final var vp_xVpStartMax = vp_xVpStartMin+vp_dxVpWidth;
//        final var vp_xVpCurrentMax = boundsViewport.getMaxX();
//        final var vp_xVpCurrentMaxFixed = vp_cVpFix(vp_xVpStartMax, vp_xVpCurrentMax);
//
//        final var vp_yVpStartMin = 0.0D; // by definition
//        final var vp_yVpCurrentMin = boundsViewport.getMinY();
//        final var vp_yVpCurrentMinFixed = vp_cVpFix(vp_yVpStartMin, vp_yVpCurrentMin);
//
//        final var vp_dyVpHeight = boundsViewport.getHeight();
//        final var vp_yVpStartMax = vp_yVpStartMin+vp_dyVpHeight;
//        final var vp_yVpCurrentMax = boundsViewport.getMaxY();
//        final var vp_yVpCurrentMaxFixed = vp_cVpFix(vp_yVpStartMax, vp_yVpCurrentMax);
//
//        final var boundsViewportFixed = new BoundingBox(vp_xVpCurrentMinFixed, vp_yVpCurrentMinFixed, vp_xVpCurrentMaxFixed-vp_xVpCurrentMinFixed, vp_yVpCurrentMaxFixed-vp_yVpCurrentMinFixed);
//        dumpBoundsX(boundsViewportFixed, "FixViewport", null);
//
        dumpBoundsX(boundsView, "View", null);

        final var widthVisible = boundsViewport.getWidth()/this.scale;
        System.out.printf("%16s:                         = %10.3f\n", "Visible Width", widthVisible);

        final var boundsContent = this.content.getBoundsInLocal();
        dumpBoundsX(boundsContent, "Content", this.content);
        final var boundsZoomNode = this.zoomNode.getBoundsInLocal();
        dumpBoundsX(boundsZoomNode, "ZoomNode", this.zoomNode);
        final var boundsScrollingRegion = this.target.getBoundsInLocal();
        dumpBoundsX(boundsScrollingRegion, "ScrollingRegion", this.target);
        final var parentScrollingRegion = this.target.getBoundsInParent();
        dumpBoundsX(parentScrollingRegion, "ParentScrolling", null);

        final var widthTravel = Math.max(0.9D, boundsScrollingRegion.getWidth()-widthVisible);
        System.out.printf("%16s:                         = %10.3f\n", "Travel Width", widthTravel);

//        final var dxMouse = mouse.getX()-boundsScrollingRegion.getMinX();
//        final var ratioTravelOfActual = widthTravel/boundsScrollingRegion.getWidth();
//        final var hOfMouse = Math.min(
//                (dxMouse*ratioTravelOfActual) / widthTravel,
//                1.0D);
//        System.out.printf("%16s: %10.5f\n", "h @ mouse X", hOfMouse);

        final var newHV = posScrollBarsFor(mouse);
        System.out.printf("scrollbars (h,v) = (%5.3f,%5.3f)\n", newHV.h(), newHV.v());

//        final var layoutScrollingRegion = this.target.getLayoutBounds();
//        dumpBoundsX(layoutScrollingRegion, "LayoutScrolling", null);

        final var boundsChartBoundary = this.chartBoundary;
        final var xC = (boundsChartBoundary.maxX()-boundsChartBoundary.minX())/2.0D;
        final var yC = (boundsChartBoundary.maxY()-boundsChartBoundary.minY())/2.0D;
        System.out.printf("%16s: %10.3f - %10.3f = %10.3f  (%10.3f,%10.3f)  xf=%s\n", "ChartPadded", boundsChartBoundary.minX()-PADDING, boundsChartBoundary.maxX()+PADDING, boundsChartBoundary.maxX()-boundsChartBoundary.minX()+2*PADDING, xC, yC,"");
        System.out.printf("%16s: %10.3f - %10.3f = %10.3f                           xf=%s\n", "ChartBoundary", boundsChartBoundary.minX(), boundsChartBoundary.maxX(), boundsChartBoundary.maxX()-boundsChartBoundary.minX(), "");


        System.out.println("---------------------------------------------------------------");
    }

    private Bounds calcView() {
        final var boundsViewport = this.getViewportBounds();
        final var boundsScrollingRegion = this.target.getBoundsInLocal();
        final var scale = this.scale;

        final var vp_xVpStartMin = 0.0D; // by definition
        final var vp_xVpCurrentMin = boundsViewport.getMinX();
        final var vp_xVpCurrentMinFixed = vp_cVpFlip(vp_xVpStartMin, vp_xVpCurrentMin);

        final var vp_dxVpWidth = boundsViewport.getWidth();
        final var vp_xVpStartMax = vp_xVpStartMin+vp_dxVpWidth;
        final var vp_xVpCurrentMax = boundsViewport.getMaxX();
        final var vp_xVpCurrentMaxFixed = vp_cVpFlip(vp_xVpStartMax, vp_xVpCurrentMax);

        final var vp_yVpStartMin = 0.0D; // by definition
        final var vp_yVpCurrentMin = boundsViewport.getMinY();
        final var vp_yVpCurrentMinFixed = vp_cVpFlip(vp_yVpStartMin, vp_yVpCurrentMin);

        final var vp_dyVpHeight = boundsViewport.getHeight();
        final var vp_yVpStartMax = vp_yVpStartMin+vp_dyVpHeight;
        final var vp_yVpCurrentMax = boundsViewport.getMaxY();
        final var vp_yVpCurrentMaxFixed = vp_cVpFlip(vp_yVpStartMax, vp_yVpCurrentMax);

//        final var boundsViewportFixed = new BoundingBox(vp_xVpCurrentMinFixed, vp_yVpCurrentMinFixed, vp_xVpCurrentMaxFixed-vp_xVpCurrentMinFixed, vp_yVpCurrentMaxFixed-vp_yVpCurrentMinFixed);

        return new BoundingBox(boundsScrollingRegion.getMinX()+(vp_xVpCurrentMinFixed/scale), boundsScrollingRegion.getMinY()+(vp_yVpCurrentMinFixed/scale), (vp_xVpCurrentMaxFixed-vp_xVpCurrentMinFixed)/scale, (vp_yVpCurrentMaxFixed-vp_yVpCurrentMinFixed)/scale);
    }

    private double vp_cVpFlip(final double vp_cVpStart, final double vp_cVpCurrent) {
        return 2*vp_cVpStart-vp_cVpCurrent;
    }

    private static void dumpBoundsX(final Bounds bounds, final String name, final Node node) {
        String xf = "";
        String xfname = "";

        if (Objects.nonNull(node)) {
            final var xform = node.localToParentTransformProperty();
            xfname = "";
            if (xform.isNotNull().get() && !xform.get().isIdentity()) {
                xfname = "[TRANSFORM]";
                xf = xform.get().toString();
            }
        }

        System.out.printf("%16s: %10.3f - %10.3f = %10.3f  (%10.3f,%10.3f)  %s\n", name, bounds.getMinX(), bounds.getMaxX(), bounds.getWidth(), bounds.getCenterX(), bounds.getCenterY(), xfname);
//        if (!xf.isBlank()) {
//            System.out.println(xf);
//        }
    }
}
