package com.sandhya.authlog;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

public final class AllTests {
    static int passed=0;
    interface Task{void run()throws Exception;}
    static void test(String name,Task task)throws Exception{task.run();passed++;System.out.println("PASS "+name);}
    static void check(boolean value){if(!value)throw new AssertionError("Check failed");}
    static void fails(Task task)throws Exception{try{task.run();}catch(IllegalArgumentException|java.io.IOException e){return;}throw new AssertionError("Expected failure");}
    static final Instant BASE=Instant.parse("2026-11-18T09:00:00Z");
    static LoginEvent event(long second,String source,String user,boolean success){return new LoginEvent(BASE.plusSeconds(second),source,user,success);}
    static List<LoginEvent> failures(int count){List<LoginEvent> out=new ArrayList<LoginEvent>();for(int i=0;i<count;i++)out.add(event(i*10,"192.0.2.10","learner",false));return out;}
    static List<Alert> analyze(List<LoginEvent> rows){return new Analyzer(5,300).analyze(rows);}
    static Path folder;
    public static void main(String[] args)throws Exception{
        folder=Files.createTempDirectory("authlog-tests-");
        try{
            test("empty input produces no alerts",()->check(analyze(Collections.<LoginEvent>emptyList()).isEmpty()));
            test("below threshold produces no alert",()->check(analyze(failures(4)).isEmpty()));
            test("threshold produces one repeated-failure alert",()->{List<Alert>a=analyze(failures(5));check(a.size()==1&&a.get(0).type.equals("REPEATED_FAILURES")&&a.get(0).failures==5);});
            test("continued burst does not duplicate alerts",()->check(analyze(failures(8)).size()==1));
            test("window includes exactly 300 seconds",()->check(new Analyzer(2,300).analyze(Arrays.asList(event(0,"S","U",false),event(300,"S","U",false))).size()==1));
            test("event beyond window is expired",()->check(new Analyzer(2,300).analyze(Arrays.asList(event(0,"S","U",false),event(301,"S","U",false))).isEmpty()));
            test("fractional seconds beyond boundary are expired",()->check(new Analyzer(2,300).analyze(Arrays.asList(event(0,"S","U",false),new LoginEvent(BASE.plusSeconds(300).plusNanos(1),"S","U",false))).isEmpty()));
            test("success after burst produces review alert",()->{List<LoginEvent> e=failures(5);e.add(event(50,"192.0.2.10","learner",true));List<Alert>a=analyze(e);check(a.size()==2&&a.get(1).type.equals("SUCCESS_AFTER_FAILURES"));});
            test("success clears same-pair failure history",()->{List<LoginEvent> e=failures(5);e.add(event(50,"192.0.2.10","learner",true));e.add(event(60,"192.0.2.10","learner",false));check(analyze(e).size()==2);});
            test("different users are not merged",()->check(new Analyzer(2,300).analyze(Arrays.asList(event(0,"S","A",false),event(1,"S","B",false))).isEmpty()));
            test("different sources are not merged",()->check(new Analyzer(2,300).analyze(Arrays.asList(event(0,"A","U",false),event(1,"B","U",false))).isEmpty()));
            test("success for another user does not clear burst",()->{List<LoginEvent>e=failures(4);e.add(event(35,"192.0.2.10","other",true));e.add(event(40,"192.0.2.10","learner",false));check(analyze(e).size()==1);});
            test("out of order input is sorted without mutating caller",()->{List<LoginEvent>e=failures(5);Collections.reverse(e);Instant first=e.get(0).time;List<Alert>a=analyze(e);check(a.size()==1&&a.get(0).first.equals(BASE)&&e.get(0).time.equals(first));});
            test("equal timestamps retain file order",()->{List<LoginEvent>e=Arrays.asList(event(0,"S","U",false),event(0,"S","U",false),event(0,"S","U",true));check(new Analyzer(2,300).analyze(e).size()==2);});
            test("new burst after gap creates a new alert",()->{List<LoginEvent>e=failures(5);for(int i=0;i<5;i++)e.add(event(1000+i*10,"192.0.2.10","learner",false));check(analyze(e).size()==2);});
            test("invalid configuration rejected",()->{fails(()->new Analyzer(1,300));fails(()->new Analyzer(5,0));fails(()->new Analyzer(5,86401));});
            test("malformed rows skipped with line number",()->{Path p=folder.resolve("bad.csv");Files.write(p,Arrays.asList(LogReader.HEADER,"bad,S,U,FAILURE","2026-11-18T09:00:00Z,S,U,SUCCESS"),StandardCharsets.UTF_8);LogReader.Result r=LogReader.read(p);check(r.events.size()==1&&r.invalidRows==1&&r.warnings.get(0).startsWith("Line 2:"));});
            test("wrong schema rejected",()->{Path p=folder.resolve("head.csv");Files.write(p,Arrays.asList("wrong"),StandardCharsets.UTF_8);fails(()->LogReader.read(p));});
            test("unknown result and blank user rejected",()->{Path p=folder.resolve("fields.csv");Files.write(p,Arrays.asList(LogReader.HEADER,"2026-11-18T09:00:00Z,S,U,UNKNOWN","2026-11-18T09:00:00Z,S,,FAILURE"),StandardCharsets.UTF_8);check(LogReader.read(p).invalidRows==2);});
            test("CSV quoted fields round trip",()->{String text="test, \"user\"";check(Csv.parse(Csv.row("a",text,"b")).get(1).equals(text));});
            test("HTML report escapes untrusted identifiers",()->{LogReader.Result r=new LogReader.Result();r.events.add(event(0,"<script>alert(1)</script>","=SUM(1)",false));r.events.add(event(1,"<script>alert(1)</script>","=SUM(1)",false));Path out=folder.resolve("reports");ReportWriter.write(out,r,new Analyzer(2,300).analyze(r.events),2,300);String html=new String(Files.readAllBytes(out.resolve("report.html")),StandardCharsets.UTF_8);check(!html.contains("<script>")&&html.contains("&lt;script&gt;"));String csv=new String(Files.readAllBytes(out.resolve("alerts.csv")),StandardCharsets.UTF_8);check(csv.contains("'=SUM(1)"));});
            test("empty results still produce usable report files",()->{Path p=folder.resolve("empty-report");ReportWriter.write(p,new LogReader.Result(),Collections.<Alert>emptyList(),5,300);check(Files.exists(p.resolve("warnings.txt"))&&new String(Files.readAllBytes(p.resolve("report.html")),StandardCharsets.UTF_8).contains("No events met"));});
            System.out.println("AuthLog Analyzer: "+passed+" tests passed");
        }finally{try(java.util.stream.Stream<Path> paths=Files.walk(folder)){paths.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.delete(p);}catch(Exception e){throw new RuntimeException(e);}});}}
    }
}
