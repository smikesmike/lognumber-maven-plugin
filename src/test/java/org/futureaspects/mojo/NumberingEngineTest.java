package org.futureaspects.mojo;

import static org.futureaspects.mojo.TestSources.line;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoFailureException;
import org.futureaspects.mojo.LogStatement.State;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NumberingEngineTest {

    private static final LogNumberRange RANGE = new LogNumberRange(100, 200);

    @TempDir
    Path dir;

    @Test
    void assignKeepsOwnBlockAndLeavesUnprefixedAlone() throws Exception {
        ScanGroup group = group(Map.of("A.java", line("100;") + line("0;") + line(";") + line("")));

        assertEquals(line("100;") + line("101;") + line("102;") + line(""), single(group, placeholders(group)));
    }

    @Test
    void assignCanNumberUnprefixed() throws Exception {
        ScanGroup group = group(Map.of("A.java", line("")));

        assertEquals(line("100;"), single(group, select(group, State.UNPREFIXED)));
    }

    @Test
    void assignCreatesNoDuplicatesInSharedBlocks() throws Exception {
        ScanGroup group = group(Map.of(
                "A.java", line("100;") + line("0;"),
                "B.java", line("101;") + line("0;")));

        List<FileChange> changes = NumberingEngine.renumber(group, placeholders(group), RANGE, 10);

        assertEquals(line("100;") + line("110;"), changes.get(0).content());
        assertEquals(line("101;") + line("120;"), changes.get(1).content());
    }

    @Test
    void secondRunChangesNothing() throws Exception {
        ScanGroup group = group(Map.of("A.java", line("0;")));
        String numbered = single(group, placeholders(group));

        ScanGroup second = group(Map.of("A.java", numbered));

        assertTrue(NumberingEngine.renumber(second, placeholders(second), RANGE, 10).isEmpty());
    }

    @Test
    void fixDuplicatesRenumbersLosersOnly() throws Exception {
        ScanGroup group = group(Map.of("A.java", line("100;") + line("100;")));

        assertEquals(line("100;") + line("101;"),
                single(group, NumberAudit.of(group, 10).duplicateLosers()));
    }

    @Test
    void fixRangeMovesNumbersIntoTheRange() throws Exception {
        ScanGroup group = group(Map.of("A.java", line("5000;") + line("100;")));

        assertEquals(line("101;") + line("100;"),
                single(group, Set.copyOf(NumberAudit.of(group, 10).outOfRange())));
    }

    @Test
    void migrateBlocksMovesLoserToOwnBlock() throws Exception {
        ScanGroup group = group(Map.of(
                "A.java", line("100;") + line("101;"),
                "B.java", line("102;")));

        List<FileChange> changes = NumberingEngine.renumber(group, NumberAudit.of(group, 10).sharedBlockLosers(),
                RANGE, 10);

        assertEquals(1, changes.size());
        assertEquals(line("110;"), changes.get(0).content());
    }

    @Test
    void rebaseRenumbersEverything() throws Exception {
        ScanGroup group = group(Map.of("A.java", line("150;"), "B.java", line("0;")));
        Set<LogStatement> all = group.statements().collect(Collectors.toSet());

        List<FileChange> changes = NumberingEngine.renumber(group, all, new LogNumberRange(120, 200), 10);

        assertEquals(line("120;"), changes.get(0).content());
        assertEquals(line("130;"), changes.get(1).content());
    }

    @Test
    void numbersOfOtherFilesStayReserved() throws Exception {
        ScanGroup group = group(Map.of("A.java", line("100;"), "B.java", line("110;")));
        Set<LogStatement> onlyB = group.files().get(1).statements().stream().collect(Collectors.toSet());

        assertEquals(line("110;"), single(group, onlyB));
    }

    @Test
    void resetWritesZero() throws Exception {
        ScanGroup group = group(Map.of("A.java", line("150;") + line("")));

        List<FileChange> changes = NumberingEngine.reset(group, select(group, State.NUMBERED));

        assertEquals(line("0;") + line(""), changes.get(0).content());
    }

    @Test
    void failsBeforeChangingAnythingIfRangeIsTooSmall() throws Exception {
        LogNumberRange small = new LogNumberRange(100, 110);
        ScanGroup group = new ScanGroup(small, List.of("module"), group(Map.of(
                "A.java", line("0;"),
                "B.java", line("0;"),
                "C.java", line("0;"))).files());

        MojoFailureException failure = assertThrows(MojoFailureException.class,
                () -> NumberingEngine.renumber(group, placeholders(group), small, 10));

        assertTrue(failure.getMessage().contains("needs 3 blocks but only 1 are free"), failure.getMessage());
    }

    private ScanGroup group(Map<String, String> contents) throws IOException {
        return TestSources.group(dir, RANGE, contents);
    }

    private static Set<LogStatement> placeholders(ScanGroup group) {
        return select(group, State.PLACEHOLDER);
    }

    private static Set<LogStatement> select(ScanGroup group, State state) {
        return group.statements().filter(statement -> statement.state() == state).collect(Collectors.toSet());
    }

    private static String single(ScanGroup group, Set<LogStatement> selected) throws MojoFailureException {
        List<FileChange> changes = NumberingEngine.renumber(group, selected, RANGE, 10);
        assertEquals(1, changes.size());
        return changes.get(0).content();
    }
}
