package org.futureaspects.mojo;

import java.util.List;

/**
 * The computed new content of a source file.
 *
 * @param file    the unchanged file
 * @param content the new text
 * @param numbers the changed statements
 */
record FileChange(SourceFile file, String content, List<NumberChange> numbers) {

    /**
     * A changed lognumber.
     *
     * @param statement the statement before the change
     * @param number    the new number
     */
    record NumberChange(LogStatement statement, int number) {
    }
}
