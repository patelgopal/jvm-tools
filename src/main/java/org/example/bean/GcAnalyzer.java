package org.example.bean;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipselabs.garbagecat.Main;

import java.io.*;

@ApplicationScoped
public class GcAnalyzer {

    public String initGcDumpRaw(String path) throws IOException {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (PrintStream ps = new PrintStream(byteArrayOutputStream, true);
             BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(byteArrayOutputStream.toByteArray())))) {

            // Redirect stdout
            System.setOut(ps);

            // Run GarbageCat
            Main.main(new String[]{path, "-c"});

        } catch (Exception e) {
            throw new IOException("Failed to analyze GC log", e);
        } finally {
            // Always restore original System.out
            System.setOut(originalOut);
        }

        // Convert captured output to string
        return byteArrayOutputStream.toString();
    }

    public String initGcDump(String path) throws IOException {
        String rawOutput = initGcDumpRaw(path);
        return rawOutput.replace(System.lineSeparator(), "<br>");
    }
}
