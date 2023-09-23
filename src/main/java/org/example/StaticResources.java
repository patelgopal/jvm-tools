package org.example;

import io.quarkus.runtime.StartupEvent;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.FileSystemAccess;
import io.vertx.ext.web.handler.StaticHandler;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;

public class StaticResources {
    @ConfigProperty(name = "heap.location", defaultValue = "/home/jboss/heap/")
    String heapLocation;
    void installRoute(@Observes StartupEvent startupEvent, Router router) {
        router.route()
                .path(heapLocation+"*")
                .handler(StaticHandler.create(FileSystemAccess.ROOT,heapLocation));
    }
}