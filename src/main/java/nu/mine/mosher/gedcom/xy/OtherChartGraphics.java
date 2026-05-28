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

package nu.mine.mosher.gedcom.xy;

import javafx.beans.binding.*;
import javafx.beans.property.DoubleProperty;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;

import java.util.List;
import java.util.concurrent.Callable;

public class OtherChartGraphics {
    private final Rectangle border = new Rectangle();
    private final Line axisXPos = new Line();
    private final Line axisXNeg = new Line();
    private final Line axisYPos = new Line();
    private final Line axisYNeg = new Line();
    private double margin;



    public void calc(final List<Indi> indis, final Metrics metrics) {
        this.margin = metrics.margin();
        this.border.setStroke(metrics.colors().lines());
        this.border.setFill(metrics.colors().bg());
        this.axisXPos.setStroke(Color.LIGHTGRAY);
        this.axisXPos.getStrokeDashArray().addAll(2D, 8D);
        this.axisXNeg.setStroke(Color.LIGHTGRAY);
        this.axisXNeg.getStrokeDashArray().addAll(2D, 8D);
        this.axisYPos.setStroke(Color.LIGHTGRAY);
        this.axisYPos.getStrokeDashArray().addAll(2D, 8D);
        this.axisYNeg.setStroke(Color.LIGHTGRAY);
        this.axisYNeg.getStrokeDashArray().addAll(2D, 8D);





        final var obsIndisX = indis.stream().map(Indi::x).toList().toArray(new DoubleProperty[0]);
        final Callable<Double> clMinX = () -> indis.stream().mapToDouble(i -> i.x().get()).min().getAsDouble();
        final var minX = Bindings.createDoubleBinding(clMinX, obsIndisX);
        final Callable<Double> clMaxX = () -> indis.stream().mapToDouble(i -> i.x().get()).max().getAsDouble();
        final var maxX = Bindings.createDoubleBinding(clMaxX, obsIndisX);

        final var obsIndisY = indis.stream().map(Indi::y).toList().toArray(new DoubleProperty[0]);
        final Callable<Double> clMinY = () -> indis.stream().mapToDouble(i -> i.y().get()).min().getAsDouble();
        final var minY = Bindings.createDoubleBinding(clMinY, obsIndisY);
        final Callable<Double> clMaxY = () -> indis.stream().mapToDouble(i -> i.y().get()).max().getAsDouble();
        final var maxY = Bindings.createDoubleBinding(clMaxY, obsIndisY);

        this.border.xProperty().bind(minX.subtract(margin));
        this.border.yProperty().bind(minY.subtract(margin));
        this.border.widthProperty().bind(maxX.subtract(minX).add(2* margin));
        this.border.heightProperty().bind(maxY.subtract(minY).add(2* margin));



        this.axisXPos.setStartX(0D);
        this.axisXPos.endXProperty().bind(maxX.add(margin));
        this.axisXPos.setStartY(0D);
        this.axisXPos.setEndY(0D);
        this.axisXNeg.setStartX(0D);
        this.axisXNeg.endXProperty().bind(minX.subtract(margin));
        this.axisXNeg.setStartY(0D);
        this.axisXNeg.setEndY(0D);



        this.axisYPos.setStartY(0D);
        this.axisYPos.endYProperty().bind(maxY.add(margin));
        this.axisYPos.setStartX(0D);
        this.axisYPos.setEndX(0D);
        this.axisYNeg.setStartY(0D);
        this.axisYNeg.endYProperty().bind(minY.subtract(margin));
        this.axisYNeg.setStartX(0D);
        this.axisYNeg.setEndX(0D);
    }

    public void addGraphicsTo(final List<Node> addto) {
        addto.add(this.border);
        addto.add(this.axisXPos);
        addto.add(this.axisXNeg);
        addto.add(this.axisYPos);
        addto.add(this.axisYNeg);
    }
}
