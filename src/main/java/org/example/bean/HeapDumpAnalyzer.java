package org.example.bean;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@ApplicationScoped
public class HeapDumpAnalyzer {

    @ConfigProperty(name = "heap.location", defaultValue = "/home/jboss/heap/")
    String heapLocation;

    private static final Logger LOG = LoggerFactory.getLogger(HeapDumpAnalyzer.class);
    private static final int BUFFER_SIZE = 128 * 1024; // 128 KB

    public String fileParse(InputStream inputStream) throws Exception {
        String uuid = UUID.randomUUID().toString();
        Path dir = Paths.get(heapLocation, uuid);
        Files.createDirectories(dir);

        Path filePath = dir.resolve(uuid);
        try (OutputStream fileOutputStream = Files.newOutputStream(filePath)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) > 0) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }
        }
        LOG.info("Heap dump written to {}", filePath);
        return dir.toString();
    }

    public String initHeapDump(String file) throws IOException, InterruptedException {
        String matHome = System.getenv("MAT_HOME");
        if (matHome == null) {
            throw new IllegalStateException("MAT_HOME environment variable not set");
        }

        List<String> command = Arrays.asList(
                Paths.get(matHome, "ParseHeapDump.sh").toString(),
                file,
                "-verbose",
                "-unzip",
                "org.eclipse.mat.api:overview"
        );

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true); // merge stdout & stderr
        Process process = pb.start();

        String output;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            output = reader.lines().reduce("", (acc, line) -> acc + line + "\n");
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            LOG.error("Heap dump analysis failed with exit code {}: {}", exitCode, output);
            throw new RuntimeException("Heap dump analysis failed");
        }

        LOG.info("Heap dump analysis completed:\n{}", output);
        return file + "_System_Overview/";
    }
}
