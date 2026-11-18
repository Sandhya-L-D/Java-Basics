package com.sandhya.applytrack;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

public final class AllTests {
    static int passed=0;
    interface Task{void run()throws Exception;}
    static void test(String name,Task task)throws Exception{task.run();passed++;System.out.println("PASS "+name);}
    static void check(boolean value){if(!value)throw new AssertionError("Check failed");}
    static void fails(Task task)throws Exception{try{task.run();}catch(IllegalArgumentException|java.io.IOException e){return;}throw new AssertionError("Expected validation failure");}
    static final LocalDate DAY=LocalDate.of(2026,11,18);
    static Application record(long id,String company){return new Application(id,company,"Java Intern",DAY,Status.APPLIED,DAY.plusDays(2),"Sample");}
    static Path folder;
    static ApplicationStore store(String name){return new ApplicationStore(folder.resolve(name));}
    public static void main(String[] args)throws Exception{
        folder=Files.createTempDirectory("applytrack-tests-");
        try{
            test("new store is empty",()->check(store("missing.csv").load().isEmpty()));
            test("add survives a new service instance",()->{
                TrackerService s=new TrackerService(store("restart.csv"));s.add("Demo","Java Intern",DAY,DAY.plusDays(1),"Ready");
                List<Application> rows=new TrackerService(store("restart.csv")).list();check(rows.size()==1&&rows.get(0).company.equals("Demo"));
            });
            test("IDs advance from the greatest stored ID",()->{
                ApplicationStore p=store("ids.csv");p.save(Arrays.asList(record(8,"A"),record(2,"B")));
                check(new TrackerService(p).add("C","Role",DAY,null,"").id==9);
            });
            test("commas quotes and Unicode survive CSV round trip",()->{
                ApplicationStore p=store("quoted.csv");String name="Mysuru, \"R&D\" - ಕನ್ನಡ";p.save(Arrays.asList(record(1,name)));
                check(p.load().get(0).company.equals(name));
            });
            test("empty optional fields survive",()->{ApplicationStore p=store("optional.csv");p.save(Arrays.asList(new Application(1,"A","B",DAY,Status.APPLIED,null,"")));check(p.load().get(0).followUpOn==null);});
            test("blank company rejected",()->fails(()->record(1,"  ")));
            test("multiline text rejected",()->fails(()->record(1,"A\nB")));
            test("follow-up before application rejected",()->fails(()->new Application(1,"A","B",DAY,Status.APPLIED,DAY.minusDays(1),"")));
            test("status update persists",()->{TrackerService s=new TrackerService(store("update.csv"));s.add("A","B",DAY,null,"");s.update(1,Status.INTERVIEW,DAY.plusDays(4),"Technical interview");check(s.list().get(0).status==Status.INTERVIEW&&s.list().get(0).notes.equals("Technical interview"));});
            test("unknown update does not alter existing file",()->{ApplicationStore p=store("unknown.csv");p.save(Arrays.asList(record(1,"A")));byte[] before=Files.readAllBytes(Paths.get(p.location()));fails(()->new TrackerService(p).update(99,Status.OFFER,null,""));check(Arrays.equals(before,Files.readAllBytes(Paths.get(p.location()))));});
            test("due filter includes the boundary date",()->check(record(1,"A").isDue(DAY.plusDays(2))));
            test("future follow-up excluded",()->check(!record(1,"A").isDue(DAY.plusDays(1))));
            test("closed statuses excluded from follow-ups",()->{for(Status s:Arrays.asList(Status.OFFER,Status.REJECTED,Status.WITHDRAWN))check(!record(1,"A").update(s,DAY,"").isDue(DAY.plusDays(5)));});
            test("follow-ups sorted oldest first",()->{ApplicationStore p=store("due.csv");p.save(Arrays.asList(record(1,"A"),record(2,"B").update(Status.ASSESSMENT,DAY,"")));check(new TrackerService(p).due(DAY.plusDays(4)).get(0).id==2);});
            test("summary includes zero counts",()->{ApplicationStore p=store("summary.csv");p.save(Arrays.asList(record(1,"A"),record(2,"B").update(Status.OFFER,null,"")));Map<Status,Integer> m=new TrackerService(p).summary();check(m.get(Status.APPLIED)==1&&m.get(Status.OFFER)==1&&m.get(Status.INTERVIEW)==0);});
            test("malformed header rejected",()->{Path p=folder.resolve("badhead.csv");Files.write(p,Arrays.asList("wrong"),StandardCharsets.UTF_8);fails(()->new ApplicationStore(p).load());});
            test("corrupt row blocks writes and preserves original bytes",()->{Path p=folder.resolve("corrupt.csv");Files.write(p,Arrays.asList(ApplicationStore.HEADER,"1,A,B,wrong,APPLIED,,"),StandardCharsets.UTF_8);byte[] before=Files.readAllBytes(p);fails(()->new TrackerService(new ApplicationStore(p)).add("C","D",DAY,null,""));check(Arrays.equals(before,Files.readAllBytes(p)));});
            test("duplicate stored IDs rejected",()->{Path p=folder.resolve("duplicates.csv");Files.write(p,Arrays.asList(ApplicationStore.HEADER,"1,A,B,2026-11-18,APPLIED,,","1,C,D,2026-11-18,APPLIED,,"),StandardCharsets.UTF_8);fails(()->new ApplicationStore(p).load());});
            test("unterminated CSV quotes rejected",()->fails(()->Csv.parse("\"unfinished")));
            test("invalid character after CSV quote rejected",()->fails(()->Csv.parse("\"A\"x,B")));
            System.out.println("ApplyTrack: "+passed+" tests passed");
        }finally{
            try(java.util.stream.Stream<Path> paths=Files.walk(folder)){paths.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.delete(p);}catch(Exception e){throw new RuntimeException(e);}});}
        }
    }
}
