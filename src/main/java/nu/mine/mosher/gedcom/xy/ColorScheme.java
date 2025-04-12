package nu.mine.mosher.gedcom.xy;

import javafx.scene.paint.Color;

public interface ColorScheme {
    boolean bold();

    Color bg();
    Color lines();

    Color indiBg();
    Color indiText();
    Color indiBorder();
    Color indiBorderDirty();
    Color indiSelBg();
    Color indiSelText();

    Color selector();
}
