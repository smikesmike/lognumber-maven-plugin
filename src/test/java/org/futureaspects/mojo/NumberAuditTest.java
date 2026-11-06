package org.futureaspects.mojo;

import static org.futureaspects.mojo.TestSources.line;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.futureaspects.mojo.NumberAudit.ViolationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NumberAuditTest {

    private static final LogNumberRange RANGE = new LogNumberRange(100, 200);

    @TempDir
    Path dir;

    @Test
    void reportsDuplicatesInsideOfAFile() throws IOException {
        NumberAudit audit = audit(Map.of("A.java", line("100;") + line("100;")));

        assertEquals(List.of(ViolationType.DUPLICATE, ViolationType.DUPLICATE),
                audit.violations().stream().map(NumberAudit.Violation::type).toList());
    }

    @Test
    void keepsDuplicateInFileWithMostNumbersOfTheBlock() throws IOException {
        NumberAudit audit = audit(Map.of(
                "A.java", line("100;"),
                "B.java", line("100;") + line("101;")));

        assertEquals(Set.of(dir.resolve("A.java")), files(audit.duplicateLosers()));
    }

    @Test
    void keepsFirstDuplicateOnTie() throws IOException {
        NumberAudit audit = audit(Map.of("A.java", line("100;"), "B.java", line("100;")));

        assertEquals(Set.of(dir.resolve("B.java")), files(audit.duplicateLosers()));
    }

    @Test
    void reportsNumbersOutsideOfTheRange() throws IOException {
        NumberAudit audit = audit(Map.of("A.java", line("99;") + line("100;") + line("200;")));

        assertEquals(List.of(99, 200), audit.outOfRange().stream().map(LogStatement::number).toList());
    }

    @Test
    void sharedBlocksAreNoViolation() throws IOException {
        NumberAudit audit = audit(Map.of("A.java", line("100;"), "B.java", line("101;")));

        assertTrue(audit.violations().isEmpty());
        assertEquals(Set.of(10), audit.sharedBlocks().keySet());
    }

    @Test
    void ownerOfSharedBlockKeepsItsNumbers() throws IOException {
        NumberAudit audit = audit(Map.of(
                "A.java", line("100;"),
                "B.java", line("101;") + line("102;")));

        assertEquals(Set.of(dir.resolve("A.java")), files(audit.sharedBlockLosers()));
    }

    private NumberAudit audit(Map<String, String> contents) throws IOException {
        return NumberAudit.of(TestSources.group(dir, RANGE, contents), 10);
    }

    private static Set<Path> files(Set<LogStatement> statements) {
        return Set.copyOf(statements.stream().map(LogStatement::file).toList());
    }
}
