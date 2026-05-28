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

import javafx.geometry.*;
import javafx.scene.input.*;
import javafx.scene.layout.Pane;
import nu.mine.mosher.gedcom.xy.Scrollable;

import java.util.*;

public class Scroller extends Pane implements Scrollable {
    private static final double SCALE_DELTA = 5.0e-3D;
    private static final double MIN_SIZE_CANVAS = 200.0D;
    private static final double MAX_SCALE = 1.0e2D;

    private final CanvasWrapper canvas;
    private final TranslateHandler translate = new TranslateHandler();
    private final ScaleHandler scale = new ScaleHandler();

    private Scroller(final Pane canvas) {
        super(canvas);
        this.canvas = new CanvasWrapper(canvas);
    }

    public static Scroller create(final Pane canvas) {
        final var ret = new Scroller(canvas);
        ret.setOnMousePressed(ret.translate::onMousePressed);
        ret.setOnMouseDragged(ret.translate::onMouseDragged);
        ret.setOnMouseReleased(ret.translate::onMouseReleased);
        ret.setOnScroll(ret.scale::onScroll);
        return ret;
    }

    @Override
    public void scrollTo(final Point2D to) {
/*
variable (x,y) naming convention:
"ct_name" where:
  c is coord system:
      "v"=viewport/window  (i.e., this scroller)
      "c"=canvas/chart/_XY (i.e., this scroller's canvas)
  t is type:
      either empty, or
      "p" for prime (')
      Examples:
          l is just plain l ("ell")
          lp represents l' ("ell prime"), a translated l
  name is:
      "p" pivot point (i.e., what the user wants to scroll into view)
      "c" is the center of the viewport
      "l" is the layout (i.e., top left corner) of canvas
      "d" is the delta vector to move "p" by
*/
        final var c_p  = to;
        final var c_c  = this.center();
        final var v_l  = this.canvas.layout();
        final var c_l  = this.canvas.viewportToCanvas(v_l);
        final var c_d  = c_c.subtract(c_p);
        final var c_lp = c_l.add(c_d);
        final var v_lp = this.canvas.canvasToViewport(c_lp);
//        System.out.println("calculated layout: "+v_lp);
        this.canvas.layout(v_lp);
    }

    @Override
    public void scaleTo(final double scale) {
        this.canvas.scaleTo(scale);
    }

    @Override
    public void scaleTo() {
        scaleTo(1.0D);
    }

    @Override
    public void scaleToFit(final Bounds boundsChart) {
        this.canvas.scaleToFit(boundsChart, this);
    }

    @Override
    // canvas coordinates
    public Point2D center() {
        final var v_center = pt(getWidth()/2D, getHeight()/2D);
        return this.canvas.viewportToCanvas(v_center);
    }



    private class TranslateHandler {
        private final LinkedList<Point2D> offset = new LinkedList<>();

        public void onMousePressed(final MouseEvent t) {
            t.consume();
            dumpEvent("pressed", t);
            final var ptMouse = pt(t.getSceneX(), t.getSceneY());
            final var ptCanvas = canvas.layout();
            final var dptOffset = ptMouse.subtract(ptCanvas);
            this.offset.clear();
            this.offset.offer(dptOffset);
        }

        public void onMouseDragged(final MouseEvent t) {
            if (this.offset.isEmpty()) {
                // not for us, just passing through
                dumpEvent(" [nop]  ", t);
                return;
            }
            dumpEvent("dragged", t);
            final var ptMouse = pt(t.getSceneX(), t.getSceneY());
            final var dptOffset = this.offset.peek();
            final var delta = ptMouse.subtract(dptOffset);
            Scroller.this.canvas.layout(delta);
            // don't consume event, so status bar gets updated by scroller event handler
        }

        public void onMouseReleased(final MouseEvent t) {
            t.consume();
            dumpEvent("released", t);
            this.offset.clear();
        }
    }

    private void dumpEvent(final String name, final MouseEvent event) {
//        final var v_e = pt(event.getSceneX(), event.getSceneY());
//        final var c_e = Scroller.this.canvas.viewportToCanvas(v_e);
//        System.out.printf("translate: %8s  v=(%7.1f,%7.1f) c=(%7.1f,%7.1f)\n",
//            name, v_e.getX(), v_e.getY(), c_e.getX(), c_e.getY());
    }


