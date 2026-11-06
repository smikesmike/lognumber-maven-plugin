package org.futureaspects.mojo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.futureaspects.mojo.NumberAudit.Violation;

/**
 * Writes violations as a semicolon-separated file.
 */
final class ViolationReportWriter {

    private ViolationReportWriter() {
    }

    /**
     * @param target     the report file, parent directories are created
     * @param violations the violations per group name
     * @throws IOException if the file cannot be written
     */
    static void write(Path target, Map<String, List<Violation>> violations) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("group;type;number;file;line");
        violations.forEach((group, entries) -> entries.forEach(violation -> {
            LogStatement statement = violation.statement();
            lines.add(group + ";" + violation.type() + ";" + statement.number() + ";"
                    + quote(statement.file().toString()) + ";" + statement.line());
        }));
        Path parent = target.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Files.write(target, lines);
    }

    private static String quote(String value) {
        return value.contains(";") || value.contains("\"") ? '"' + value.replace("\"", "\"\"") + '"' : value;
    }
}
