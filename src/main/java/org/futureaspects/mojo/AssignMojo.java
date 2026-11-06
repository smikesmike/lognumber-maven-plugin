package org.futureaspects.mojo;

import java.util.List;
import java.util.function.Predicate;

import org.apache.maven.plugins.annotations.Mojo;
import org.futureaspects.mojo.LogStatement.State;

/**
 * Assigns lognumbers to all statements beginning with ";" or "0;", using one block of numbers per file.
 */
@Mojo(name = "assign", requiresProject = false, aggregator = true, threadSafe = true)
public class AssignMojo extends AbstractWritingMojo {

    @Override
    protected Predicate<LogStatement> select(ScanGroup group) {
        return statement -> statement.state() == State.PLACEHOLDER || isSelectedUnprefixed(statement);
    }

    @Override
    protected void afterRun(List<ScanGroup> groups) {
        groups.forEach(this::logViolations);
    }
}
