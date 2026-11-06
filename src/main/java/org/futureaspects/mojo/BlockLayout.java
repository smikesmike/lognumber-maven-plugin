package org.futureaspects.mojo;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeSet;

/**
 * Hands out new lognumbers. A file only receives numbers from blocks nobody else uses.
 */
final class BlockLayout {

    private final LogNumberRange range;
    private final int blockSize;
    private final Set<Integer> usedNumbers;
    private final Set<Integer> usedBlocks;
    private final Map<Path, TreeSet<Integer>> ownBlocks;
    private final Map<Path, Integer> overflowNumbers = new HashMap<>();
    private int nextBlock;
    private int claimedBlocks;
    private int missingBlocks;

    private BlockLayout(LogNumberRange range, int blockSize, Set<Integer> usedNumbers, Set<Integer> usedBlocks,
            Map<Path, TreeSet<Integer>> ownBlocks) {
        this.range = range;
        this.blockSize = blockSize;
        this.usedNumbers = usedNumbers;
        this.usedBlocks = usedBlocks;
        this.ownBlocks = ownBlocks;
        this.nextBlock = range.start() / blockSize;
    }

    /**
     * Creates the layout of the numbers which stay in place.
     *
     * @param range     the range for new numbers
     * @param blockSize the numbers per block
     * @param retained  the numbered statements which are not going to change
     * @return the layout
     */
    static BlockLayout of(LogNumberRange range, int blockSize, List<LogStatement> retained) {
        Set<Integer> usedNumbers = new HashSet<>();
        Map<Integer, Set<Path>> filesByBlock = new HashMap<>();
        for (LogStatement statement : retained) {
            usedNumbers.add(statement.number());
            filesByBlock.computeIfAbsent(statement.number() / blockSize, b -> new HashSet<>()).add(statement.file());
        }

        Map<Path, TreeSet<Integer>> ownBlocks = new HashMap<>();
        filesByBlock.forEach((block, files) -> {
            if (files.size() == 1) {
                ownBlocks.computeIfAbsent(files.iterator().next(), f -> new TreeSet<>()).add(block);
            }
        });
        return new BlockLayout(range, blockSize, usedNumbers, new HashSet<>(filesByBlock.keySet()), ownBlocks);
    }

    /**
     * Reserves the next number for a file.
     *
     * @param file the file
     * @return the number, empty if the range is exhausted
     */
    OptionalInt allocate(Path file) {
        for (int block : ownBlocks.computeIfAbsent(file, f -> new TreeSet<>())) {
            OptionalInt number = takeNumber(block);
            if (number.isPresent()) {
                return number;
            }
        }
        for (int lastBlock = (range.end() - 1) / blockSize; nextBlock <= lastBlock; nextBlock++) {
            if (usedBlocks.add(nextBlock)) {
                claimedBlocks++;
                ownBlocks.get(file).add(nextBlock);
                OptionalInt number = takeNumber(nextBlock);
                if (number.isPresent()) {
                    return number;
                }
            }
        }
        countMissingNumber(file);
        return OptionalInt.empty();
    }

    private OptionalInt takeNumber(int block) {
        for (int number = block * blockSize; number < (block + 1) * blockSize; number++) {
            if (number > 0 && range.contains(number) && usedNumbers.add(number)) {
                return OptionalInt.of(number);
            }
        }
        return OptionalInt.empty();
    }

    private void countMissingNumber(Path file) {
        int left = overflowNumbers.getOrDefault(file, 0);
        if (left == 0) {
            missingBlocks++;
            left = blockSize;
        }
        overflowNumbers.put(file, left - 1);
    }

    /** Returns how many free blocks were available and have been used. */
    int claimedBlocks() {
        return claimedBlocks;
    }

    /** Returns how many additional blocks would have been needed. */
    int missingBlocks() {
        return missingBlocks;
    }
}
