package org.futureaspects.mojo;

import java.util.function.Predicate;

import org.apache.maven.plugins.annotations.Mojo;

/**
 * Keeps one occurrence of every duplicated lognumber and assigns new numbers to the others.
 */
@Mojo(name = "fix-duplicates", requiresProject = false, aggregator = true, threadSafe = true)
public class FixDuplicatesMojo extends AbstractWritingMojo {

    @Override
    protected Predicate<LogStatement> select(ScanGroup group) {
        return NumberAudit.of(group, blockSize()).duplicateLosers()::contains;
    }
}
