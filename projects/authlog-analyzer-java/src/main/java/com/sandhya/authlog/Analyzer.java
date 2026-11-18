package com.sandhya.authlog;
import java.time.*;
import java.util.*;

/** Batch heuristic detector; one independent failure window per (source,user). */
public final class Analyzer {
    private final int threshold;
    private final long windowSeconds;
    public Analyzer(int threshold,long windowSeconds) {
        if(threshold<2||windowSeconds<1||windowSeconds>86400)
            throw new IllegalArgumentException("Threshold must be >=2; window must be 1..86400 seconds");
        this.threshold=threshold;this.windowSeconds=windowSeconds;
    }
    private static final class Key {
        final String source,user;
        Key(LoginEvent e){source=e.source;user=e.user;}
        public boolean equals(Object o){if(!(o instanceof Key))return false;Key k=(Key)o;return source.equals(k.source)&&user.equals(k.user);}
        public int hashCode(){return Objects.hash(source,user);}
    }
    private static final class Window {
        final Deque<Instant> failures=new ArrayDeque<Instant>();
        boolean alerted;
    }
    public List<Alert> analyze(List<LoginEvent> input) {
        List<LoginEvent> events=new ArrayList<LoginEvent>(input);
        // Stable sort preserves file order for records with equal timestamps.
        Collections.sort(events,new Comparator<LoginEvent>(){public int compare(LoginEvent a,LoginEvent b){return a.time.compareTo(b.time);}});
        Map<Key,Window> windows=new HashMap<Key,Window>();
        List<Alert> alerts=new ArrayList<Alert>();
        for(LoginEvent event:events) {
            Key key=new Key(event);Window window=windows.get(key);
            if(window==null){window=new Window();windows.put(key,window);}
            // Inclusive interval [currentTime-windowSeconds,currentTime].
            while(!window.failures.isEmpty()&&Duration.between(window.failures.peekFirst(),event.time).compareTo(Duration.ofSeconds(windowSeconds))>0)
                window.failures.removeFirst();
            if(window.failures.size()<threshold)window.alerted=false;
            if(event.success) {
                if(window.failures.size()>=threshold)
                    alerts.add(new Alert("SUCCESS_AFTER_FAILURES",event.source,event.user,window.failures.peekFirst(),event.time,window.failures.size()));
                window.failures.clear();window.alerted=false;
            } else {
                window.failures.addLast(event.time);
                if(window.failures.size()>=threshold&&!window.alerted) {
                    alerts.add(new Alert("REPEATED_FAILURES",event.source,event.user,window.failures.peekFirst(),event.time,window.failures.size()));
                    window.alerted=true;
                }
            }
        }
        return alerts;
    }
}
