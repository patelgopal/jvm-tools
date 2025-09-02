package org.example;

import io.quarkus.runtime.StartupEvent;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.FileSystemAccess;
import io.vertx.ext.web.handler.StaticHandler;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StaticResources {

    private static final Logger LOG = LoggerFactory.getLogger(StaticResources.class);

    @ConfigProperty(name = "heap.location", defaultValue = "/home/jboss/heap/")
    String heapLocation;

    @ConfigProperty(name = "heap.route", defaultValue = "/heap/*")
    String heapRoute;

    void registerHeapRoute(@Observes StartupEvent startupEvent, Router router) {
        LOG.info("Registering static resource handler: {} -> {}", heapRoute, heapLocation);

        StaticHandler handler = StaticHandler.create(FileSystemAccess.ROOT, heapLocation)
                .setCachingEnabled(false)       // no caching for sensitive dumps
                .setDirectoryListing(false)     // prevent directory browsing
                .setIncludeHidden(false);       // no hidden files

        router.route(heapRoute).handler(handler);
    }
}
