package org.futureaspects.mojo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.futureaspects.mojo.LogStatement.State;

/**
 * Base of all goals which change files. Every change is computed first, so that a failure leaves all files untouched.
 */
public abstract class AbstractWritingMojo extends AbstractLognumberMojo {

    /**
     * Flag to run in "readonly mode" without touching nor modifying the files
     */
    @Parameter(defaultValue = "false", property = "dryRun")
    private boolean dryRun;

    /**
     * Flag to backup the original files before modification
     */
    @Parameter(defaultValue = "false", property = "backupFiles")
    private boolean backupFiles;

    /**
     * File extension which is used as backup name of the original file
     */
    @Parameter(defaultValue = ".bak", property = "backupExtension")
    private String backupExtension;

    /**
     * Limits writing to these files, relative to the directory Maven was started in. All files are still analysed.
     */
    @Parameter(property = "files")
    private List<String> files;

    /**
     * Flag to also process statements without any prefix, such as log.info("message")
     */
    @Parameter(defaultValue = "false", property = "numberUnprefixed")
    private boolean numberUnprefixed;

    @Override
    protected final void run(List<ScanGroup> groups) throws IOException, MojoFailureException {
        Predicate<LogStatement> writable = writableStatements();
        List<FileChange> changes = new ArrayList<>();
        for (ScanGroup group : groups) {
            Set<LogStatement> selected = group.statements().filter(select(group)).filter(writable)
                    .collect(Collectors.toCollection(HashSet::new));
            changes.addAll(plan(group, selected));
        }

        int numbers = 0;
        for (FileChange change : changes) {
            logChange(change);
            numbers += change.numbers().size();
            if (!dryRun) {
                SourceFileStore.write(change, backupFiles ? backupExtension : null);
            }
        }
        getLog().info("0;" + (dryRun ? "Dry run: would change " : "Changed ") + numbers + " lognumber(s) in "
                + changes.size() + " file(s)");
        afterRun(groups);
    }

    private Predicate<LogStatement> writableStatements() {
        if (files == null || files.isEmpty()) {
            return statement -> true;
        }
        Set<Path> paths = new HashSet<>();
        for (String file : files) {
            Path path = executionRoot().resolve(file).toAbsolutePath().normalize();
            if (!Files.exists(path)) {
                getLog().warn("0;Ignoring missing file " + path);
            }
            paths.add(path);
        }
        return statement -> paths.contains(statement.file());
    }

    private void logChange(FileChange change) {
        for (FileChange.NumberChange number : change.numbers()) {
            LogStatement statement = number.statement();
            String message = statement.location() + ": " + statement.number() + " -> " + number.number();
            if (dryRun) {
                getLog().info("0;" + message);
            } else {
                getLog().debug("0;" + message);
            }
        }
    }

    /**
     * Returns which statements of a group this goal processes.
     *
     * @param group the group
     * @return the filter
     */
    protected abstract Predicate<LogStatement> select(ScanGroup group);

    /**
     * Computes the changes of a group.
     *
     * @param group    the group
     * @param selected the statements to process
     * @return the changes
     * @throws MojoFailureException if the changes cannot be computed
     */
    protected List<FileChange> plan(ScanGroup group, Set<LogStatement> selected) throws MojoFailureException {
        return NumberingEngine.renumber(group, selected, allocationRange(group), blockSize());
    }

    /**
     * Returns the range new numbers are taken from.
     *
     * @param group the group
     * @return the range
     * @throws MojoFailureException if there is no valid range
     */
    protected LogNumberRange allocationRange(ScanGroup group) throws MojoFailureException {
        return group.range();
    }

    /**
     * Called after the changes have been made.
     *
     * @param groups the groups in the state before the change
     */
    protected void afterRun(List<ScanGroup> groups) {
        // nothing to do by default
    }

    /** Returns whether a statement is an unprefixed one which should be processed. */
    protected final boolean isSelectedUnprefixed(LogStatement statement) {
        return numberUnprefixed && statement.state() == State.UNPREFIXED;
    }
}
