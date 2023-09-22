package org.example;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class Filter implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        try {
            InputStream is = requestContext.getEntityStream();
            byte[] data = is.readAllBytes();
            String body = new String(data, StandardCharsets.UTF_8);
            requestContext.setEntityStream(new ByteArrayInputStream(data));
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
