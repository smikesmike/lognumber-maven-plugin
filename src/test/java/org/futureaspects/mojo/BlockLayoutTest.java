package org.futureaspects.mojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.futureaspects.mojo.LogStatement.State;
import org.junit.jupiter.api.Test;

class BlockLayoutTest {

    private static final Path A = Path.of("A.java");
    private static final Path B = Path.of("B.java");
    private static final LogNumberRange RANGE = new LogNumberRange(100, 130);

    @Test
    void usesFreeSlotsOfOwnBlockFirst() {
        BlockLayout layout = BlockLayout.of(RANGE, 10, List.of(numbered(A, 110), numbered(A, 111)));

        assertEquals(112, layout.allocate(A).orElseThrow());
    }

    @Test
    void neverTakesNumbersFromSharedBlocks() {
        BlockLayout layout = BlockLayout.of(RANGE, 10, List.of(numbered(A, 100), numbered(B, 101)));

        assertEquals(110, layout.allocate(A).orElseThrow());
        assertEquals(120, layout.allocate(B).orElseThrow());
    }

    @Test
    void claimsNextBlockWhenOwnBlockIsFull() {
        BlockLayout layout = BlockLayout.of(RANGE, 10, List.of());
        for (int i = 0; i < 10; i++) {
            assertEquals(100 + i, layout.allocate(A).orElseThrow());
        }
        assertEquals(110, layout.allocate(A).orElseThrow());
        assertEquals(2, layout.claimedBlocks());
    }

    @Test
    void numbersOutsideOfTheRangeAreIgnored() {
        BlockLayout layout = BlockLayout.of(RANGE, 10, List.of(numbered(A, 5000)));

        assertEquals(100, layout.allocate(A).orElseThrow());
    }

    @Test
    void respectsBoundsOfUnalignedRange() {
        BlockLayout layout = BlockLayout.of(new LogNumberRange(108, 111), 10, List.of());

        assertEquals(108, layout.allocate(A).orElseThrow());
        assertEquals(109, layout.allocate(A).orElseThrow());
        assertEquals(110, layout.allocate(A).orElseThrow());
        assertTrue(layout.allocate(A).isEmpty());
    }

    @Test
    void neverAssignsZero() {
        BlockLayout layout = BlockLayout.of(new LogNumberRange(0, 10), 10, List.of());

        assertEquals(1, layout.allocate(A).orElseThrow());
    }

    @Test
    void countsMissingBlocks() {
        BlockLayout layout = BlockLayout.of(new LogNumberRange(100, 110), 10, List.of());
        layout.allocate(A);

        assertTrue(layout.allocate(B).isEmpty());
        assertTrue(layout.allocate(B).isEmpty());

        assertEquals(1, layout.claimedBlocks());
        assertEquals(1, layout.missingBlocks());
    }

    private static LogStatement numbered(Path file, int number) {
        return new LogStatement(file, 1, 0, 0, State.NUMBERED, number);
    }
}
