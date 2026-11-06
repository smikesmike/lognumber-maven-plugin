package org.futureaspects.mojo;

import java.util.function.Predicate;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Renumbers all logging statements in blocks.
 */
@Mojo(name = "rebase", requiresProject = false, aggregator = true, threadSafe = true)
public class RebaseMojo extends AbstractWritingMojo {

    /**
     * The first number to use, which must be inside of the range. Defaults to the start of the range.
     */
    @Parameter(property = "newBaseNumber")
    private Integer newBaseNumber;

    @Override
    protected Predicate<LogStatement> select(ScanGroup group) {
        return statement -> statement.state() != LogStatement.State.UNPREFIXED || isSelectedUnprefixed(statement);
    }

    @Override
    protected LogNumberRange allocationRange(ScanGroup group) throws MojoFailureException {
        LogNumberRange range = group.range();
        if (newBaseNumber == null) {
            return range;
        }
        if (!range.contains(newBaseNumber)) {
            throw new MojoFailureException("newBaseNumber " + newBaseNumber + " is outside of range " + range);
        }
        return new LogNumberRange(newBaseNumber, range.end());
    }
}
