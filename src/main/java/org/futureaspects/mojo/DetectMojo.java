/**
 * Copyright Futureaspects(c) 2018
 * 
 * Project: lognumber-maven-plugin
 *  
 * Created on 13.09.2018
 */
package org.futureaspects.mojo;

import java.io.File;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Detects the highest lognumber in all given files
 *
 */
@Mojo(name = "detect", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresProject = false)
public class DetectMojo extends LognumberBaseMojo {
    int highestLognumber;
    Collection<File> listFiles;

    public void execute() throws MojoExecutionException {

        if (!baseDirectory.exists()) {
            throw new MojoExecutionException("Missing baseDir: '" + baseDirectory + "'");
        }

        getLog().info("Scanning folder '" + baseDirectory + "' for files with extension '*." + fileExtension + "' ...");

        highestLognumber = 0;
        listFiles = FileUtils.listFiles(baseDirectory, new String[] { fileExtension }, true);
        for (File f : listFiles) {
            highestLognumber = Math.max(highestLognumber, seekHighestNumber(f));
        }
        getLog().info("Detected [" + highestLognumber + "] as highest lognumber");

    }
}
