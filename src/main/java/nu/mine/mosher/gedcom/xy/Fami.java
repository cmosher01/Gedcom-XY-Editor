package nu.mine.mosher.gedcom.xy;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import nu.mine.mosher.gedcom.xy.util.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static nu.mine.mosher.gedcom.xy.Indi.CORNERS;

public class Fami {
    private static final double MIN_DISTANCE = 1.51D;


    private Metrics metrics;

    private Indi husb;
    private Indi wife;
    private final List<Indi> rChild = new ArrayList<>();

    private Line parentBar1;
    private Line parentBar2;
    private final List<StackPane> phantomPanes = new ArrayList<>(0);

    private Line descentBar1;
    private Line descentBar2;
    private Line descentBar3;

    private Line childBar;
    private Line[] rChildBar;

    public void setMetrics(final Metrics metrics) {
        this.metrics = metrics;
    }

    public void setHusb(final Indi indi) {
        husb = indi;
    }

    public Optional<Indi> getHusb() {
        return Optional.ofNullable(this.husb);
    }

    public void setWife(final Indi indi) {
        wife = indi;
    }

    public Optional<Indi> getWife() {
        return Optional.ofNullable(this.wife);
    }

    public void addChild(final Indi indi) {
        if (Objects.nonNull(indi)) {
            rChild.add(indi);
        }
    }

    public List<Indi> getChildren() {
        return Collections.unmodifiableList(this.rChild);
    }

    public void calc() {
        if (husb == null && wife == null && rChild.size() == 0) {
            return;
        }

        final Couple couple = new Couple(husb, wife);

        if (couple.exists) {
            parentBar1 = createLine();
            parentBar1.startXProperty().bind(couple.pt1x);
            parentBar1.startYProperty().bind(couple.pt1y.subtract(barHeight()));
            parentBar1.endXProperty().bind(couple.pt2x);
            parentBar1.endYProperty().bind(couple.pt2y.subtract(barHeight()));

            parentBar2 = createLine();
            parentBar2.startXProperty().bind(couple.pt1x);
            parentBar2.startYProperty().bind(couple.pt1y.add(barHeight()));
            parentBar2.endXProperty().bind(couple.pt2x);
            parentBar2.endYProperty().bind(couple.pt2y.add(barHeight()));
        }

        if (!rChild.isEmpty()) {
            childBar = createLine();
            childBar.startXProperty().bind(new DoubleBinding() {
                {
                    for (final Indi child : rChild) {
                        super.bind(child.x());
                    }
                }

                @Override
                protected double computeValue() {
                    return rChild.stream().mapToDouble(c -> c.x().get()).min().getAsDouble();
                }
            });
            childBar.endXProperty().bind(new DoubleBinding() {
                {
                    for (final Indi child : rChild) {
                        super.bind(child.x());
                    }
                }

                @Override
                protected double computeValue() {
                    return rChild.stream().mapToDouble(c -> c.x().get()).max().getAsDouble();
                }
            });
            final DoubleBinding top = new DoubleBinding() {
                {
                    for (final Indi child : rChild) {
                        super.bind(child.y());
                    }
                }

                @Override
                protected double computeValue() {
                    return rChild.stream().mapToDouble(c -> c.y().get()).min().getAsDouble() - childHeight();
                }
            };
            childBar.startYProperty().bind(top);
            childBar.endYProperty().bind(top);

            rChildBar = new Line[rChild.size()];
            for (int i = 0; i < rChildBar.length; i++) {
                final Indi c = rChild.get(i);
                rChildBar[i] = createLine();

                rChildBar[i].startXProperty().bind(c.x());
                rChildBar[i].startYProperty().bind(childBar.startYProperty());
                rChildBar[i].endXProperty().bind(c.x());
                rChildBar[i].endYProperty().bind(c.y());
            }








            if (couple.exists) {
                final DoubleBinding descentLineEndParentX = new DoubleBinding() {
                    {
                        super.bind(couple.pt1x, couple.pt1y, couple.pt2x, couple.pt2y, childBar.startXProperty(), childBar.endXProperty(), childBar.startYProperty());
                    }

                    @Override
                    protected double computeValue() {
                        final Point2D child = new Point2D((childBar.getStartX() + childBar.getEndX()) / 2.0D, childBar.getStartY());
                        final Point2D p1 = new Point2D(couple.pt1x.get(), couple.pt1y.get());
                        final Point2D p2 = new Point2D(couple.pt2x.get(), couple.pt2y.get());
                        return (child.distance(p1) < child.distance(p2) ? ptParentDescentBar(p1, p2) : ptParentDescentBar(p2, p1)).getX();
                    }
                };

                final DoubleBinding descentLineEndParentY = new DoubleBinding() {
                    {
                        super.bind(couple.pt1x, couple.pt1y, couple.pt2x, couple.pt2y, childBar.startXProperty(), childBar.endXProperty(), childBar.startYProperty());
                    }

                    @Override
                    protected double computeValue() {
                        final Point2D child = new Point2D((childBar.getStartX() + childBar.getEndX()) / 2.0D, childBar.getStartY());
                        final Point2D p1 = new Point2D(couple.pt1x.get(), couple.pt1y.get());
                        final Point2D p2 = new Point2D(couple.pt2x.get(), couple.pt2y.get());
                        return (child.distance(p1) < child.distance(p2) ? ptParentDescentBar(p1, p2) : ptParentDescentBar(p2, p1)).getY();
                    }
                };

                final DoubleBinding descentLineStartChildrenX = new DoubleBinding() {
                    {
                        super.bind(childBar.startXProperty(), childBar.endXProperty(), descentLineEndParentX);
                    }

                    @Override
                    protected double computeValue() {
                        final double offset = childHeight();
                        if (childBar.getEndX()-childBar.getStartX() < offset*2.0D) {
                            return (childBar.getEndX()+childBar.getStartX())/2.0D;
                        }
                        return clamp(childBar.getStartX()+offset, descentLineEndParentX.get(), childBar.getEndX()-offset);
                    }
                };






                descentBar3 = createLine();
                descentBar3.startXProperty().bind(descentLineStartChildrenX);
                descentBar3.startYProperty().bind(childBar.startYProperty());
                descentBar3.endXProperty().bind(descentBar3.startXProperty());
                descentBar3.endYProperty().bind(descentBar3.startYProperty().subtract(rChild.size() == 1 ? 0.0D : childHeight() / 2.0D));

                descentBar2 = createLine();
                descentBar2.startXProperty().bind(descentBar3.endXProperty());
                descentBar2.startYProperty().bind(descentBar3.endYProperty());
                descentBar2.endXProperty().bind(descentLineEndParentX);
                descentBar2.endYProperty().bind(descentBar2.startYProperty());

                descentBar1 = createLine();
                descentBar1.startXProperty().bind(descentBar2.endXProperty());
                descentBar1.startYProperty().bind(descentBar2.endYProperty());
                descentBar1.endXProperty().bind(descentBar1.startXProperty());
                descentBar1.endYProperty().bind(descentLineEndParentY);
            }
        }
    }








