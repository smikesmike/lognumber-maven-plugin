package org.futureaspects.mojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.futureaspects.mojo.LogStatement.State;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class LogStatementParserTest {

    private static final Path FILE = Path.of("Example.java");

    private final LogStatementParser parser = LogStatementParser.of(LogStatementParser.DEFAULT_LOGGER_NAMES,
            LogStatementParser.DEFAULT_LOG_LEVELS);

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "log.info(\"text\");           | UNPREFIXED  | 0",
            "log.info(\";text\");          | PLACEHOLDER | 0",
            "log.info(\"0;text\");         | PLACEHOLDER | 0",
            "log.info(\"000;text\");       | PLACEHOLDER | 0",
            "log.info(\"12345;text\");     | NUMBERED    | 12345",
            "LOGGER.warn(\"7;text\");      | NUMBERED    | 7",
            "getLog().info(\"7;text\");    | NUMBERED    | 7",
            "s_logger.error(\"7;text\");   | NUMBERED    | 7",
            "this.log.debug(\"7;text\");   | NUMBERED    | 7",
            "Log.d(\"7;text\");            | NUMBERED    | 7",
            "log.info(\"20261106120000;\"); | UNPREFIXED | 0" })
    void recognizesStatements(String source, State state, int number) {
        LogStatement statement = parseSingle(source);
        assertEquals(state, statement.state());
        assertEquals(number, statement.number());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "log.info(\"\"\"",
            "catalog.info(\"0;text\");",
            "m_log.info(\"0;text\");",
            "log.info(message);",
            "Log.d(TAG, \"0;text\");" })
    void ignoresNonStatements(String source) {
        assertTrue(parser.parse(FILE, source).isEmpty());
    }

    @Test
    void findsMultiLineStatements() {
        LogStatement statement = parseSingle("log.info(\n    \"5;text\");");
        assertEquals(5, statement.number());
        assertEquals(1, statement.line());
    }

    @Test
    void countsLinesAcrossStatements() {
        List<LogStatement> statements = parser.parse(FILE, "\nlog.info(\"1;a\");\n\nlog.info(\"2;b\");\n");
        assertEquals(List.of(2, 4), statements.stream().map(LogStatement::line).toList());
    }

    private LogStatement parseSingle(String source) {
        List<LogStatement> statements = parser.parse(FILE, source);
        assertEquals(1, statements.size());
        return statements.get(0);
    }
}
