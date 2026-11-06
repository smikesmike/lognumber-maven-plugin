package org.futureaspects.mojo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SourceFileStoreTest {

    private static final LogNumberRange RANGE = new LogNumberRange(100, 200);

    @TempDir
    Path dir;

    @Test
    void keepsLineEndingsAndMissingLastNewline() throws Exception {
        Path file = dir.resolve("A.java");
        Files.writeString(file, "a\r\nlog.info(\"0;x\");\r\nend");

        assertEquals("a\r\nlog.info(\"100;x\");\r\nend", assign(file));
    }

    @Test
    void keepsIsoEncoding() throws Exception {
        Path file = dir.resolve("A.java");
        byte[] original = "// ä\nlog.info(\"0;x\");\n".getBytes(StandardCharsets.ISO_8859_1);
        Files.write(file, original);

        assign(file);

        assertArrayEquals("// ä\nlog.info(\"100;x\");\n".getBytes(StandardCharsets.ISO_8859_1),
                Files.readAllBytes(file));
    }

    @Test
    void backupHoldsTheOriginal() throws Exception {
        Path file = dir.resolve("A.java");
        Files.writeString(file, "log.info(\"0;x\");");
        FileChange change = plan(file);

        SourceFileStore.write(change, ".bak");

        assertEquals("log.info(\"0;x\");", Files.readString(dir.resolve("A.java.bak")));
    }

    @Test
    void findSkipsMavenBuildDirectoryOnly() throws IOException {
        Files.writeString(dir.resolve("pom.xml"), "<project/>");
        Path generated = dir.resolve("target/generated/G.java");
        Path packageNamedTarget = dir.resolve("src/main/java/target/T.java");
        for (Path path : List.of(generated, packageNamedTarget)) {
            Files.createDirectories(path.getParent());
            Files.writeString(path, "");
        }

        assertEquals(Set.of(packageNamedTarget.toAbsolutePath().normalize()), Set.copyOf(TestSources.store().find(dir)));
    }

    @Test
    void findAcceptsMissingDirectories() throws IOException {
        assertTrue(TestSources.store().find(dir.resolve("missing")).isEmpty());
    }

    private String assign(Path file) throws IOException, MojoFailureException {
        SourceFileStore.write(plan(file), null);
        return Files.readString(file, StandardCharsets.ISO_8859_1);
    }

    private FileChange plan(Path file) throws IOException, MojoFailureException {
        SourceFile source = TestSources.store().read(file);
        ScanGroup group = new ScanGroup(RANGE, List.of("module"), List.of(source));
        return NumberingEngine.renumber(group, Set.copyOf(source.statements()), RANGE, 10).get(0);
    }
}
