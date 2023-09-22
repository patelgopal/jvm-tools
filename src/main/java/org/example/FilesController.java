package org.example;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.example.bean.GcAnalyzer;
import org.example.bean.HeapDumpAnalyzer;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

import java.io.*;
import java.util.UUID;

@ApplicationScoped
@Path("files.html")
@Produces(MediaType.TEXT_HTML)
public class FilesController {
    @Location("files.html")
    Template template;

    @Location("report.html")
    Template report;

    @Inject
    HeapDumpAnalyzer heapDumpAnalyzer;

    @Inject
    GcAnalyzer gcAnalyzer;

    @GET
    public TemplateInstance get(){
        return template.instance();
    }

/*    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public TemplateInstance upload(Map<String, InputStream> inputs) throws IOException {
        for(Map.Entry<String,InputStream> entry: inputs.entrySet()){
            System.out.printf("Key: %s; Value: %s", entry.getKey(),new String(entry.getValue().readAllBytes()));
        }
        //System.out.printf("Input: %s", new String(inputStream.readAllBytes()));
        return report.instance();
    }*/

/*    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public TemplateInstance upload(@MultipartForm MultipartInput inputs) throws IOException {
        for (InputPart part : inputs.getParts()) {
            System.err.printf("Headers %s\n", part.getHeaders()
                    .getFirst("Content-Disposition"));
            System.err.printf("Body %s\n",part.getBodyAsString());
            part.getHeaders().containsValue("file");
        }
        return report.instance();
    }*/

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_HTML)
    public String upload(@MultipartForm FileUploadInput inputs) throws Exception {

        System.err.printf(">>>\n");
        System.out.printf("Analysis: %s\n",inputs.radiotype);
        System.out.printf("File: %s\n",inputs.inputStream.available());
        System.err.printf("\n<<<");

        /*
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(byteArrayOutputStream);
        PrintStream stdOut = System.out;
        System.setOut(printStream);
        Main.main(new String[]{inputs.file.getAbsolutePath(),"-c"});

        BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(byteArrayOutputStream.toByteArray())));
        StringBuffer stringBuffer = new StringBuffer();
        String line = "";
        while((line = br.readLine()) != null){
            stringBuffer.append(line);
            stringBuffer.append("<br>");
        }
        System.setOut(stdOut);
        return stringBuffer.toString();
        */
        String output = "";
        if(inputs.radiotype.equals("GC")){
            output = gcAnalyzer.initGcDump(getFile(inputs.inputStream));
        }else if (inputs.radiotype.equals("HEAP")){
            output = heapDumpAnalyzer.initHeapDump(getFile(inputs.inputStream));
        }
        return output;
    }

    public String getFile(InputStream inputStream) throws Exception {
        String uuid = UUID.randomUUID().toString();
        String name = "/home/jboss/heap/"+uuid;
        File file = new File(name);
        if(!file.exists()){
            file.mkdir();
        }
        try (FileOutputStream fileOutputStream = new FileOutputStream( new File(name+"/"+uuid))) {
            byte[] buffer = new byte[131072];
            int bytes;
            while ((bytes = inputStream.read(buffer)) > 0) {
                fileOutputStream.write(buffer, 0, bytes);
            }
            fileOutputStream.flush();
        }
        return name;
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
}
