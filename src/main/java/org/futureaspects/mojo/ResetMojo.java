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
 * Resets all logging statements to start with "0;"
 *
 */
@Mojo(name = "reset", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresProject = false)
public class ResetMojo extends DetectMojo {

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

        getLog().info("Resetting lognumbers to \"0\"...");

        // -- search for the highest lognumber --
        super.execute();

        getLog().info("resetting existing lognumbers to \"0\"...");
        ComputingContext ctx = new ComputingContext();
        ctx.highestNumber = highestLognumber;
        for (File f : listFiles) {
            resetLognumbers(ctx, f, dryRun, backupFiles);
        }
        getLog().info("Resetted [" + ctx.correctedStatements + "] lognumbers in " + ctx.touchedFiles.size()
                + " file(s). Highest lognumber was [" + ctx.highestNumber + "]");
    }
}
