package com.trcm.qa.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Optional;
import java.util.stream.Stream;

/** Waits for a browser download to finish (ignores Chrome's temporary .crdownload files). */
public final class FileWaitUtil {

    private FileWaitUtil() {
    }

    public static Path waitForDownload(String dir, String prefix, String extension, Duration timeout)
            throws IOException, InterruptedException {
        long end = System.currentTimeMillis() + timeout.toMillis();
        Path folder = Paths.get(dir);
        while (System.currentTimeMillis() < end) {
            if (Files.exists(folder)) {
                try (Stream<Path> files = Files.list(folder)) {
                    Optional<Path> match = files
                            .filter(p -> p.getFileName().toString().startsWith(prefix))
                            .filter(p -> p.getFileName().toString().endsWith(extension))
                            .findFirst();
                    if (match.isPresent() && Files.size(match.get()) > 0) {
                        return match.get();
                    }
                }
            }
            Thread.sleep(250);
        }
        throw new AssertionError("No download matching " + prefix + "*" + extension + " in " + dir + " after " + timeout);
    }

    public static void cleanDirectory(String dir) throws IOException {
        Path folder = Paths.get(dir);
        Files.createDirectories(folder);
        try (Stream<Path> files = Files.list(folder)) {
            for (Path p : (Iterable<Path>) files::iterator) {
                Files.deleteIfExists(p);
            }
        }
    }
}
