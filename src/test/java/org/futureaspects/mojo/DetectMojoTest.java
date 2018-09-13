



package org.futureaspects.mojo;

import java.io.File;

import org.apache.commons.io.FileUtils;

public class DetectMojoTest {

//    @Test
    public void testScanFolder() {
        File baseDirectory = new File("/Users/marc/Projects/fa/maven/lognumber-maven-plugin");
        String filemask = "java";
        
        //log2.debug("1001;this is a test");
        //log2.debug("1002;this is a test");
        //log2.debug("1000;this is a test");
        FileUtils.listFiles(baseDirectory, new String[] { filemask }, true).forEach(f -> {
            System.out.println("processing " + f.getAbsolutePath());
        });
    }
}