package org.futureaspects.mojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LogNumberRangeTest {

    @ParameterizedTest
    @ValueSource(strings = { "20_000,30_000", "20.000,30.000", "20000,30000", " 20_000 , 30_000 " })
    void parsesSupportedFormats(String value) {
        assertEquals(new LogNumberRange(20_000, 30_000), LogNumberRange.parse(value));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "20000", "1,2,3", "a,b", "-1,5", "5,5", "9,1" })
    void rejectsInvalidValues(String value) {
        assertThrows(IllegalArgumentException.class, () -> LogNumberRange.parse(value));
    }

    @Test
    void endIsExclusive() {
        LogNumberRange range = new LogNumberRange(10, 20);
        assertTrue(range.contains(10));
        assertTrue(range.contains(19));
        assertFalse(range.contains(20));
        assertFalse(range.contains(9));
    }

    @Test
    void detectsOverlaps() {
        LogNumberRange range = new LogNumberRange(10, 20);
        assertTrue(range.overlaps(new LogNumberRange(19, 30)));
        assertFalse(range.overlaps(new LogNumberRange(20, 30)));
    }
}