    private class ScaleHandler {
        public void onScroll(final ScrollEvent t) {
            t.consume();

            /*
                ScrollEvent.getDeltaY()

                How far the user scrolled (dragged with center mouse
                button, dragged two fingers on touchpad, turned mouse
                wheel, etc.).

                Positive values for scrolled "away" from user, or up,
                which mean "zoom in" (larger scale, larger canvas).

                Negative values for scrolled "towards" user, or down,
                which mean "zoom out" (smaller scale, smaller canvas).
             */
            final var dy = t.getDeltaY();
            final var zoomOut = dy < 0D;
            final var zoomIn = !zoomOut;

            if (
                (Math.abs(dy) < 1e-2D) ||
                (zoomOut && canvas.tooSmall()) ||
                (zoomIn && canvas.tooLarge())) {
                return;
            }

            final var z = Math.exp(dy * SCALE_DELTA);

            // scale to zoom in or out
            canvas.scaleBy(z);

/*
We want to translate canvas to lp, so pivot point p
appears to stay in the same location visually.
By default, scaling uses center C as pivot point.

Math for one dimension (the other dimension is identical).
Note: none of these calculations depend on the viewport
size or postion, only canvas and mouse.

p = pivot point (mouse position)
l = canvas layout (min x or y)
w = canvas width
m = p-(w/2)
lp= m-(z*(m-l))



Initial state:

m             l        p
              |_________________C_________________| <--- canvas

              |<-------------- (w) -------------->|
              |<---- (w/2) ---->|
|<-- (m-l) ---|



After zooming (which by default is towards center C):

                          p translated, so we need to translate
m         l'   p' <------ the canvas to put p' back to p

          |_____________________C_____________________| <--- canvas



After translate adjustment:

m                 lp   p <------ correctly adjusted position of p'
                  |_____________________C_____________________| <--- canvas

|<-- (z*(m-l)) ---|    p
m                 lp <------ layout (min x) that causes p' to
                             correctly be adjusted back to p
*/
            final var p = pt(t.getSceneX(), t.getSceneY());
            final var m = p.add(canvas.size().multiply(-1D/2D)); //m = p-(w/2)
            final var lp = m.subtract(m.subtract(canvas.layout()).multiply(z)); // lp = m-(z*(m-l))
            canvas.layout(lp);
        }
    }


    /**
     * Simplified interface to the canvas Pane. Contains only
     * what Scroller needs. Also provides nicer methods, in terms
     * of Point2D objects rather than pairs of setBlahX() and setBlahY() methods.
     */
    private record CanvasWrapper(Pane canvas) {
        public boolean tooLarge() {
            return MAX_SCALE <= this.canvas.getScaleX();
        }

        public boolean tooSmall() {
            final var sz = this.canvas.getBoundsInParent();
            return
                sz.getWidth () <= MIN_SIZE_CANVAS ||
                sz.getHeight() <= MIN_SIZE_CANVAS;
        }

        public Point2D size() {
            return pt(this.canvas.getWidth(), this.canvas.getHeight());
        }

        public Point2D layout() {
            return pt(this.canvas.getLayoutX(), this.canvas.getLayoutY());
        }

        public void layout(final Point2D layout) {
            this.canvas.setLayoutX(layout.getX());
            this.canvas.setLayoutY(layout.getY());
        }

        public void scaleBy(final double z) {
            scaleTo(z * this.canvas.getScaleX());
        }

        public void scaleTo(final double scale) {
            this.canvas.setScaleX(scale);
            this.canvas.setScaleY(scale);
        }

        public Point2D canvasToViewport(final Point2D c_local) {
            return this.canvas.localToParent(c_local);
        }

        public Point2D viewportToCanvas(final Point2D v_local) {
            final var c_local = this.canvas.parentToLocal(v_local);
            if (Objects.isNull(c_local)) {
                // this happens if called before layout is complete
                throw new IllegalStateException();
            }
            return c_local;
        }

        public void scaleToFit(final Bounds c, final Scroller scroller) {
            final var v = scroller.getBoundsInLocal();
            final var w = v.getWidth ()/c.getWidth();
            final var h = v.getHeight()/c.getHeight();
            final var s = Math.min(w,h);
            scaleTo(s);
        }
    }




    private static Point2D pt(final double x, final double y) {
        return new Point2D(x,y);
    }
}
