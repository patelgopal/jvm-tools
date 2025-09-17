package org.example;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.example.bean.GcAnalyzer;
import org.example.bean.HeapDumpAnalyzer;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.jboss.resteasy.annotations.providers.multipart.PartType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

@ApplicationScoped
@Path("/")
@Produces(MediaType.TEXT_HTML)
public class FilesController {
    @Location("index.html")
    Template template;

    @Location("thread-index.html")
    Template report;

    @Inject
    HeapDumpAnalyzer heapDumpAnalyzer;

    @Inject
    GcAnalyzer gcAnalyzer;

    @ConfigProperty(name = "heap.location", defaultValue = "/home/jboss/heap/")
    String heapLocation;

    public static List<File> dirList = new ArrayList<>();

    private String homeLink = "<div style=\"" +
            "margin-bottom: 6px;\"><a href=\"/\"" +
            "style=\"font-weight:bold\">Home</a></div>";

    @GET
    public TemplateInstance get() {
        return template.instance();
    }

    @GET
    @Path("thread")
    public TemplateInstance getThread() {
        return report.instance();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_HTML)
    public Response upload(@MultipartForm FileUploadInput inputs) throws Exception {

        System.err.printf(">>>\n");
        System.out.printf("Analysis: %s\n", inputs.radiotype);
        System.out.printf("File: %s\n", inputs.inputStream.available());
        System.err.printf("\n<<<");

        String output = "";
        if (inputs.radiotype.equals("GC")) {
            output = gcAnalyzer.initGcDump(getFile(inputs.inputStream));
        } else if (inputs.radiotype.equals("HEAP")) {
            output = heapDumpAnalyzer.initHeapDump(getFile(inputs.inputStream));

            File outputFile = new File(output);
            File runDir = outputFile.getParentFile();

            dirList.add(runDir);
            addHomeLink(outputFile);
            
            //return Response.status(301).location(URI.create(output)).build();
            // redirect to results/<uuid>
            String uuid = runDir.getName();
            URI redirectUri = URI.create("/results/" + uuid);

            return Response.status(Response.Status.SEE_OTHER)
                        .location(redirectUri)
                        .build();
        }
        return Response.ok().entity(output).build();
    }

    public String getFile(InputStream inputStream) throws Exception {
        String uuid = UUID.randomUUID().toString();

        File dir = new File(heapLocation, uuid);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File targetFile = new File(dir, uuid);

        try (FileOutputStream fileOutputStream = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[131072];
            int bytes;
            while ((bytes = inputStream.read(buffer)) > 0) {
                fileOutputStream.write(buffer, 0, bytes);
            }
        }

        return targetFile.getAbsolutePath();
    }

    public static class FileUploadInput {
        @FormParam("text")
        public String text;

        @FormParam("file")
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        public InputStream inputStream;

        @FormParam("radiotype")
        public String radiotype;
    }

    public void addHomeLink(File mainDir) throws Exception {
        Arrays.stream(mainDir.listFiles()).forEach(file -> {
            if (file.isDirectory()) {
                try {
                    addHomeLink(file);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                if (file.getName().endsWith(".html")) {
                    //Document document = null;
                    try {
                        Document document = Jsoup.parse(file, "UTF-8");
                        document.body().children().get(0).before(homeLink);
                        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                            fileOutputStream.write(document.html().getBytes(StandardCharsets.UTF_8));
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
    }
}
