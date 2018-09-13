/**
 * Copyright Futureaspects(c) 2018
 * 
 * Project: lognumber-maven-plugin
 *  
 * Created on 13.09.2018
 */
package org.futureaspects.mojo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Context which is passed during file processing
 *
 * @author <a href="mailto:marc.wilhelm@futureaspects.com">Marc Wilhelm</a>
 *
 */
class ComputingContext {
    int highestNumber = 0;
    int correctedStatements = 0;
    List<File> touchedFiles = new ArrayList<>();
}