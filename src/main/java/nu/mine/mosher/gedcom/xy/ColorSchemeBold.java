package nu.mine.mosher.gedcom.xy;

import javafx.scene.paint.Color;
import nu.mine.mosher.gedcom.xy.util.Solarized;

public class ColorSchemeBold implements ColorScheme {
    @Override
    public boolean bold() { return true; }

    @Override
    public Color bg() {
        return Color.BEIGE;
    }

    @Override
    public Color lines() {
        return Color.BLACK;
    }

    @Override
    public Color indiBg() {
        return Color.WHITE;
    }

    @Override
    public Color indiText() {
        return Color.BLACK;
    }

    @Override
    public Color indiBorder() {
        return Color.DARKGRAY;
    }

    @Override
    public Color indiBorderDirty() {
        return Solarized.BLUE;
    }

    @Override
    public Color indiSelBg() {
        return Solarized.BASE2;
    }

    @Override
    public Color indiSelText() {
        return Solarized.MAGENTA;
    }

    @Override
    public Color selector() {
        return Solarized.MAGENTA;
    }
}
