package org.futureaspects.mojo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Analyses the numbers of a group: duplicates, numbers outside the range and blocks shared by several files.
 */
final class NumberAudit {

    /** Kind of a violation. */
    enum ViolationType {
        DUPLICATE, OUT_OF_RANGE
    }

    /**
     * A statement breaking the numbering rules.
     *
     * @param type      what is wrong
     * @param statement the statement
     */
    record Violation(ViolationType type, LogStatement statement) {
    }

    private final int blockSize;
    private final Map<Integer, List<LogStatement>> duplicates;
    private final List<LogStatement> outOfRange;
    private final Map<Integer, Set<Path>> sharedBlocks;
    private final List<LogStatement> numberedInRange;
    private final Map<String, Integer> numbersPerFileAndBlock;

    private NumberAudit(int blockSize, Map<Integer, List<LogStatement>> duplicates, List<LogStatement> outOfRange,
            Map<Integer, Set<Path>> sharedBlocks, List<LogStatement> numberedInRange,
            Map<String, Integer> numbersPerFileAndBlock) {
        this.blockSize = blockSize;
        this.duplicates = duplicates;
        this.outOfRange = outOfRange;
        this.sharedBlocks = sharedBlocks;
        this.numberedInRange = numberedInRange;
        this.numbersPerFileAndBlock = numbersPerFileAndBlock;
    }

    /**
     * Analyses a group.
     *
     * @param group     the group, which needs a range
     * @param blockSize the numbers per block
     * @return the result
     */
    static NumberAudit of(ScanGroup group, int blockSize) {
        Map<Integer, List<LogStatement>> byNumber = new TreeMap<>();
        List<LogStatement> outOfRange = new ArrayList<>();
        List<LogStatement> numberedInRange = new ArrayList<>();
        Map<Integer, Set<Path>> filesByBlock = new TreeMap<>();
        Map<String, Integer> numbersPerFileAndBlock = new HashMap<>();

        group.statements().filter(LogStatement::isNumbered).forEach(statement -> {
            byNumber.computeIfAbsent(statement.number(), n -> new ArrayList<>()).add(statement);
            if (group.range().contains(statement.number())) {
                int block = statement.number() / blockSize;
                numberedInRange.add(statement);
                filesByBlock.computeIfAbsent(block, b -> new TreeSet<>()).add(statement.file());
                numbersPerFileAndBlock.merge(key(statement.file(), block), 1, Integer::sum);
            } else {
                outOfRange.add(statement);
            }
        });
        byNumber.values().removeIf(statements -> statements.size() < 2);
        filesByBlock.values().removeIf(files -> files.size() < 2);
        return new NumberAudit(blockSize, byNumber, outOfRange, filesByBlock, numberedInRange,
                numbersPerFileAndBlock);
    }

    private static String key(Path file, int block) {
        return block + ":" + file;
    }

    /** Returns duplicates and out-of-range numbers in a stable order. */
    List<Violation> violations() {
        List<Violation> violations = new ArrayList<>();
        duplicates.values().forEach(statements -> statements
                .forEach(statement -> violations.add(new Violation(ViolationType.DUPLICATE, statement))));
        outOfRange.forEach(statement -> violations.add(new Violation(ViolationType.OUT_OF_RANGE, statement)));
        return violations;
    }

    List<LogStatement> outOfRange() {
        return outOfRange;
    }

    /** Returns the blocks used by more than one file. */
    Map<Integer, Set<Path>> sharedBlocks() {
        return sharedBlocks;
    }

    /**
     * Returns all duplicates except the kept one per number. The occurrence in the file with the most numbers in that
     * block stays, ties are resolved by path and position.
     */
    Set<LogStatement> duplicateLosers() {
        Set<LogStatement> losers = new HashSet<>();
        for (Map.Entry<Integer, List<LogStatement>> duplicate : duplicates.entrySet()) {
            int block = duplicate.getKey() / blockSize;
            List<LogStatement> ranked = new ArrayList<>(duplicate.getValue());
            ranked.sort(Comparator.comparingInt((LogStatement s) -> -numbersIn(s.file(), block))
                    .thenComparing(LogStatement::file)
                    .thenComparingInt(LogStatement::start));
            losers.addAll(ranked.subList(1, ranked.size()));
        }
        return losers;
    }

    /** Returns the statements of all files which do not own the shared block they are in. */
    Set<LogStatement> sharedBlockLosers() {
        Map<Integer, Path> owners = new HashMap<>();
        sharedBlocks.forEach((block, files) -> owners.put(block, ownerOf(block, files)));
        Set<LogStatement> losers = new HashSet<>();
        for (LogStatement statement : numberedInRange) {
            Path owner = owners.get(statement.number() / blockSize);
            if (owner != null && !owner.equals(statement.file())) {
                losers.add(statement);
            }
        }
        return losers;
    }

    private Path ownerOf(int block, Set<Path> sortedFiles) {
        Path owner = null;
        int most = 0;
        for (Path file : sortedFiles) {
            if (numbersIn(file, block) > most) {
                most = numbersIn(file, block);
                owner = file;
            }
        }
        return owner;
    }

    private int numbersIn(Path file, int block) {
        return numbersPerFileAndBlock.getOrDefault(key(file, block), 0);
    }
}
