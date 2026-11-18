package com.sandhya.applytrack;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

/** Business operations shared by the console and Swing interfaces. */
public final class TrackerService {
    private final ApplicationStore store;
    public TrackerService(ApplicationStore store) { this.store=store; }
    public List<Application> list() throws IOException { return store.load(); }
    public Application add(String company,String role,LocalDate applied,LocalDate followUp,String notes) throws IOException {
        List<Application> all=store.load(); long max=0;
        for(Application a:all) max=Math.max(max,a.id);
        if(max==Long.MAX_VALUE) throw new IllegalArgumentException("No more IDs available");
        Application added=new Application(max+1,company,role,applied,Status.APPLIED,followUp,notes);
        all.add(added); store.save(all); return added;
    }
    public void update(long id,Status status,LocalDate followUp,String notes) throws IOException {
        List<Application> all=store.load();
        for(int i=0;i<all.size();i++) if(all.get(i).id==id) {
            all.set(i,all.get(i).update(status,followUp,notes)); store.save(all); return;
        }
        throw new IllegalArgumentException("Application ID not found: "+id);
    }
    public List<Application> due(LocalDate asOf) throws IOException {
        List<Application> out=new ArrayList<Application>();
        for(Application a:list()) if(a.isDue(asOf)) out.add(a);
        Collections.sort(out,new Comparator<Application>() {
            public int compare(Application a,Application b) {
                int byDate=a.followUpOn.compareTo(b.followUpOn);
                return byDate!=0?byDate:Long.compare(a.id,b.id);
            }
        });
        return out;
    }
    public Map<Status,Integer> summary() throws IOException {
        Map<Status,Integer> counts=new EnumMap<Status,Integer>(Status.class);
        for(Status s:Status.values()) counts.put(s,0);
        for(Application a:list()) counts.put(a.status,counts.get(a.status)+1);
        return counts;
    }
    public String location() { return store.location(); }
}
