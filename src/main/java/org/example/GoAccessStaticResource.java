package org.example;

import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import io.quarkus.vertx.web.Route;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class GoAccessStaticResource {

    @ConfigProperty(name = "goaccess.location", defaultValue = "/tmp/goaccess")
    String goAccessLocation;

    @Route(path = "/goaccess/*", methods = Route.HttpMethod.GET)
    void serveReport(RoutingContext ctx) {
        // Strip "/goaccess" prefix from URI
        String path = ctx.request().uri().replaceFirst("/goaccess", "");
        if (path.isEmpty() || path.equals("/")) {
            path = "/report.html"; // default file
        }

        File file = new File(goAccessLocation + path);
        if (!file.exists() || file.isDirectory()) {
            ctx.response().setStatusCode(404).end("File not found: " + path);
            return;
        }

        try {
            byte[] bytes = Files.readAllBytes(file.toPath());

            // Determine content type
            String contentType = "text/html";
            String lower = file.getName().toLowerCase();
            if (lower.endsWith(".css")) contentType = "text/css";
            else if (lower.endsWith(".js")) contentType = "application/javascript";
            else if (lower.endsWith(".png")) contentType = "image/png";
            else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) contentType = "image/jpeg";
            else if (lower.endsWith(".gif")) contentType = "image/gif";
            else if (lower.endsWith(".ico")) contentType = "image/x-icon";

            // Serve file with no-cache headers to always get the latest version
            ctx.response()
               .putHeader("Content-Type", contentType)
               .putHeader("Cache-Control", "no-cache, no-store, must-revalidate")
               .putHeader("Pragma", "no-cache")
               .putHeader("Expires", "0")
               .end(Buffer.buffer(bytes));
        } catch (IOException e) {
            ctx.response().setStatusCode(500).end("Failed to read file: " + e.getMessage());
        }
    }
}
