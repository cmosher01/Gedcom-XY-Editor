package nu.mine.mosher.gedcom.xy;

import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;

import java.util.*;

public class Fami {
    public static final double BAR_HEIGHT = 4.0D;
    public static final double CHILD_HEIGHT = 20.0D;
    public static final double MARRIAGE_SPACING = CHILD_HEIGHT * 5.0D;
    public static final double CHILD_LINE_DISTANCE = MARRIAGE_SPACING / 2.0D + 3.0D;
    public static final double MIN_DISTANCE = 1.51D;


    private Indi husb;
    private Indi wife;
    private List<Indi> rChild = new ArrayList<>();

    private Line parentBar1;
    private Line parentBar2;
    private final List<Circle> phantoms = new ArrayList<>(0);

    private Line descentBar1;
    private Line descentBar2;
    private Line descentBar3;

    private Line childBar;
    private Line[] rChildBar;

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
            parentBar1.startYProperty().bind(couple.getPt1().layoutYProperty().subtract(BAR_HEIGHT));
            parentBar1.endXProperty().bind(couple.getPt2().layoutXProperty());
            parentBar1.endYProperty().bind(couple.getPt2().layoutYProperty().subtract(BAR_HEIGHT));

            parentBar2 = createLine();
            parentBar2.startXProperty().bind(couple.getPt1().layoutXProperty());
            parentBar2.startYProperty().bind(couple.getPt1().layoutYProperty().add(BAR_HEIGHT));
            parentBar2.endXProperty().bind(couple.getPt2().layoutXProperty());
            parentBar2.endYProperty().bind(couple.getPt2().layoutYProperty().add(BAR_HEIGHT));
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
                    return rChild.stream().mapToDouble(c -> c.getCircle().getLayoutY()).min().getAsDouble() - CHILD_HEIGHT;
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
                descentBar3 = createLine();
                final DoubleBinding midpoint = new DoubleBinding() {
                    {
                        super.bind(childBar.startXProperty(), childBar.endXProperty());
                    }

                    @Override
                    protected double computeValue() {
                        return (childBar.getStartX() + childBar.getEndX()) / 2.0D;
                    }
                };
                descentBar3.startXProperty().bind(midpoint);
                descentBar3.startYProperty().bind(childBar.startYProperty());
                descentBar3.endXProperty().bind(midpoint);
                descentBar3.endYProperty().bind(childBar.endYProperty().subtract(CHILD_HEIGHT / 2.0D));

                final DoubleBinding closestParentX = new DoubleBinding() {
                    {
                        super.bind(
                            couple.pt1.layoutXProperty(),
                            couple.pt1.layoutYProperty(),
                            couple.pt2.layoutXProperty(),
                            couple.pt2.layoutYProperty(),
                            descentBar3.startXProperty(),
                            descentBar3.startYProperty());
                    }

                    @Override
                    protected double computeValue() {
                        final Point2D child = new Point2D(descentBar3.getStartX(), descentBar3.getStartY());
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
                final DoubleBinding closestParentY = new DoubleBinding() {
                    {
                        super.bind(
                            couple.pt1.layoutXProperty(),
                            couple.pt1.layoutYProperty(),
                            couple.pt2.layoutXProperty(),
                            couple.pt2.layoutYProperty(),
                            descentBar3.startXProperty(),
                            descentBar3.startYProperty());
                    }

                    @Override
                    protected double computeValue() {
                        final Point2D child = new Point2D(descentBar3.getStartX(), descentBar3.getStartY());
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

                descentBar2 = createLine();
                descentBar2.startXProperty().bind(descentBar3.endXProperty());
                descentBar2.startYProperty().bind(descentBar3.endYProperty());
                descentBar2.endXProperty().bind(closestParentX);
                descentBar2.endYProperty().bind(descentBar3.endYProperty());

                descentBar1 = createLine();
                descentBar1.startXProperty().bind(closestParentX);
                descentBar1.startYProperty().bind(descentBar2.endYProperty());
                descentBar1.endXProperty().bind(closestParentX);
                descentBar1.endYProperty().bind(closestParentY);
            }
        }
    }

    private Point2D marpt(Point2D p1, Point2D p2) {
        final Point2D pstart = new Point2D(p2.getX(), p2.getY() + BAR_HEIGHT);
        final Point2D pend = new Point2D(p1.getX(), p1.getY() + BAR_HEIGHT);
        final double pd = Math.max(pstart.distance(pend), MIN_DISTANCE);
        final double dt = CHILD_LINE_DISTANCE / pd;
        return new Point2D((1 - dt) * pstart.getX() + dt * pend.getX(), (1 - dt) * pstart.getY() + dt * pend.getY());
    }

    private Line createLine() {
        final Line line = new Line();
        line.setStroke(Color.ORANGE);
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
                pt1.layoutXProperty().bind(pt2.layoutXProperty().subtract(MARRIAGE_SPACING));
            } else if (wife == null) {
                pt1 = husb.getCircle();
                pt2 = createPhantom();
                pt2.layoutYProperty().bind(pt1.layoutYProperty());
                pt2.layoutXProperty().bind(pt1.layoutXProperty().add(MARRIAGE_SPACING));
            } else {
                pt1 = husb.getCircle();
                pt2 = wife.getCircle();
            }
        }

        private Circle createPhantom() {
            final Circle phantom = new Circle(10, Color.LIGHTGRAY);
            phantom.setStroke(Color.GREEN);
            phantom.setStrokeWidth(1.2D);
            Fami.this.phantoms.add(phantom);
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
        addto.addAll(this.phantoms);
    }

    private void addGraphic(List<Node> addto, Line p) {
        if (Objects.nonNull(p)) {
            addto.add(p);
        }
    }
}
