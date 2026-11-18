package com.sandhya.authlog;
import java.time.Instant;
/** Normalized input record. Source is an opaque identifier, not DNS-resolved. */
public final class LoginEvent {
    public final Instant time;
    public final String source,user;
    public final boolean success;
    public LoginEvent(Instant time,String source,String user,boolean success) {
        if(time==null||source==null||source.trim().isEmpty()||user==null||user.trim().isEmpty())
            throw new IllegalArgumentException("Time, source and user are required");
        this.time=time;this.source=source.trim();this.user=user.trim();this.success=success;
    }
}
