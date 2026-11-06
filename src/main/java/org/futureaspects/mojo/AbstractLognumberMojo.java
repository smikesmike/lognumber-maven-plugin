package org.futureaspects.mojo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.futureaspects.mojo.NumberAudit.Violation;

/**
 * Common flow of all goals: resolve the modules, scan them and hand the result to the goal.
 */
public abstract class AbstractLognumberMojo extends AbstractMojo {

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    /**
     * Extension of the files which should be processed without a "."
     */
    @Parameter(property = "fileExtension", defaultValue = "java")
    private String fileExtension;

    /**
     * Folder to scan when running without a pom.xml. Has no effect inside a Maven project.
     */
    @Parameter(defaultValue = ".", property = "baseDir", required = true)
    private File baseDirectory;

    /**
     * Regular expression for the possible logger names
     */
    @Parameter(defaultValue = LogStatementParser.DEFAULT_LOGGER_NAMES, property = "loggerNamePattern")
    private String loggerNamePattern;

    /**
     * Regular expression for the possible log levels
     */
    @Parameter(defaultValue = LogStatementParser.DEFAULT_LOG_LEVELS, property = "logLevelPattern")
    private String logLevelPattern;

    /**
     * Number of lognumbers per block
     */
    @Parameter(defaultValue = "10", property = "blockSize")
    private int blockSize;

    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException {
        if (blockSize < 1) {
            throw new MojoFailureException("blockSize must be positive but is " + blockSize);
        }
        SourceFileStore store = new SourceFileStore(fileExtension,
                LogStatementParser.of(loggerNamePattern, logLevelPattern));
        try {
            run(new ScanGroupResolver(store).resolve(modules(), rangeRequired()));
        } catch (IOException e) {
            throw new MojoExecutionException("Processing the source files failed: " + e.getMessage(), e);
        }
    }

    /**
     * Executes the goal.
     *
     * @param groups the scanned files per range
     * @throws IOException          if a file cannot be accessed
     * @throws MojoFailureException if the goal cannot be completed
     */
    protected abstract void run(List<ScanGroup> groups) throws IOException, MojoFailureException;

    /** Returns whether this goal needs a lognumber range. */
    protected boolean rangeRequired() {
        return true;
    }

    /** Returns the numbers per block. */
    protected final int blockSize() {
        return blockSize;
    }

    /** Returns the directory Maven was started in. */
    protected final Path executionRoot() {
        return Path.of(session.getExecutionRootDirectory());
    }

    /**
     * Logs the violations of a group.
     *
     * @param group the group
     * @return the violations
     */
    protected final List<Violation> logViolations(ScanGroup group) {
        NumberAudit audit = NumberAudit.of(group, blockSize);
        List<Violation> violations = audit.violations();
        for (Violation violation : violations) {
            LogStatement statement = violation.statement();
            getLog().warn("0;" + violation.type() + " " + statement.number() + " at " + statement.location());
        }
        if (!audit.sharedBlocks().isEmpty()) {
            getLog().info("0;" + audit.sharedBlocks().size() + " block(s) are shared by several files in range "
                    + group.name());
        }
        return violations;
    }

    private List<ModuleSources> modules() throws MojoFailureException {
        return session.getRequest().isProjectPresent() ? reactorModules() : List.of(standaloneModule());
    }

    private ModuleSources standaloneModule() throws MojoFailureException {
        if (!baseDirectory.exists()) {
            throw new MojoFailureException("Missing baseDir: '" + baseDirectory + "'");
        }
        String range = session.getUserProperties().getProperty(LogNumberRange.PROPERTY);
        if (range == null && rangeRequired()) {
            throw new MojoFailureException(
                    "Without a pom.xml the range must be given by -D" + LogNumberRange.PROPERTY + "=<first>,<end>");
        }
        return new ModuleSources(baseDirectory.getPath(), parseRange(range, "-D" + LogNumberRange.PROPERTY),
                List.of(baseDirectory.toPath()));
    }

    private List<ModuleSources> reactorModules() throws MojoFailureException {
        LogNumberRange rootRange = rangeOf(session.getTopLevelProject());
        List<ModuleSources> modules = new ArrayList<>();
        for (MavenProject project : session.getProjects()) {
            LogNumberRange ownRange = rangeOf(project);
            LogNumberRange range = ownRange == null ? rootRange : ownRange;
            warnIfParentIsMissing(project, range);
            modules.add(new ModuleSources(project.getArtifactId(), range, sourceRoots(project)));
        }
        return modules;
    }

    private static LogNumberRange rangeOf(MavenProject project) throws MojoFailureException {
        return parseRange(project.getProperties().getProperty(LogNumberRange.PROPERTY), project.getArtifactId());
    }

    private static LogNumberRange parseRange(String value, String source) throws MojoFailureException {
        try {
            return value == null ? null : LogNumberRange.parse(value);
        } catch (IllegalArgumentException e) {
            throw new MojoFailureException(
                    "Invalid value of " + LogNumberRange.PROPERTY + " in " + source + ": " + e.getMessage(), e);
        }
    }

    private void warnIfParentIsMissing(MavenProject project, LogNumberRange range) throws MojoFailureException {
        MavenProject parent = project.getParent();
        boolean localParent = parent != null && parent.getFile() != null
                && "pom.xml".equals(parent.getFile().getName());
        if (range != null && localParent && !session.getProjects().contains(parent)
                && range.equals(rangeOf(parent))) {
            getLog().warn("0;" + project.getArtifactId() + " shares range " + range + " with modules outside of this"
                    + " run. Start Maven from the root directory to check all of them.");
        }
    }

    private static List<Path> sourceRoots(MavenProject project) {
        Path buildDirectory = Path.of(project.getBuild().getDirectory());
        return Stream.concat(project.getCompileSourceRoots().stream(), project.getTestCompileSourceRoots().stream())
                .map(Path::of)
                .filter(root -> !root.startsWith(buildDirectory))
                .toList();
    }
}
