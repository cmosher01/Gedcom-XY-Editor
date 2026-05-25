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

import javafx.beans.binding.DoubleBinding;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;

import java.util.List;

public class OtherChartGraphics {
    private static final double PADDING = 200.0D;

    private final Rectangle border = new Rectangle();
    private final Line axisXPos = new Line();
    private final Line axisXNeg = new Line();
    private final Line axisYPos = new Line();
    private final Line axisYNeg = new Line();



    public void calc(final List<Indi> indis, final ColorScheme colors) {
        this.border.setStroke(colors.lines());
        this.border.setFill(colors.bg());
        this.axisXPos.setStroke(Color.LIGHTGRAY);
        this.axisXPos.getStrokeDashArray().addAll(2D, 8D);
        this.axisXNeg.setStroke(Color.LIGHTGRAY);
        this.axisXNeg.getStrokeDashArray().addAll(2D, 8D);
        this.axisYPos.setStroke(Color.LIGHTGRAY);
        this.axisYPos.getStrokeDashArray().addAll(2D, 8D);
        this.axisYNeg.setStroke(Color.LIGHTGRAY);
        this.axisYNeg.getStrokeDashArray().addAll(2D, 8D);


        final var minX = new DoubleBinding() {
            {
                indis.forEach(i -> super.bind(i.x()));
            }

            @Override
            protected double computeValue() {
                return indis.stream().mapToDouble(i -> i.x().get()).min().getAsDouble();
            }
        };

        final var minY = new DoubleBinding() {
            {
                indis.forEach(i -> super.bind(i.y()));
            }

            @Override
            protected double computeValue() {
                return indis.stream().mapToDouble(i -> i.y().get()).min().getAsDouble();
            }
        };

        final var maxX = new DoubleBinding() {
            {
                indis.forEach(i -> super.bind(i.x()));
            }

            @Override
            protected double computeValue() {
                return indis.stream().mapToDouble(i -> i.x().get()).max().getAsDouble();
            }
        };

        final var maxY = new DoubleBinding() {
            {
                indis.forEach(i -> super.bind(i.y()));
            }
            @Override
            protected double computeValue() {
                return indis.stream().mapToDouble(i -> i.y().get()).max().getAsDouble();
            }
        };



        this.border.xProperty().bind(minX.subtract(PADDING));
        this.border.yProperty().bind(minY.subtract(PADDING));
        this.border.widthProperty().bind(maxX.subtract(minX).add(2*PADDING));
        this.border.heightProperty().bind(maxY.subtract(minY).add(2*PADDING));



        this.axisXPos.setStartX(0D);
        this.axisXPos.endXProperty().bind(maxX.add(PADDING));
        this.axisXPos.setStartY(0D);
        this.axisXPos.setEndY(0D);
        this.axisXNeg.setStartX(0D);
        this.axisXNeg.endXProperty().bind(minX.subtract(PADDING));
        this.axisXNeg.setStartY(0D);
        this.axisXNeg.setEndY(0D);

        this.axisYPos.setStartY(0D);
        this.axisYPos.endYProperty().bind(maxY.add(PADDING));
        this.axisYPos.setStartX(0D);
        this.axisYPos.setEndX(0D);
        this.axisYNeg.setStartY(0D);
        this.axisYNeg.endYProperty().bind(minY.subtract(PADDING));
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
