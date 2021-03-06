/**
 * Copyright Futureaspects(c) 2018
 * 
 * Project: lognumber-maven-plugin
 *  
 * Created on 13.09.2018
 */
package org.futureaspects.mojo;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Searches for the highest lognumber and adds a lognumber to all missing
 * positions or statements begining with "0;"
 *
 */
@Mojo(name = "assign", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresProject = false)
public class AssignMojo extends DetectMojo {

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

        getLog().info("Assigning missing lognumbers...");

        // -- search for the highest lognumber --
        super.execute();

        getLog().info("Computing new lognumbers...");
        ComputingContext ctx = new ComputingContext();
        ctx.highestNumber = highestLognumber;
        for (File f : listFiles) {
            assignLognumbers(ctx, f, dryRun, backupFiles);
        }
        getLog().info("Corrected [" + ctx.correctedStatements + "] lognumbers in " + ctx.touchedFiles.size()
                + " file(s). Highest lognumber is [" + ctx.highestNumber + "]");

    }
}
