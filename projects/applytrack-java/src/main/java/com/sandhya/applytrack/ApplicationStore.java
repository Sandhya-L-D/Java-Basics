package com.sandhya.applytrack;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

/** Single-user CSV persistence. Invalid files are rejected before any write. */
public final class ApplicationStore {
    public static final String HEADER = "id,company,role,applied_on,status,follow_up_on,notes";
    private final Path path;
    public ApplicationStore(Path path) { this.path = path.toAbsolutePath().normalize(); }
    public List<Application> load() throws IOException {
        List<Application> apps = new ArrayList<Application>();
        if (!Files.exists(path)) return apps;
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !HEADER.equals(lines.get(0))) throw new IOException("Invalid CSV header in " + path);
        Set<Long> ids = new HashSet<Long>();
        for (int i=1; i<lines.size(); i++) {
            if (lines.get(i).trim().isEmpty()) continue;
            try {
                List<String> f = Csv.parse(lines.get(i));
                if (f.size()!=7) throw new IllegalArgumentException("Expected 7 fields");
                Application a = new Application(Long.parseLong(f.get(0)),f.get(1),f.get(2),
                    LocalDate.parse(f.get(3)),Status.valueOf(f.get(4)),
                    f.get(5).isEmpty()?null:LocalDate.parse(f.get(5)),f.get(6));
                if (!ids.add(a.id)) throw new IllegalArgumentException("Duplicate ID");
                apps.add(a);
            } catch (RuntimeException e) { throw new IOException("Invalid row " + (i+1) + ": " + e.getMessage(),e); }
        }
        return apps;
    }
    public void save(List<Application> apps) throws IOException {
        List<String> lines = new ArrayList<String>(); lines.add(HEADER);
        Set<Long> ids = new HashSet<Long>();
        for (Application a:apps) {
            if (!ids.add(a.id)) throw new IllegalArgumentException("Duplicate ID");
            lines.add(Csv.row(Long.toString(a.id),a.company,a.role,a.appliedOn.toString(),
                a.status.name(),a.followUpOn==null?"":a.followUpOn.toString(),a.notes));
        }
        Files.createDirectories(path.getParent());
        Path temp = Files.createTempFile(path.getParent(), ".applytrack-", ".tmp");
        try {
            Files.write(temp,lines,StandardCharsets.UTF_8);
            try { Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch(AtomicMoveNotSupportedException e) { Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
    public String location() { return path.toString(); }
}
