package org.futureaspects.mojo;

import java.nio.file.Path;

/**
 * A logging statement found in a source file.
 *
 * @param file   the file containing the statement
 * @param line   1-based line of the statement
 * @param start  offset of the first character behind the opening quote
 * @param end    offset behind the lognumber prefix, equal to {@code start} if the statement has no prefix
 * @param state  how the statement is numbered
 * @param number the lognumber, 0 unless the state is {@link State#NUMBERED}
 */
record LogStatement(Path file, int line, int start, int end, State state, int number) {

    /** How a statement is numbered. */
    enum State {
        /** Carries a lognumber. */
        NUMBERED,
        /** Requests a lognumber using {@code ;} or {@code 0;}. */
        PLACEHOLDER,
        /** Has no prefix at all. */
        UNPREFIXED
    }

    boolean isNumbered() {
        return state == State.NUMBERED;
    }

    /** Returns the location in the {@code file:line} form. */
    String location() {
        return file + ":" + line;
    }
}
