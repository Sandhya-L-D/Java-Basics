package com.sandhya.authlog;
import java.time.Instant;
public final class Alert {
    public final String type,source,user;
    public final Instant first,last;
    public final int failures;
    public Alert(String type,String source,String user,Instant first,Instant last,int failures) {
        this.type=type;this.source=source;this.user=user;this.first=first;this.last=last;this.failures=failures;
    }
}
