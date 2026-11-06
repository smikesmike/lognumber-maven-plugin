package org.futureaspects.mojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

import org.apache.maven.plugin.MojoFailureException;

/**
 * Computes file changes without writing anything.
 */
final class NumberingEngine {

    private NumberingEngine() {
    }

    /**
     * Gives new numbers to the selected statements.
     *
     * @param group     the group
     * @param selected  the statements to number
     * @param range     the range for new numbers
     * @param blockSize the numbers per block
     * @return the changes
     * @throws MojoFailureException if the range is too small
     */
    static List<FileChange> renumber(ScanGroup group, Set<LogStatement> selected, LogNumberRange range, int blockSize)
            throws MojoFailureException {
        List<LogStatement> retained = group.statements()
                .filter(statement -> statement.isNumbered() && !selected.contains(statement))
                .toList();
        BlockLayout layout = BlockLayout.of(range, blockSize, retained);

        Map<LogStatement, Integer> numbers = new HashMap<>();
        group.statements().filter(selected::contains).forEach(statement -> {
            OptionalInt number = layout.allocate(statement.file());
            number.ifPresent(value -> numbers.put(statement, value));
        });

        if (layout.missingBlocks() > 0) {
            int free = layout.claimedBlocks();
            throw new MojoFailureException("Range " + range + " is too small: needs " + (free + layout.missingBlocks())
                    + " blocks but only " + free + " are free. No file was changed.");
        }
        return rewrite(group, numbers);
    }

    /**
     * Turns the selected statements back into {@code 0;}.
     *
     * @param group    the group
     * @param selected the statements to reset
     * @return the changes
     */
    static List<FileChange> reset(ScanGroup group, Set<LogStatement> selected) {
        Map<LogStatement, Integer> numbers = new HashMap<>();
        selected.forEach(statement -> numbers.put(statement, 0));
        return rewrite(group, numbers);
    }

    private static List<FileChange> rewrite(ScanGroup group, Map<LogStatement, Integer> numbers) {
        List<FileChange> changes = new ArrayList<>();
        for (SourceFile file : group.files()) {
            StringBuilder content = new StringBuilder();
            List<FileChange.NumberChange> changed = new ArrayList<>();
            int position = 0;
            for (LogStatement statement : file.statements()) {
                Integer number = numbers.get(statement);
                if (number != null) {
                    content.append(file.content(), position, statement.start()).append(number).append(';');
                    position = statement.end();
                    changed.add(new FileChange.NumberChange(statement, number));
                }
            }
            if (!changed.isEmpty()) {
                content.append(file.content(), position, file.content().length());
                changes.add(new FileChange(file, content.toString(), changed));
            }
        }
        return changes;
    }
}
