package org.example.bean;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.*;
import  java.util.*;

@ApplicationScoped
public class HeapDumpAnalyzer {
    private StringBuffer stringBuffer = new StringBuffer();

    public String fileParse(InputStream inputStream) throws Exception {
        String uuid = UUID.randomUUID().toString();
        String name = "/home/jboss/heap/"+uuid;
        File file = new File(name);
        if(!file.exists()){
            file.mkdir();
        }
        FileOutputStream fileOutputStream = new FileOutputStream( new File(name+"/"+uuid));
        byte[] buffer = new byte[131072];
        int bytes;
        while((bytes = inputStream.read(buffer))> 0){
            fileOutputStream.write(buffer,0, bytes);
        }
        fileOutputStream.flush();
        return name;
    }

    public String initHeapDump(String file) throws Exception {
        String command = System.getenv("MAT_HOME")+"/ParseHeapDump.sh "+file+" -verbose -unzip org.eclipse.mat.api:overview";
        System.out.println(command);
        Process process = Runtime.getRuntime().exec(command);
        return file;
    }



}
