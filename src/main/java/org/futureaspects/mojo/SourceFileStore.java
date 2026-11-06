package org.futureaspects.mojo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Finds, reads and writes source files without changing anything but the given text.
 */
final class SourceFileStore {

    private final String extensionSuffix;
    private final LogStatementParser parser;

    /**
     * @param fileExtension extension of the files to process, without the dot
     * @param parser        parser for the file content
     */
    SourceFileStore(String fileExtension, LogStatementParser parser) {
        this.extensionSuffix = "." + fileExtension;
        this.parser = parser;
    }

    /**
     * Lists the files below a directory. Maven build directories are skipped.
     *
     * @param root the directory, which does not need to exist
     * @return the absolute, normalized file paths
     * @throws IOException if a directory cannot be read
     */
    List<Path> find(Path root) throws IOException {
        List<Path> files = new ArrayList<>();
        if (!Files.isDirectory(root)) {
            return files;
        }
        Files.walkFileTree(root.toAbsolutePath().normalize(), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                return isBuildDirectory(dir) ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.getFileName().toString().endsWith(extensionSuffix)) {
                    files.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return files;
    }

    private static boolean isBuildDirectory(Path dir) {
        return dir.endsWith("target") && Files.isRegularFile(dir.resolveSibling("pom.xml"));
    }

    /**
     * Reads a file as strict UTF-8 and falls back to ISO-8859-1.
     *
     * @param path the file
     * @return the file with its logging statements
     * @throws IOException if the file cannot be read
     */
    SourceFile read(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        try {
            String content = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
            return new SourceFile(path, StandardCharsets.UTF_8, content, parser.parse(path, content));
        } catch (CharacterCodingException e) {
            String content = new String(bytes, StandardCharsets.ISO_8859_1);
            return new SourceFile(path, StandardCharsets.ISO_8859_1, content, parser.parse(path, content));
        }
    }

    /**
     * Writes a change using the charset the file was read with.
     *
     * @param change          the change to write
     * @param backupExtension extension of the backup copy, {@code null} to write no backup
     * @throws IOException if the file cannot be written
     */
    static void write(FileChange change, String backupExtension) throws IOException {
        Path path = change.file().path();
        if (backupExtension != null) {
            Files.copy(path, path.resolveSibling(path.getFileName() + backupExtension),
                    StandardCopyOption.REPLACE_EXISTING);
        }
        Files.writeString(path, change.content(), change.file().charset());
    }
}
