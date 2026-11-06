package org.futureaspects.mojo;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;

/**
 * A scanned source file.
 *
 * @param path       the file
 * @param charset    the charset the file was decoded with
 * @param content    the decoded text
 * @param statements the logging statements in order of appearance
 */
record SourceFile(Path path, Charset charset, String content, List<LogStatement> statements) {
}
