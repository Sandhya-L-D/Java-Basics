package com.sandhya.authlog;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.time.Instant;
import java.util.*;

/** Skips malformed data rows with line numbers; rejects the wrong schema. */
public final class LogReader {
    public static final String HEADER="timestamp,source,user,result";
    public static final class Result {
        public final List<LoginEvent> events=new ArrayList<LoginEvent>();
        public final List<String> warnings=new ArrayList<String>();
        public int invalidRows;
    }
    public static Result read(Path path) throws IOException {
        Result out=new Result();
        try(BufferedReader reader=Files.newBufferedReader(path,StandardCharsets.UTF_8)) {
            String header=reader.readLine();
            if(header!=null && header.startsWith("\ufeff"))header=header.substring(1);
            if(!HEADER.equals(header))throw new IOException("Expected header: "+HEADER);
            String line;int lineNo=1;
            while((line=reader.readLine())!=null) {
                lineNo++;if(line.trim().isEmpty())continue;
                try {
                    List<String> f=Csv.parse(line);
                    if(f.size()!=4)throw new IllegalArgumentException("Expected 4 fields");
                    String result=f.get(3).trim().toUpperCase(Locale.ROOT);
                    if(!result.equals("SUCCESS")&&!result.equals("FAILURE"))throw new IllegalArgumentException("Result must be SUCCESS or FAILURE");
                    out.events.add(new LoginEvent(Instant.parse(f.get(0).trim()),f.get(1),f.get(2),result.equals("SUCCESS")));
                }catch(RuntimeException e){out.invalidRows++;if(out.warnings.size()<100)out.warnings.add("Line "+lineNo+": "+e.getMessage());}
            }
        }
        return out;
    }
}
