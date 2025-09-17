package org.example;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.Arrays;

@Path("/results")
public class ResultsResource {

    @ConfigProperty(name = "heap.location", defaultValue = "/home/jboss/heap/")
    String heapLocation;

    /**
     * Redirect /results/<uuid> to the main report index.html
     */
    @GET
    @Path("/{id}")
    public Response redirectToMainReport(@PathParam("id") String id) {
        File runDir = new File(heapLocation, id);
        if (!runDir.exists() || !runDir.isDirectory()) {
            return Response.status(404)
                    .entity("No results found for " + id)
                    .build();
        }

        // Try to find the first subfolder containing index.html
        File[] subDirs = runDir.listFiles(File::isDirectory);
        if (subDirs != null && subDirs.length > 0) {
            File mainReport = new File(subDirs[0], "index.html");
            if (mainReport.exists()) {
                URI redirectUri = URI.create("/results/" + id + "/" + subDirs[0].getName() + "/index.html");
                return Response.status(303).location(redirectUri).build();
            }
        }

        // Fallback: check for index.html directly under runDir
        File indexFile = new File(runDir, "index.html");
        if (indexFile.exists()) {
            URI redirectUri = URI.create("/results/" + id + "/index.html");
            return Response.status(303).location(redirectUri).build();
        }

        return Response.status(404)
                .entity("No main report found for " + id)
                .build();
    }

    /**
     * Serve any file under /results/<uuid>/... dynamically
     */
    @GET
    @Path("/{id}/{path: .*}") // regex to capture nested paths
    public Response serveFile(@PathParam("id") String id,
                              @PathParam("path") String path) throws IOException {

        File baseDir = new File(heapLocation, id);
        File target = path.isEmpty() ? baseDir : new File(baseDir, path);

        if (!target.exists()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("<h2>Not found: " + path + "</h2>")
                    .build();
        }

        if (target.isDirectory()) {
            // Simple listing page
            StringBuilder html = new StringBuilder();
            html.append("<html><head><title>Listing for ").append(path).append("</title></head><body>");
            html.append("<h1>Contents of ").append(path.isEmpty() ? id : path).append("</h1>");
            html.append("<p><a href=\"/results/").append(id).append("\">&larr; Back to Results Home</a></p>");
            html.append("<ul>");
            Arrays.stream(target.listFiles())
                    .forEach(file -> {
                        String relativePath = path.isEmpty() ? file.getName() : path + "/" + file.getName();
                        html.append("<li><a href=\"/results/").append(id).append("/").append(relativePath)
                                .append("\">").append(file.getName()).append("</a></li>");
                    });
            html.append("</ul></body></html>");
            return Response.ok(html.toString(), "text/html").build();
        } else {
            // Serve a file with proper MIME type
            String mimeType = Files.probeContentType(target.toPath());
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            return Response.ok(target, mimeType).build();
        }
    }

}
