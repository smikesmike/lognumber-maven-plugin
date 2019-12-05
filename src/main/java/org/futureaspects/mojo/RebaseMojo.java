/**
 * Copyright Futureaspects(c) 2019
 * 
 * Project: lognumber-maven-plugin
 *  
 * Created on 05.12.2019
 */
package org.futureaspects.mojo;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Reassign all logging statements to be based on a new number set.
 *
 */
@Mojo(name = "rebase", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresProject = false)
public class RebaseMojo extends DetectMojo {

    /**
     * The new starting number for the lognumbers after resetting to "0"
     */
    @Parameter(property = "newBaseNumber", required = true)
    protected int newBaseNumber;

    /**
     * Flag to run in "readonly mode" without touching nor modifying the files
     */
    @Parameter(defaultValue = "false", property = "dryRun", required = false)
    protected boolean dryRun;

    /**
     * Flag to backup the original files before modification
     */
    @Parameter(defaultValue = "false", property = "backupFiles", required = false)
    protected boolean backupFiles;

    public void execute() throws MojoExecutionException {

        getLog().info("Rebase lognumbers to \"" + newBaseNumber + "\"...");

        // -- search for the highest lognumber --
        super.execute();

        ComputingContext ctx = new ComputingContext();
        ctx.highestNumber = newBaseNumber;
        for (File f : listFiles) {
            resetLognumbers(ctx, f, dryRun, backupFiles);
            assignLognumbers(ctx, f, dryRun, backupFiles);
        }
        getLog().info("Rebased [" + ctx.correctedStatements/2 + "] lognumbers in " + ctx.touchedFiles.size()/2
                + " file(s). Highest lognumber is now [" + ctx.highestNumber + "]");
    }
}
