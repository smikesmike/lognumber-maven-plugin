package org.futureaspects.mojo;

import java.util.function.Predicate;

import org.apache.maven.plugins.annotations.Mojo;

/**
 * Resolves blocks shared by several files. The file with the most numbers in a block keeps it, the others get new
 * blocks. Run this once after fix-range and fix-duplicates to move a project to block numbering.
 */
@Mojo(name = "migrate-blocks", requiresProject = false, aggregator = true, threadSafe = true)
public class MigrateBlocksMojo extends AbstractWritingMojo {

    @Override
    protected Predicate<LogStatement> select(ScanGroup group) {
        return NumberAudit.of(group, blockSize()).sharedBlockLosers()::contains;
    }
}
