package org.futureaspects.mojo;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * Files which share one lognumber range.
 *
 * @param range   the common range, {@code null} if the goal does not need one
 * @param modules names of the modules in this group
 * @param files   the files sorted by path
 */
record ScanGroup(LogNumberRange range, List<String> modules, List<SourceFile> files) {

    /** Returns all statements in a stable order. */
    Stream<LogStatement> statements() {
        return files.stream().map(SourceFile::statements).flatMap(Collection::stream);
    }

    /** Returns the name used for reports. */
    String name() {
        return range == null ? "all" : range.toString();
    }
}
