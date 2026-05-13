package nu.mine.mosher.gedcom.xy.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestGrid {
    @Test
    void snapOffsetEqual() {
        final var uut = gridWithOffset(2);
        assertEquals(27.0D, uut.snap(27.0D));
    }

    @Test
    void snapOffsetLow() {
        final var uut = gridWithOffset(2);
        assertEquals(27.0D, uut.snap(25.0D));
    }

    @Test
    void snapOffsetHigh() {
        final var uut = gridWithOffset(2);
        assertEquals(27.0D, uut.snap(29.0D));
    }

    private static Grid gridWithOffset(final int offset) {
        final var uut = Grid.createDefault();
        uut.setOffset(offset);
        return uut;
    }
}
