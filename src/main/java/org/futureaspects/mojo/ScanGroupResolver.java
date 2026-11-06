package org.futureaspects.mojo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoFailureException;

/**
 * Scans modules and joins those with an identical range, so that their numbers are checked together.
 */
final class ScanGroupResolver {

    private final SourceFileStore store;

    ScanGroupResolver(SourceFileStore store) {
        this.store = store;
    }

    /**
     * @param modules      the modules to scan
     * @param rangeRequired whether modules containing logging statements need a range
     * @return one group per range
     * @throws IOException          if a file cannot be read
     * @throws MojoFailureException if a required range is missing or ranges overlap
     */
    List<ScanGroup> resolve(List<ModuleSources> modules, boolean rangeRequired)
            throws IOException, MojoFailureException {
        Map<LogNumberRange, Set<Path>> filesByRange = new LinkedHashMap<>();
        Map<LogNumberRange, List<String>> namesByRange = new LinkedHashMap<>();
        Map<Path, SourceFile> filesByPath = new LinkedHashMap<>();
        List<String> withoutRange = new ArrayList<>();

        for (ModuleSources module : modules) {
            List<SourceFile> files = scan(module, filesByPath);
            if (files.isEmpty()) {
                continue;
            }
            if (rangeRequired && module.range() == null) {
                withoutRange.add(module.name());
                continue;
            }
            LogNumberRange range = rangeRequired ? module.range() : null;
            files.forEach(file -> filesByRange.computeIfAbsent(range, r -> new LinkedHashSet<>()).add(file.path()));
            namesByRange.computeIfAbsent(range, r -> new ArrayList<>()).add(module.name());
        }

        if (!withoutRange.isEmpty()) {
            throw new MojoFailureException("Property " + LogNumberRange.PROPERTY + " is missing in the pom.xml of: "
                    + String.join(", ", withoutRange) + ". Example: <" + LogNumberRange.PROPERTY + ">20_000,30_000</"
                    + LogNumberRange.PROPERTY + ">");
        }
        if (rangeRequired) {
            rejectOverlaps(new ArrayList<>(filesByRange.keySet()));
        }

        List<ScanGroup> groups = new ArrayList<>();
        filesByRange.forEach((range, paths) -> groups.add(new ScanGroup(range, namesByRange.get(range),
                paths.stream().sorted().map(filesByPath::get).toList())));
        return groups;
    }

    private List<SourceFile> scan(ModuleSources module, Map<Path, SourceFile> filesByPath) throws IOException {
        List<SourceFile> files = new ArrayList<>();
        for (Path root : module.roots()) {
            for (Path path : store.find(root)) {
                SourceFile file = filesByPath.containsKey(path) ? filesByPath.get(path) : store.read(path);
                if (!file.statements().isEmpty()) {
                    filesByPath.put(path, file);
                    files.add(file);
                }
            }
        }
        files.sort(Comparator.comparing(SourceFile::path));
        return files;
    }

    private static void rejectOverlaps(List<LogNumberRange> ranges) throws MojoFailureException {
        for (int i = 0; i < ranges.size(); i++) {
            for (int j = i + 1; j < ranges.size(); j++) {
                if (ranges.get(i).overlaps(ranges.get(j))) {
                    throw new MojoFailureException(ranges.stream().map(LogNumberRange::toString)
                            .collect(Collectors.joining("], [", "Overlapping lognumber ranges: [", "]")));
                }
            }
        }
    }
}
