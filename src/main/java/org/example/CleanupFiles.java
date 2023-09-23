package org.example;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Iterator;

import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.scheduler.Scheduled;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped              
public class CleanupFiles {
    @ConfigProperty(name = "discard.files.older.than", defaultValue = "86400000")
    Long discardInterval;
    @Scheduled(every="${cleanup.duration:86400s}")
    void increment() throws IOException {
        Iterator<File> iterator = FilesController.dirList.iterator();
        System.out.println(">> Cleanup Scheduler: number of directories before cleanup: "+FilesController.dirList.size());
        while (iterator.hasNext()) {
            File file = iterator.next();
            long currentTime = System.currentTimeMillis();
            if (currentTime-file.lastModified() > discardInterval) {
                Files.walk(Path.of(file.getAbsolutePath()))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                System.out.println("Cleaning up the file " + file.getAbsolutePath() );
                iterator.remove();
                    System.out.println("Removed file ^^");
            }
        }
        System.out.println(">> Cleanup Scheduler: number of directories after cleanup: "+FilesController.dirList.size());
    }


}