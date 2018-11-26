package nu.mine.mosher.gedcom.xy;

import javafx.beans.binding.DoubleBinding;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static nu.mine.mosher.gedcom.xy.Indi.CORNERS;

public class Fami {
    public static final double MIN_DISTANCE = 1.51D;


    private Metrics metrics;

    private Indi husb;
    private Indi wife;
    private List<Indi> rChild = new ArrayList<>();

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

    public void setHusb(Indi indi) {
        husb = indi;
    }

    public Optional<Indi> getHusb() {
        return Optional.ofNullable(this.husb);
    }

    public void setWife(Indi indi) {
        wife = indi;
    }

    public Optional<Indi> getWife() {
        return Optional.ofNullable(this.wife);
    }

    public void addChild(Indi indi) {
        rChild.add(indi);
    }

    public List<Indi> getChildren() {
        return Collections.unmodifiableList(this.rChild);
    }

    public void calc() {
        if (husb == null && wife == null && rChild.size() == 0) {
            return;
        }

        final Couple couple = new Couple(husb, wife);

        if (couple.exists()) {
            parentBar1 = createLine();
            parentBar1.startXProperty().bind(couple.getPt1().layoutXProperty());
            parentBar1.startYProperty().bind(couple.getPt1().layoutYProperty().subtract(barHeight()));
            parentBar1.endXProperty().bind(couple.getPt2().layoutXProperty());
            parentBar1.endYProperty().bind(couple.getPt2().layoutYProperty().subtract(barHeight()));

            parentBar2 = createLine();
            parentBar2.startXProperty().bind(couple.getPt1().layoutXProperty());
            parentBar2.startYProperty().bind(couple.getPt1().layoutYProperty().add(barHeight()));
            parentBar2.endXProperty().bind(couple.getPt2().layoutXProperty());
            parentBar2.endYProperty().bind(couple.getPt2().layoutYProperty().add(barHeight()));
        }

        if (!rChild.isEmpty()) {
            childBar = createLine();
            childBar.startXProperty().bind(new DoubleBinding() {
                {
                    for (final Indi child : rChild) {
                        super.bind(child.getCircle().layoutXProperty());
                    }
                }

                @Override
                protected double computeValue() {
                    return rChild.stream().mapToDouble(c -> c.getCircle().getLayoutX()).min().getAsDouble();
                }
            });
            childBar.endXProperty().bind(new DoubleBinding() {
                {
                    for (final Indi child : rChild) {
                        super.bind(child.getCircle().layoutXProperty());
                    }
                }

                @Override
                protected double computeValue() {
                    return rChild.stream().mapToDouble(c -> c.getCircle().getLayoutX()).max().getAsDouble();
                }
            });
            final DoubleBinding top = new DoubleBinding() {
                {
                    for (final Indi child : rChild) {
                        super.bind(child.getCircle().layoutYProperty());
                    }
                }

                @Override
                protected double computeValue() {
                    return rChild.stream().mapToDouble(c -> c.getCircle().getLayoutY()).min().getAsDouble() - childHeight();
                }
            };
            childBar.startYProperty().bind(top);
            childBar.endYProperty().bind(top);

            rChildBar = new Line[rChild.size()];
            for (int i = 0; i < rChildBar.length; i++) {
                final Circle c = rChild.get(i).getCircle();
                rChildBar[i] = createLine();

                rChildBar[i].startXProperty().bind(c.layoutXProperty());
                rChildBar[i].startYProperty().bind(childBar.startYProperty());
                rChildBar[i].endXProperty().bind(c.layoutXProperty());
                rChildBar[i].endYProperty().bind(c.layoutYProperty());
            }








            if (couple.exists()) {
                final DoubleBinding descentLineEndParentX = new DoubleBinding() {
                    {
                        super.bind(
                                couple.pt1.layoutXProperty(),
                                couple.pt1.layoutYProperty(),
                                couple.pt2.layoutXProperty(),
                                couple.pt2.layoutYProperty(),
                                childBar.startXProperty(),
                                childBar.endXProperty(),
                                childBar.startYProperty());
                    }

                    @Override
                    protected double computeValue() {
                        final Point2D child = new Point2D((childBar.getStartX() + childBar.getEndX()) / 2.0D, childBar.getStartY());
                        final Point2D p1 = new Point2D(couple.pt1.getLayoutX(), couple.pt1.getLayoutY());
                        final Point2D p2 = new Point2D(couple.pt2.getLayoutX(), couple.pt2.getLayoutY());
                        final Double d1 = child.distance(p1);
                        final Double d2 = child.distance(p2);
                        if (d1 < d2) {
                            return marpt(p2, p1).getX();
                        }
                        return marpt(p1, p2).getX();
                    }
                };

                final DoubleBinding descentLineEndParentY = new DoubleBinding() {
                    {
                        super.bind(
                                couple.pt1.layoutXProperty(),
                                couple.pt1.layoutYProperty(),
                                couple.pt2.layoutXProperty(),
                                couple.pt2.layoutYProperty(),
                                childBar.startXProperty(),
                                childBar.endXProperty(),
                                childBar.startYProperty());
                    }

                    @Override
                    protected double computeValue() {
                        final Point2D child = new Point2D((childBar.getStartX() + childBar.getEndX()) / 2.0D, childBar.getStartY());
                        final Point2D p1 = new Point2D(couple.pt1.getLayoutX(), couple.pt1.getLayoutY());
                        final Point2D p2 = new Point2D(couple.pt2.getLayoutX(), couple.pt2.getLayoutY());
                        final Double d1 = child.distance(p1);
                        final Double d2 = child.distance(p2);
                        if (d1 < d2) {
                            return marpt(p2, p1).getY();
                        }
                        return marpt(p1, p2).getY();
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
        return husb.getCoords().distance(wife.getCoords());
    }

    public double getGenDistance() {
        if (husb == null || wife == null) {
            return 0D;
        }
        final double avgChildX = this.rChild.stream().mapToDouble(c -> c.getCoords().getX()).average().orElse(0D);
        final double avgChildY = this.rChild.stream().mapToDouble(c -> c.getCoords().getY()).average().orElse(0D);
        if (avgChildX < 1D && avgChildY < 1D) {
            return 0D;
        }
        final Point2D avgChild = new Point2D(avgChildX, avgChildY);
        if (husb == null) {
            return wife.getCoords().distance(avgChild);
        }
        if (wife == null) {
            return husb.getCoords().distance(avgChild);
        }
        return Math.min(husb.getCoords().distance(avgChild), wife.getCoords().distance(avgChild));
    }

    private Point2D marpt(Point2D p1, Point2D p2) {
        final Point2D pstart = new Point2D(p2.getX(), p2.getY() + barHeight());
        final Point2D pend = new Point2D(p1.getX(), p1.getY() + barHeight());
        final double pd = Math.max(pstart.distance(pend), MIN_DISTANCE);
        final double dt = Math.min(descentLineDistance(), pd/2.0D)/ pd;
        return new Point2D((1 - dt) * pstart.getX() + dt * pend.getX(), (1 - dt) * pstart.getY() + dt * pend.getY());
    }

    private double barHeight() {
        return this.metrics.getFontSize() / 2.0D;
    }

    private double childHeight() {
        return this.metrics.getFontSize() * 4.0D;
    }

    private double marrSpacing() {
        return this.metrics.getMarrDistance();
    }

    private double descentLineDistance() {
        return this.metrics.getMarrDistance() / 2.0D;
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
        private Circle pt1;
        private Circle pt2;
        private final boolean exists;

        public Circle getPt1() {
            return pt1;
        }

        public Circle getPt2() {
            return pt2;
        }

        public boolean exists() {
            return this.exists;
        }

        public Couple(final Indi husb, final Indi wife) {
            exists = !(husb == null && wife == null);
            if (husb == null && wife == null) {
                pt1 = createPhantom();
                pt2 = createPhantom();
            } else if (husb == null) {
                pt2 = wife.getCircle();
                pt1 = createPhantom();
                pt1.layoutYProperty().bind(pt2.layoutYProperty());
                pt1.layoutXProperty().bind(pt2.layoutXProperty().subtract(marrSpacing()));
            } else if (wife == null) {
                pt1 = husb.getCircle();
                pt2 = createPhantom();
                pt2.layoutYProperty().bind(pt1.layoutYProperty());
                pt2.layoutXProperty().bind(pt1.layoutXProperty().add(marrSpacing()));
            } else {
                pt1 = husb.getCircle();
                pt2 = wife.getCircle();
            }
        }

        private Circle createPhantom() {
            final Circle phantom = new Circle(0D, Color.TRANSPARENT);

            final Text textshape = new Text();
            textshape.setFill(metrics.colorIndiText());
            textshape.setFont(Fami.this.metrics.getFont());
            textshape.setTextAlignment(TextAlignment.CENTER);
            textshape.setText("\u00A0?\u00A0");
            new Scene(new Group(textshape));
            textshape.applyCss();
            final double inset = Fami.this.metrics.getFontSize() / 2.0D;
            final double w = textshape.getLayoutBounds().getWidth() + inset * 2.0D;
            final double h = textshape.getLayoutBounds().getHeight() + inset * 2.0D;

            final StackPane plaque = new StackPane();
            Fami.this.phantomPanes.add(plaque);
            plaque.setBackground(new Background(new BackgroundFill(metrics.colorIndiBg(), CORNERS, Insets.EMPTY)));
            plaque.setBorder(new Border(new BorderStroke(metrics.colorIndiBorder(), BorderStrokeStyle.SOLID, CORNERS, BorderWidths.DEFAULT)));
            StackPane.setMargin(textshape, new Insets(inset));
            plaque.getChildren().addAll(textshape);

            plaque.layoutXProperty().bind(phantom.layoutXProperty().subtract(w / 2.0D));
            plaque.layoutYProperty().bind(phantom.layoutYProperty().subtract(h / 2.0D));

            return phantom;
        }
    }

    public void addGraphicsTo(List<Node> addto) {
        addGraphic(addto, this.parentBar1);
        addGraphic(addto, this.parentBar2);
        addGraphic(addto, this.descentBar1);
        addGraphic(addto, this.descentBar2);
        addGraphic(addto, this.descentBar3);
        addGraphic(addto, this.childBar);
        if (!rChild.isEmpty()) {
            Arrays.asList(this.rChildBar).forEach(c -> addGraphic(addto, c));
        }
        addto.addAll(this.phantomPanes);
    }

    private void addGraphic(List<Node> addto, Line p) {
        if (Objects.nonNull(p)) {
            addto.add(p);
        }
    }
}
