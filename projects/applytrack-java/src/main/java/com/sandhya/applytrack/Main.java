package com.sandhya.applytrack;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;

public final class Main {
    public static void main(String[] raw) {
        try { run(raw); }
        catch(Exception e) { System.err.println("Error: "+e.getMessage()); System.exit(1); }
    }
    private static void run(String[] raw) throws Exception {
        List<String> args=new ArrayList<String>(Arrays.asList(raw));
        String path="data/applications.csv";
        if(!args.isEmpty() && args.get(0).equals("--data")) {
            if(args.size()<2) throw new IllegalArgumentException("--data needs a file path");
            path=args.remove(1); args.remove(0);
        }
        final TrackerService service=new TrackerService(new ApplicationStore(Paths.get(path)));
        String cmd=args.isEmpty()?"gui":args.get(0);
        if(cmd.equals("gui")) {
            size(args,0,1);
            if(GraphicsEnvironment.isHeadless()) throw new IllegalArgumentException("Desktop unavailable. Use list, add, update, due or summary.");
            service.list(); // Validate before opening the desktop.
            SwingUtilities.invokeLater(new Runnable(){public void run(){new TrackerFrame(service).setVisible(true);}});
        } else if(cmd.equals("add")) {
            size(args,4,6);
            Application a=service.add(args.get(1),args.get(2),LocalDate.parse(args.get(3)),
                args.size()>4?date(args.get(4)):null,args.size()>5?args.get(5):"");
            System.out.println("Added application #"+a.id+" for "+a.company);
        } else if(cmd.equals("update")) {
            size(args,5,5);
            service.update(Long.parseLong(args.get(1)),Status.valueOf(args.get(2).toUpperCase(Locale.ROOT)),date(args.get(3)),args.get(4));
            System.out.println("Updated application #"+args.get(1));
        } else if(cmd.equals("list")) { size(args,1,1); print(service.list()); }
        else if(cmd.equals("due")) { size(args,1,2); print(service.due(args.size()==2?LocalDate.parse(args.get(1)):LocalDate.now())); }
        else if(cmd.equals("summary")) {
            size(args,1,1);
            for(Map.Entry<Status,Integer> e:service.summary().entrySet()) System.out.println(e.getKey()+": "+e.getValue());
        } else if(cmd.equals("help")||cmd.equals("--help")) help();
        else throw new IllegalArgumentException("Unknown command. Run with help.");
    }
    static LocalDate date(String value) { return value.equals("-")||value.trim().isEmpty()?null:LocalDate.parse(value); }
    static void size(List<String> args,int min,int max) {
        if(args.size()<min||args.size()>max) throw new IllegalArgumentException("Wrong number of arguments. Run with help.");
    }
    static void print(List<Application> list) {
        System.out.println("ID | COMPANY | ROLE | STATUS | APPLIED | FOLLOW-UP | NOTES");
        for(Application a:list) System.out.println(a.id+" | "+a.company+" | "+a.role+" | "+a.status+" | "+a.appliedOn+" | "+(a.followUpOn==null?"-":a.followUpOn)+" | "+a.notes);
        System.out.println(list.size()+" application(s)");
    }
    static void help() {
        System.out.println("ApplyTrack - Java application tracker\n"+
            "java -jar applytrack.jar [--data FILE] gui\n"+
            "java -jar applytrack.jar [--data FILE] add COMPANY ROLE YYYY-MM-DD [FOLLOW-UP|-] [NOTES]\n"+
            "java -jar applytrack.jar [--data FILE] update ID STATUS FOLLOW-UP|- NOTES\n"+
            "java -jar applytrack.jar [--data FILE] list|summary\n"+
            "java -jar applytrack.jar [--data FILE] due [YYYY-MM-DD]\n"+
            "Statuses: APPLIED ASSESSMENT INTERVIEW OFFER REJECTED WITHDRAWN\n"+
            "Dates use YYYY-MM-DD. Quote arguments containing spaces.");
    }
}
