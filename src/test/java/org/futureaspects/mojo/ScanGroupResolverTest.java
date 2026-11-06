package org.futureaspects.mojo;

import static org.futureaspects.mojo.TestSources.line;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScanGroupResolverTest {

    private static final LogNumberRange SHARED = new LogNumberRange(100, 200);
    private static final LogNumberRange OTHER = new LogNumberRange(200, 300);

    @TempDir
    Path dir;

    @Test
    void joinsModulesWithTheSameRange() throws Exception {
        List<ScanGroup> groups = resolve(true,
                module("a", SHARED, line("1;")),
                module("b", SHARED, line("2;")),
                module("c", OTHER, line("3;")));

        assertEquals(2, groups.size());
        assertEquals(List.of("a", "b"), groups.get(0).modules());
        assertEquals(2, groups.get(0).files().size());
        assertEquals(List.of("c"), groups.get(1).modules());
    }

    @Test
    void namesEveryModuleWithoutRange() throws Exception {
        MojoFailureException failure = assertThrows(MojoFailureException.class, () -> resolve(true,
                module("a", null, line("1;")),
                module("b", SHARED, line("2;")),
                module("c", null, line("3;"))));

        assertTrue(failure.getMessage().contains("a, c"), failure.getMessage());
    }

    @Test
    void modulesWithoutStatementsNeedNoRange() throws Exception {
        assertTrue(resolve(true, module("a", null, "class Empty {}\n")).isEmpty());
    }

    @Test
    void rejectsOverlappingRanges() {
        assertThrows(MojoFailureException.class, () -> resolve(true,
                module("a", SHARED, line("1;")),
                module("b", new LogNumberRange(150, 250), line("2;"))));
    }

    @Test
    void goalsWithoutRangeGetOneGroup() throws Exception {
        List<ScanGroup> groups = resolve(false, module("a", null, line("1;")), module("b", SHARED, line("2;")));

        assertEquals(1, groups.size());
        assertEquals("all", groups.get(0).name());
    }

    private List<ScanGroup> resolve(boolean rangeRequired, ModuleSources... modules)
            throws IOException, MojoFailureException {
        return new ScanGroupResolver(TestSources.store()).resolve(List.of(modules), rangeRequired);
    }

    private ModuleSources module(String name, LogNumberRange range, String content) throws IOException {
        Path root = dir.resolve(name);
        Files.createDirectories(root);
        Files.writeString(root.resolve("A.java"), content);
        return new ModuleSources(name, range, List.of(root));
    }
}
