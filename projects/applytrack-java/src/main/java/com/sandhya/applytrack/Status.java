package com.sandhya.applytrack;
public enum Status {
    APPLIED, ASSESSMENT, INTERVIEW, OFFER, REJECTED, WITHDRAWN;
    public boolean isClosed() { return this == OFFER || this == REJECTED || this == WITHDRAWN; }
}
