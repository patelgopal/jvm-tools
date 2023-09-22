package org.example.bean;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipselabs.garbagecat.Main;

import java.io.*;

@ApplicationScoped
public class GcAnalyzer {
    public String initGcDump(String path) throws IOException{
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(byteArrayOutputStream);
        PrintStream stdOut = System.out;
        System.setOut(printStream);
        Main.main(new String[]{ path,"-c"});

        BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(byteArrayOutputStream.toByteArray())));
        StringBuffer stringBuffer = new StringBuffer();
        String line = "";
        while((line = br.readLine()) != null){
            stringBuffer.append(line);
            stringBuffer.append("<br>");
        }
        System.setOut(stdOut);
        return stringBuffer.toString();
    }
}
