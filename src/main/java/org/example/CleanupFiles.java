package org.example;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Iterator;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.scheduler.Scheduled;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class CleanupFiles {

    @ConfigProperty(name = "discard.files.older.than", defaultValue = "86400000") // 1 day in milliseconds
    Long discardInterval;

    private static final Logger LOG = LoggerFactory.getLogger(CleanupFiles.class);

    @Scheduled(every = "${cleanup.duration:86400s}")
    void cleanupOldFiles() {
        long now = System.currentTimeMillis();
        LOG.info("Cleanup Scheduler: {} directories before cleanup", FilesController.dirList.size());

        Iterator<File> iterator = FilesController.dirList.iterator();
        while (iterator.hasNext()) {
            File file = iterator.next();
            long age = now - file.lastModified();

            if (age > discardInterval) {
                try (Stream<Path> paths = Files.walk(file.toPath())) {
                    paths.sorted(Comparator.reverseOrder())
                         .map(Path::toFile)
                         .forEach(f -> {
                             if (!f.delete()) {
                                 LOG.warn("Failed to delete {}", f.getAbsolutePath());
                             }
                         });
                    iterator.remove();
                    LOG.info("Cleaned up and removed {}", file.getAbsolutePath());
                } catch (IOException e) {
                    LOG.error("Error while cleaning up {}", file.getAbsolutePath(), e);
                }
            }
        }
        LOG.info("Cleanup Scheduler: {} directories after cleanup", FilesController.dirList.size());
    }
}
