package com.sandhya.authlog;
import java.nio.file.*;
import java.util.*;

public final class Main {
    public static void main(String[] args) {
        try{run(args);}catch(Exception e){System.err.println("Error: "+e.getMessage());System.exit(1);}
    }
    static void run(String[] args)throws Exception{
        if(args.length==1&&(args[0].equals("--help")||args[0].equals("help"))){help();return;}
        if(args.length>4)throw new IllegalArgumentException("Too many arguments. Run with --help.");
        Path input=Paths.get(args.length>0?args[0]:"samples/login-events.csv").toAbsolutePath().normalize();
        Path out=Paths.get(args.length>1?args[1]:"output").toAbsolutePath().normalize();
        for(String name:Arrays.asList("alerts.csv","warnings.txt","report.html"))
            if(input.equals(out.resolve(name)))throw new IllegalArgumentException("Output would overwrite the input file");
        int threshold=args.length>2?Integer.parseInt(args[2]):5;
        long seconds=args.length>3?Long.parseLong(args[3]):300;
        Analyzer analyzer=new Analyzer(threshold,seconds);
        LogReader.Result result=LogReader.read(input);
        List<Alert> alerts=analyzer.analyze(result.events);
        ReportWriter.write(out,result,alerts,threshold,seconds);
        System.out.println("AuthLog Analyzer\nValid events: "+result.events.size()+"\nInvalid rows skipped: "+result.invalidRows+"\nAlerts: "+alerts.size());
        for(Alert a:alerts)System.out.println(a.type+" | "+a.source+" | "+a.user+" | failures="+a.failures+" | "+a.last);
        System.out.println("Report: "+out.resolve("report.html"));
    }
    static void help(){System.out.println("java -jar authlog-analyzer.jar [INPUT.csv] [OUTPUT_DIRECTORY] [THRESHOLD=5] [WINDOW_SECONDS=300]\nNo arguments runs the synthetic sample. Header: timestamp,source,user,result\nTimestamp: ISO 8601 with timezone, e.g. 2026-11-18T09:00:00Z. Result: SUCCESS or FAILURE.");}
}
