package org.example;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import java.io.*;
import java.io.IOException;
import java.io.InputStream;
//import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Filter implements ContainerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(Filter.class);
    private static final int MAX_BODY_SIZE = 1024 * 1024; // 1 MB limit for safety

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        InputStream originalStream = requestContext.getEntityStream();

        byte[] data = readLimited(originalStream, MAX_BODY_SIZE);
        requestContext.setEntityStream(new ByteArrayInputStream(data));

        if (LOG.isDebugEnabled()) {
            String body = new String(data, java.nio.charset.StandardCharsets.UTF_8);
            LOG.debug("Request body: {}", body);
        }
    }

    private byte[] readLimited(InputStream in, int maxBytes) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] tmp = new byte[4096];
        int read;
        int total = 0;
        while ((read = in.read(tmp)) != -1) {
            total += read;
            if (total > maxBytes) {
                throw new IOException("Request body too large (" + total + " bytes, max " + maxBytes + ")");
            }
            buffer.write(tmp, 0, read);
        }
        return buffer.toByteArray();
    }
}
