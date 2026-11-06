package org.futureaspects.mojo;

import java.nio.file.Path;
import java.util.List;

/**
 * The source locations of a module.
 *
 * @param name  the name shown to the user
 * @param range the configured range, {@code null} if there is none
 * @param roots the directories to scan
 */
record ModuleSources(String name, LogNumberRange range, List<Path> roots) {
}
