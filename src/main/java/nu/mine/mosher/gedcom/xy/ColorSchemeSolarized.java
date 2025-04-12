package nu.mine.mosher.gedcom.xy;

import javafx.scene.paint.Color;
import nu.mine.mosher.gedcom.xy.util.Solarized;

public class ColorSchemeSolarized implements ColorScheme {
    @Override
    public boolean bold() { return false; }

    @Override
    public Color bg() {
        return Color.TRANSPARENT;
    }

    @Override
    public Color lines() {
        return Solarized.YELLOW;
    }

    @Override
    public Color indiBg() {
        return Solarized.BASE3.deriveColor(1.0D, 1.0D, 1.0D, 0.75D);
    }

    @Override
    public Color indiText() {
        return Solarized.BASE00;
    }

    @Override
    public Color indiBorder() {
        return Solarized.GREEN;
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