    private static double clamp(final double min, final double n, final double max) {
        if (max < min) {
            return n;
        }
        if (n < min) {
            return min;
        }
        if (max < n) {
            return max;
        }
        return n;
    }

    public double getMarrDistance() {
        if (husb == null || wife == null) {
            return 0D;
        }
        if (!husb.laidOut().isPresent() || !wife.laidOut().isPresent()) {
            return 0D;
        }
        return husb.laidOut().get().distance(wife.laidOut().get());
    }

    public double getGenDistance() {
        if (husb == null || wife == null) {
            return 0D;
        }
        final double avgChildX = this.rChild.stream().map(Indi::laidOut).filter(Optional::isPresent).map(Optional::get).mapToDouble(Point2D::getX).average().orElse(0D);
        final double avgChildY = this.rChild.stream().map(Indi::laidOut).filter(Optional::isPresent).map(Optional::get).mapToDouble(Point2D::getY).average().orElse(0D);
        if (avgChildX < 1D && avgChildY < 1D) {
            return 0D;
        }
        final Point2D avgChild = new Point2D(avgChildX, avgChildY);
        if (husb != null && husb.laidOut().isPresent() && wife != null && wife.laidOut().isPresent()) {
            return Math.min(husb.laidOut().get().distance(avgChild), wife.laidOut().get().distance(avgChild));
        }
        if (husb != null && husb.laidOut().isPresent()) {
            return husb.laidOut().get().distance(avgChild);
        }
        if (wife != null && wife.laidOut().isPresent()) {
            return wife.laidOut().get().distance(avgChild);
        }
        return 0D;
    }

    private Point2D ptParentDescentBar(final Point2D ptNear, final Point2D ptFar) {
        final Point2D ptStart = new Point2D(ptNear.getX(), ptNear.getY() + barHeight());
        final Point2D ptEnd = new Point2D(ptFar.getX(), ptFar.getY() + barHeight());
        final double pd = Math.max(ptStart.distance(ptEnd), MIN_DISTANCE);
        final double dt = pd < this.metrics.getWidthMax()*6 ? 0.5D : this.metrics.getWidthMax()/pd;
        return new Point2D((1 - dt) * ptStart.getX() + dt * ptEnd.getX(), (1 - dt) * ptStart.getY() + dt * ptEnd.getY());
    }

    private double barHeight() {
        return this.metrics.getBarHeight();
    }

