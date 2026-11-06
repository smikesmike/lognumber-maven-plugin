package org.futureaspects.mojo;

import java.util.HashSet;
import java.util.function.Predicate;

import org.apache.maven.plugins.annotations.Mojo;

/**
 * Assigns new numbers to all lognumbers outside of the configured range.
 */
@Mojo(name = "fix-range", requiresProject = false, aggregator = true, threadSafe = true)
public class FixRangeMojo extends AbstractWritingMojo {

    @Override
    protected Predicate<LogStatement> select(ScanGroup group) {
        return new HashSet<>(NumberAudit.of(group, blockSize()).outOfRange())::contains;
    }
}
