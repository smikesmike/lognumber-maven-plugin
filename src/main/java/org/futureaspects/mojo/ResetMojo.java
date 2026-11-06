package org.futureaspects.mojo;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.maven.plugins.annotations.Mojo;

/**
 * Resets all lognumbers to "0;"
 */
@Mojo(name = "reset", requiresProject = false, aggregator = true, threadSafe = true)
public class ResetMojo extends AbstractWritingMojo {

    @Override
    protected boolean rangeRequired() {
        return false;
    }

    @Override
    protected Predicate<LogStatement> select(ScanGroup group) {
        return statement -> statement.isNumbered() || isSelectedUnprefixed(statement);
    }

    @Override
    protected List<FileChange> plan(ScanGroup group, Set<LogStatement> selected) {
        return NumberingEngine.reset(group, selected);
    }
}
