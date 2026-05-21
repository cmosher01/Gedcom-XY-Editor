package nu.mine.mosher.gedcom.xy;

import javafx.beans.binding.DoubleBinding;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;

import java.util.List;

public class OtherChartGraphics implements ChartBoundary {
    private Rectangle border = new Rectangle();
    private Line axisX = new Line();
    private Line axisY = new Line();
    private DoubleBinding bindingMinX;
    private DoubleBinding bindingMinY;
    private DoubleBinding bindingMaxX;
    private DoubleBinding bindingMaxY;



    @Override
    public double minX() {
        return bindingMinX.get();
    }

    @Override
    public double minY() {
        return bindingMinY.get();
    }

    @Override
    public double maxX() {
        return bindingMaxX.get();
    }

    @Override
    public double maxY() {
        return bindingMaxY.get();
    }



    public void calc(final List<Indi> indis, final ColorScheme colors) {
        this.border.setStroke(colors.lines());
        this.border.setFill(colors.bg());
        this.axisX.setStroke(Color.LIGHTGRAY);
// TODO        this.axisX.getStrokeDashArray().addAll(2D, 8D);
//        this.axisX.setStrokeDashOffset(1-(canvas.left%10+10)%10); ???bind to minx???
//        this.axisX.setStrokeLineCap(StrokeLineCap.ROUND);
        this.axisY.setStroke(Color.LIGHTGRAY);
// TODO       this.axisY.getStrokeDashArray().addAll(2D, 8D);
//        this.axisY.setStrokeLineCap(StrokeLineCap.ROUND);
//        this.axisY.setStrokeDashOffset();



        this.bindingMinX = new DoubleBinding() {
            {
                indis.forEach(i -> super.bind(i.x()));
            }
            @Override
            protected double computeValue() {
                return indis.stream().mapToDouble(i -> i.x().get()).min().getAsDouble();
            }
        };

        this.bindingMinY = new DoubleBinding() {
            {
                indis.forEach(i -> super.bind(i.y()));
            }
            @Override
            protected double computeValue() {
                return indis.stream().mapToDouble(i -> i.y().get()).min().getAsDouble();
            }
        };

        this.bindingMaxX = new DoubleBinding() {
            {
                indis.forEach(i -> super.bind(i.x()));
            }
            @Override
            protected double computeValue() {
                return indis.stream().mapToDouble(i -> i.x().get()).max().getAsDouble();
            }
        };

        this.bindingMaxY = new DoubleBinding() {
            {
                indis.forEach(i -> super.bind(i.y()));
            }
            @Override
            protected double computeValue() {
                return indis.stream().mapToDouble(i -> i.y().get()).max().getAsDouble();
            }
        };



        this.border.xProperty().bind(bindingMinX.subtract(PADDING));
        this.border.yProperty().bind(bindingMinY.subtract(PADDING));
        this.border.widthProperty().bind(bindingMaxX.subtract(bindingMinX).add(2*PADDING));
        this.border.heightProperty().bind(bindingMaxY.subtract(bindingMinY).add(2*PADDING));



        this.axisX.startXProperty().bind(bindingMinX.subtract(PADDING));
        this.axisX.endXProperty().bind(bindingMaxX.add(PADDING));
        this.axisX.setStartY(0D);
        this.axisX.setEndY(0D);

        this.axisY.startYProperty().bind(bindingMinY.subtract(PADDING));
        this.axisY.endYProperty().bind(bindingMaxY.add(PADDING));
        this.axisY.setStartX(0D);
        this.axisY.setEndX(0D);
    }

    public void addGraphicsTo(List<Node> addto) {
        addto.add(this.border);
        addto.add(this.axisX);
        addto.add(this.axisY);
    }
}