    private double childHeight() {
        return this.metrics.getChildHeight();
    }

    private double marrSpacing() {
        return this.metrics.getMarrDistance();
    }

    private Line createLine() {
        return createLine(metrics.colorLines());
    }

    private Line createLine(final Color color) {
        final Line line = new Line();
        line.setStroke(color);
        line.setStrokeWidth(1.0D);
        return line;
    }

    private class Couple {
        public final DoubleProperty pt1x;
        public final DoubleProperty pt1y;
        public final DoubleProperty pt2x;
        public final DoubleProperty pt2y;
        public final boolean exists;

        public Couple(final Indi indi1, final Indi indi2) {
            exists = !(indi1 == null && indi2 == null);

            if (!exists) {
                // don't create two phantom parents
                pt1x = pt1y = pt2x = pt2y = null;
            } else if (indi1 == null) {
                pt2x = indi2.x();
                pt2y = indi2.y();
                final Circle phantom = createPhantom();
                pt1x = phantom.layoutXProperty();
                pt1x.bind(pt2x.subtract(marrSpacing()));
                pt1y = phantom.layoutYProperty();
                pt1y.bind(pt2y);
            } else if (indi2 == null) {
                pt1x = indi1.x();
                pt1y = indi1.y();
                final Circle phantom = createPhantom();
                pt2x = phantom.layoutXProperty();
                pt2x.bind(pt1x.add(marrSpacing()));
                pt2y = phantom.layoutYProperty();
                pt2y.bind(pt1y);
            } else {
                pt1x = indi1.x();
                pt1y = indi1.y();
                pt2x = indi2.x();
                pt2y = indi2.y();
            }
        }

        private Circle createPhantom() {
            final Circle phantom = new Circle(0D, Color.TRANSPARENT);

            final Text textshape = new Text();
            textshape.setFill(metrics.colorIndiText());
            textshape.setFont(metrics.getFont());
            textshape.setTextAlignment(TextAlignment.CENTER);
            textshape.setText("\u00A0?\u00A0");
            new Scene(new Group(textshape));
            textshape.applyCss();
            final double inset = metrics.getFontSize() / 2.0D;
            final double w = textshape.getLayoutBounds().getWidth() + inset * 2.0D;
            final double h = textshape.getLayoutBounds().getHeight() + inset * 2.0D;

            final StackPane plaque = new StackPane();
            phantomPanes.add(plaque);
            plaque.setBackground(new Background(new BackgroundFill(metrics.colorIndiBg(), CORNERS, Insets.EMPTY)));
            plaque.setBorder(new Border(new BorderStroke(metrics.colorIndiBorder(), BorderStrokeStyle.SOLID, CORNERS, BorderWidths.DEFAULT)));
            StackPane.setMargin(textshape, new Insets(inset));
            plaque.getChildren().addAll(textshape);

            plaque.layoutXProperty().bind(phantom.layoutXProperty().subtract(w / 2.0D));
            plaque.layoutYProperty().bind(phantom.layoutYProperty().subtract(h / 2.0D));

            return phantom;
        }
    }

    public void savePdf(PdfBuilder builder) {
        builder.addLine(this.parentBar1);
        builder.addLine(this.parentBar2);
        builder.addLine(this.descentBar1);
        builder.addLine(this.descentBar2);
        builder.addLine(this.descentBar3);
        builder.addLine(this.childBar);
        if (!this.rChild.isEmpty()) {
            Arrays.asList(this.rChildBar).forEach(builder::addLine);
        }
        this.phantomPanes.stream().map(Node::getBoundsInParent).forEach(builder::addPhantom);
    }

    public void saveSvg(SvgBuilder svg) {
        svg.addLine(this.parentBar1);
        svg.addLine(this.parentBar2);
        svg.addLine(this.descentBar1);
        svg.addLine(this.descentBar2);
        svg.addLine(this.descentBar3);
        svg.addLine(this.childBar);
        if (!this.rChild.isEmpty()) {
            Arrays.asList(this.rChildBar).forEach(svg::addLine);
        }
    }

    public void addGraphicsTo(List<Node> addto) {
        addGraphic(addto, this.parentBar1);
        addGraphic(addto, this.parentBar2);
        addGraphic(addto, this.descentBar1);
        addGraphic(addto, this.descentBar2);
        addGraphic(addto, this.descentBar3);
        addGraphic(addto, this.childBar);
        if (!this.rChild.isEmpty()) {
            Arrays.asList(this.rChildBar).forEach(c -> addGraphic(addto, c));
        }
        addto.addAll(this.phantomPanes);
    }

    private static void addGraphic(List<Node> addto, Line p) {
        if (Objects.nonNull(p)) {
            addto.add(p);
        }
    }
}
