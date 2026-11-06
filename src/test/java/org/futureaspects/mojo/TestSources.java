package org.futureaspects.mojo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Creates scanned groups from text.
 */
final class TestSources {

    private TestSources() {
    }

    static SourceFileStore store() {
        return new SourceFileStore("java", LogStatementParser.of(LogStatementParser.DEFAULT_LOGGER_NAMES,
                LogStatementParser.DEFAULT_LOG_LEVELS));
    }

    /** Returns a line with a statement carrying the given prefix. */
    static String line(String prefix) {
        return "log.info(\"" + prefix + "text\");\n";
    }

    static ScanGroup group(Path dir, LogNumberRange range, Map<String, String> contents) throws IOException {
        List<SourceFile> files = new ArrayList<>();
        for (Map.Entry<String, String> file : new TreeMap<>(contents).entrySet()) {
            Path path = dir.resolve(file.getKey());
            Files.createDirectories(path.getParent());
            Files.writeString(path, file.getValue());
            files.add(store().read(path));
        }
        return new ScanGroup(range, List.of("module"), files);
    }
}
