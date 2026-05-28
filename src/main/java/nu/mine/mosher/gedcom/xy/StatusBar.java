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

import javafx.geometry.*;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.text.Font;

import java.util.Objects;

public class StatusBar extends StackPane {
    private final Label statusName = new Label();
    private final Label labelMouseViewport = new Label();
    private final Label labelMouseVpToCv = new Label();
    private final FamilyChart chart;

    public static StatusBar create(final FamilyChart chart) {
        final var ret = new StatusBar(chart);
        ret.init();
        return ret;
    }

    private StatusBar(final FamilyChart chart) {
        this.chart = chart;
    }

    private void init() {
        configureSelectionLabel(this.statusName);
        final var selection = new HBox(this.statusName);

        configureCoordinateLabel(this.labelMouseViewport);
        configureCoordinateLabel(this.labelMouseVpToCv);
        final var coords = new HBox(this.labelMouseViewport, this.labelMouseVpToCv);
        coords.setAlignment(Pos.CENTER_RIGHT);

        final var layout = new AnchorPane(selection, coords);
        AnchorPane.setLeftAnchor(selection, 5D);
        AnchorPane.setRightAnchor(coords, 5D);

        selection.prefWidthProperty().bind(layout.widthProperty().divide(2.0D));
        selection.setMaxWidth(Region.USE_PREF_SIZE);
        selection.setMinWidth(Region.USE_PREF_SIZE);
        coords.prefWidthProperty().bind(layout.widthProperty().divide(2.0D));
        coords.setMaxWidth(Region.USE_PREF_SIZE);
        coords.setMinWidth(Region.USE_PREF_SIZE);

        super.getChildren().add(layout);
    }

    public void updateViewPort(final Point2D coords) {
        this.labelMouseViewport.setText(displayCoords("window", coords));
    }

    public void updateVpToCv(final Point2D coords) {
        this.labelMouseVpToCv.setText(displayCoords("canvas", coords));
    }

    private void configureSelectionLabel(final Label label) {
        label.textProperty().bind(this.chart.selectedName());
        label.setPadding(new Insets(5.0D));
        label.setFont(Font.font(Metrics.FONT_FAMILY_NAME));
    }

    private void configureCoordinateLabel(final Label label) {
        label.setPadding(new Insets(5.0D));
        label.setFont(Font.font(Metrics.FONT_FAMILY_NAME_MONO));
    }

    private static String displayCoords(String name6chars, Point2D coords) {
        return String.format("  %6s=(%7.1f,%7.1f)",
            name6chars,
            Objects.isNull(coords) ? 0D : coords.getX(),
            Objects.isNull(coords) ? 0D : coords.getY());
    }
}
