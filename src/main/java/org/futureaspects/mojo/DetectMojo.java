package org.futureaspects.mojo;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.futureaspects.mojo.LogStatement.State;
import org.futureaspects.mojo.NumberAudit.Violation;

/**
 * Reports the lognumbers of all modules: highest number, missing numbers, duplicates and numbers outside of the range.
 */
@Mojo(name = "detect", requiresProject = false, aggregator = true, threadSafe = true)
public class DetectMojo extends AbstractLognumberMojo {

    /**
     * Flag to fail the build if duplicates or numbers outside of the range exist
     */
    @Parameter(defaultValue = "false", property = "failOnViolation")
    private boolean failOnViolation;

    /**
     * File to write the violations to, semicolon-separated. Nothing is written if it is not set.
     */
    @Parameter(property = "reportFile")
    private File reportFile;

    @Override
    protected void run(List<ScanGroup> groups) throws IOException, MojoFailureException {
        Map<String, List<Violation>> violations = new LinkedHashMap<>();
        for (ScanGroup group : groups) {
            logSummary(group);
            violations.put(group.name(), logViolations(group));
        }
        if (reportFile != null) {
            ViolationReportWriter.write(reportFile.toPath(), violations);
        }
        long count = violations.values().stream().mapToLong(List::size).sum();
        if (failOnViolation && count > 0) {
            throw new MojoFailureException(count + " lognumber violation(s) found");
        }
    }

    private void logSummary(ScanGroup group) {
        int highest = group.statements().mapToInt(LogStatement::number).max().orElse(0);
        getLog().info("0;Range " + group.name() + " (" + String.join(", ", group.modules()) + "): "
                + group.files().size() + " file(s), highest lognumber " + highest + ", "
                + count(group, State.PLACEHOLDER) + " to assign, " + count(group, State.UNPREFIXED)
                + " without prefix");
    }

    private static long count(ScanGroup group, State state) {
        return group.statements().filter(statement -> statement.state() == state).count();
    }
}
