/**
 * Copyright Deutsche Telekom GmbH (c) 2018
 * 
 * Project: lognumber-maven-plugin
 *  
 * Created on 13.09.2018
 */
package org.futureaspects.mojo;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * base class for common operations on files containing lognumber statements
 *
 * @author <a href="mailto:marc.wilhelm@futureaspects.com">Marc Wilhelm</a>
 *
 */
public abstract class LognumberBaseMojo extends AbstractMojo {

    // Pattern pattern = Pattern.compile(
    // "(?<prefix>(?:log|logger|LOG|getLog\\(\\))\\s*\\.(debug|info|warn|trace|error|fatal)\\s*\\(\\s*\\\")((?<lognum>\\d+)\\;)?",
    // Pattern.MULTILINE | Pattern.DOTALL);

    /**
     * Extension of the files which should be processed without a "."
     */
    @Parameter(property = "fileExtension", defaultValue = "java")
    protected String fileExtension;

    /**
     * Base folder which is scanned for files with the given extension
     */
    @Parameter(defaultValue = ".", property = "baseDir", required = true)
    protected File baseDirectory;

    /**
     * File extension which is used as backup name of the original file
     */
    @Parameter(defaultValue = ".bak", property = "backupExtension", required = false)
    private String backupExtension;

    /**
     * Regular expression for the possible logger names
     */
    @Parameter(defaultValue = "log|logger|s_logger|LOG|LOGGER|getLog\\(\\)", property = "loggerNamePattern", required = false)
    private String loggerNamePattern;

    /**
     * Regular expression for the possible log levels
     */
    @Parameter(defaultValue = "debug|info|warn|trace|error|fatal", property = "logLevelPattern", required = false)
    private String logLevelPattern;

    /**
     * Searches for the highest lognumber
     * 
     * @param f
     * @return the highest lognumber or 0 if no were found
     */
    protected int seekHighestNumber(File f) {
        getLog().debug("scanning '" + f.getAbsolutePath() + "' ...");
        Stream<String> lines;
        int highest = 0;
        int missing = 0;
        try {
            String content = null;
            try {
                lines = Files.lines(f.toPath());
                content = lines.reduce("", (a, b) -> a + "\n" + b);
            } catch (MalformedInputException |UncheckedIOException mie) {
                // -- try to decode the file with ISO encoding --
                lines = Files.lines(f.toPath(), StandardCharsets.ISO_8859_1);
                content = lines.reduce("", (a, b) -> a + "\n" + b);
            } // read-try-catch
            Matcher m = getPattern().matcher(content);
            while (m.find()) {
                String lognum = m.group("lognum");
                // getLog().info("lognum=" + lognum);
                if (lognum == null || lognum.equals("0")) {
                    missing++;
                } else {
                    int n = Integer.parseInt(lognum);
                    highest = Math.max(highest, n);
                }
            } // while
            getLog().debug("highest:" + highest + " missing:" + missing);

            lines.close();
        } catch (IOException e) {
            getLog().error(
                    "IOException due processing baseDir '" + baseDirectory + "':" + e.getMessage(),
                    e);
            // throw new MojoExecutionException("IOException due processing
            // baseDir '" + baseDirectory + "':" + e.getMessage(), e);
        } finally {

        } // try-catch-finally
        return highest;
    }

    /**
     * Searches for the highest lognumber
     * 
     * @param f
     * @param backupFiles
     * @param dryRun
     * @return the highest lognumber or 0 if no were found
     */
    protected void assignLognumbers(ComputingContext ctx, File f, boolean dryRun,
            boolean backupFiles) {
        getLog().debug("modifying '" + f.getAbsolutePath() + "' ...");
        Stream<String> lines;
        int highest = 0;
        boolean modified = false;
        try {
            lines = Files.lines(f.toPath());
            String content = lines.reduce("", (a, b) -> a + "\n" + b);
            lines.close();

            Matcher m = getPattern().matcher(content);
            StringBuffer sb = new StringBuffer();

            while (m.find()) {
                String lognum = m.group("lognum");
                String prefix = m.group("prefix");
                String repString = prefix + (++ctx.highestNumber) + ";";
                // getLog().info("lognum=" + lognum);
                if (lognum == null || lognum.equals("0")) {
                    m.appendReplacement(sb, repString);
                    modified = true;
                    ctx.correctedStatements++;
                } else {
                    int n = Integer.parseInt(lognum);
                    highest = Math.max(highest, n);
                }
            } // while more lognumbers available
            m.appendTail(sb);

            if (modified) {
                getLog().debug("assigning lognumbers in: " + f.getAbsolutePath());
                ctx.touchedFiles.add(f);
                if (dryRun) {
                    getLog().info("dry run modification of '" + f.getAbsolutePath() + "'");
                } else {
                    if (backupFiles) {
                        Files.move(f.toPath(),
                                new File(f.getAbsolutePath() + backupExtension).toPath(),
                                StandardCopyOption.REPLACE_EXISTING);
                    }
                    Files.write(f.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8),
                            StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);

                }
            }

        } catch (IOException e) {
            getLog().error(
                    "IOException due processing baseDir '" + baseDirectory + "':" + e.getMessage(),
                    e);
            // throw new MojoExecutionException("IOException due processing
            // baseDir '" + baseDirectory + "':" + e.getMessage(), e);
        } finally {

        }
    }

    /**
     * Creates a new reg ex pattern
     * 
     * @return a fresh created pattern
     */
    Pattern getPattern() {
        return Pattern.compile(
                "(?<prefix>(?:" + loggerNamePattern + ")\\s*\\.(" + logLevelPattern
                        + ")\\s*\\(\\s*\\\")((?<lognum>\\d+)\\;)?",
                Pattern.MULTILINE | Pattern.DOTALL);
    }
}
