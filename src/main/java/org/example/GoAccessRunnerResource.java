package org.example;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.*;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import java.io.*;
import java.nio.file.*;
import java.util.UUID;

@Path("/goaccess")
@ApplicationScoped
public class GoAccessRunnerResource {

    @ConfigProperty(name = "goaccess.container.runtime", defaultValue = "podman")
    String runtime;

    @ConfigProperty(name = "goaccess.image", defaultValue = "allinurl/goaccess")
    String image;

    @ConfigProperty(name = "goaccess.port", defaultValue = "7890")
    int port;

    @ConfigProperty(name = "goaccess.report.dir", defaultValue = "/tmp/goaccess")
    String reportDir;
    
    @POST
    @Path("/run")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_HTML)
    public Response runGoAccess(MultipartFormDataInput input) {

        try {
            // Extract the uploaded file
            InputPart filePart = input.getFormDataMap().get("file").get(0);
            String fileName = input.getFormDataMap().get("fileName").get(0).getBodyAsString();

            // Prepare a unique folder for this run
            String uuid = UUID.randomUUID().toString();
            File runFolder = new File(reportDir, uuid);
            runFolder.mkdirs();

            // Save uploaded file
            File uploadedFile = new File(runFolder, fileName);
            try (InputStream in = filePart.getBody(InputStream.class, null);
                 OutputStream out = new FileOutputStream(uploadedFile)) {
                in.transferTo(out);
            }

            // Prepare GoAccess output file
            @SuppressWarnings("unused")
            File reportFile = new File(runFolder, "report.html");

            // Build runtime command dynamically
            String[] cmd = {
                runtime, "run", "--rm", "-i",
                    "-v", uploadedFile.getAbsolutePath() + ":/srv/log/" + fileName,
                    "-v", runFolder.getAbsolutePath() + ":/srv/report",
                    "-p", port + ":" + port,
                    image,
                    "/srv/log/" + fileName,
                    "--log-format=COMBINED",
                    "-o", "/srv/report/report.html",
                    "--ws-url=ws://localhost:" + port
            };

            ProcessBuilder pb = new ProcessBuilder(cmd);            // Build Podman command

            pb.inheritIO();
            pb.start();

            // Wait a short time for report.html to be generated (adjust if needed)
            Thread.sleep(3000);

            // Return an HTML page with link to report
            String reportLink = "/goaccess/" + uuid + "/report.html";
            String html = "<html><body>" +
                    "<p>GoAccess report ready:</p>" +
                    "<a href='" + reportLink + "' target='_blank'>Open Report</a>" +
                    "</body></html>";

            return Response.ok(html).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500).entity("Failed to run GoAccess: " + e.getMessage()).build();
        }
    }

    // Serve static GoAccess files
    @GET
    @Path("/report/{uuid}/{fileName}")
    public Response serveReport(@PathParam("uuid") String uuid,
                                @PathParam("fileName") String fileName) {
        File file = new File(reportDir + "/" + uuid, fileName);
        if (!file.exists()) {
            return Response.status(404).entity("File not found").build();
        }
        String contentType = "text/html";
        if (fileName.endsWith(".css")) contentType = "text/css";
        else if (fileName.endsWith(".js")) contentType = "application/javascript";

        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            return Response.ok(bytes, contentType)
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .header("Pragma", "no-cache")
                    .header("Expires", "0")
                    .build();
        } catch (IOException e) {
            return Response.status(500).entity("Failed to read file").build();
        }
    }
}
