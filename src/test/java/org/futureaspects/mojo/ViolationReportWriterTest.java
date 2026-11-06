package org.futureaspects.mojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.futureaspects.mojo.LogStatement.State;
import org.futureaspects.mojo.NumberAudit.Violation;
import org.futureaspects.mojo.NumberAudit.ViolationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ViolationReportWriterTest {

    @TempDir
    Path dir;

    @Test
    void writesOneLinePerViolation() throws IOException {
        Path report = dir.resolve("nested/report.csv");

        ViolationReportWriter.write(report, Map.of("100,200", List.of(
                violation(ViolationType.DUPLICATE, "A.java", 150),
                violation(ViolationType.OUT_OF_RANGE, "a;b.java", 999))));

        assertEquals(List.of(
                "group;type;number;file;line",
                "100,200;DUPLICATE;150;A.java;3",
                "100,200;OUT_OF_RANGE;999;\"a;b.java\";3"), Files.readAllLines(report));
    }

    private static Violation violation(ViolationType type, String file, int number) {
        return new Violation(type, new LogStatement(Path.of(file), 3, 0, 0, State.NUMBERED, number));
    }
}
