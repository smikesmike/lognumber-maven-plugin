package org.futureaspects.mojo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.futureaspects.mojo.LogStatement.State;

/**
 * Finds logging statements in source text.
 */
final class LogStatementParser {

    static final String DEFAULT_LOGGER_NAMES = "log|logger|s_logger|LOG|LOGGER|Log|getLog\\(\\)";
    static final String DEFAULT_LOG_LEVELS = "debug|info|warn|trace|error|fatal|v|d|i|w|e";

    private final Pattern pattern;

    private LogStatementParser(Pattern pattern) {
        this.pattern = pattern;
    }

    /**
     * Creates a parser for the given regular expressions.
     *
     * @param loggerNames alternatives of the logger names
     * @param logLevels   alternatives of the log levels
     * @return the parser
     */
    static LogStatementParser of(String loggerNames, String logLevels) {
        return new LogStatementParser(Pattern.compile(
                "(?<![\\w$])(?:" + loggerNames + ")\\s*\\.(?:" + logLevels + ")\\s*\\(\\s*\"(?!\"\")"
                        + "(?<prefix>(?<digits>\\d{1,9})?;)?"));
    }

    /**
     * Finds all logging statements.
     *
     * @param file    the file the content belongs to
     * @param content the source text
     * @return the statements in order of appearance
     */
    List<LogStatement> parse(Path file, String content) {
        List<LogStatement> statements = new ArrayList<>();
        Matcher matcher = pattern.matcher(content);
        int line = 1;
        int position = 0;
        while (matcher.find()) {
            for (; position < matcher.start(); position++) {
                if (content.charAt(position) == '\n') {
                    line++;
                }
            }
            statements.add(toStatement(file, line, matcher));
        }
        return statements;
    }

    private static LogStatement toStatement(Path file, int line, Matcher matcher) {
        int end = matcher.end();
        if (matcher.group("prefix") == null) {
            return new LogStatement(file, line, end, end, State.UNPREFIXED, 0);
        }
        int start = matcher.start("prefix");
        String digits = matcher.group("digits");
        int number = digits == null ? 0 : Integer.parseInt(digits);
        State state = number == 0 ? State.PLACEHOLDER : State.NUMBERED;
        return new LogStatement(file, line, start, end, state, number);
    }
}
