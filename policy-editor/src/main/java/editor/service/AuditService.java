package editor.service;

import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@ApplicationScoped
public class AuditService {
    private static final Path LOG = Paths.get("data", "audit.log");
    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    public AuditService() {
        try { Files.createDirectories(LOG.getParent()); } catch (IOException ignored) {}
    }

    public synchronized void record(String actor, String action, String target, String details) {
        recordCtx(actor, action, target, details, null, null, null);
    }

    public synchronized void recordCtx(String actor, String action, String target, String details, String tenant, String ip, String userAgent) {
        String line = String.format("%s actor=%s action=%s target=%s tenant=%s ip=%s ua=%s details=%s%n",
                F.format(Instant.now()),
                safe(actor), safe(action), safe(target), safe(tenant), safe(ip), safe(userAgent), safe(details));
        try {
            Files.writeString(LOG, line, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException("写入审计日志失败", e);
        }
    }

    private String safe(String s) { return s == null ? "-" : s.replaceAll("\n"," "); }

    public synchronized java.util.List<String> readAll() {
        try {
            if (!java.nio.file.Files.exists(LOG)) return java.util.List.of();
            return java.nio.file.Files.readAllLines(LOG);
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    public static class AuditEntry {
        public String timestamp; public String actor; public String action; public String target; public String tenant; public String ip; public String ua; public String details;
    }

    public synchronized java.util.List<AuditEntry> query(int page, int size, String q) {
        var lines = readAll();
        java.util.List<AuditEntry> all = new java.util.ArrayList<>();
        for (String line : lines) {
            AuditEntry e = parse(line);
            if (e == null) continue;
            if (q != null && !q.isBlank()) {
                String L = (e.timestamp+" "+e.actor+" "+e.action+" "+e.target+" "+e.details).toLowerCase();
                if (!L.contains(q.toLowerCase())) continue;
            }
            all.add(e);
        }
        int from = Math.max(0, Math.min(page*size, all.size()));
        int to = Math.max(from, Math.min(from+size, all.size()));
        return all.subList(from, to);
    }

    private AuditEntry parse(String line) {
        try {
            AuditEntry e = new AuditEntry();
            int idx = line.indexOf(" actor=");
            e.timestamp = idx>0 ? line.substring(0, idx).trim() : line;
            String rest = idx>0 ? line.substring(idx+1).trim() : "";
            for (String part : rest.split(" ")) {
                if (part.startsWith("actor=")) e.actor = part.substring(6);
                else if (part.startsWith("action=")) e.action = part.substring(7);
                else if (part.startsWith("target=")) e.target = part.substring(7);
                else if (part.startsWith("tenant=")) e.tenant = part.substring(7);
                else if (part.startsWith("ip=")) e.ip = part.substring(3);
                else if (part.startsWith("ua=")) e.ua = part.substring(3);
                else if (part.startsWith("details=")) e.details = part.substring(8);
            }
            return e;
        } catch (Exception ex) { return null; }
    }

    public synchronized String exportText(String start, String end, String q) {
        var lines = readAll();
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            boolean ok = true;
            if (q != null && !q.isBlank()) ok &= line.toLowerCase().contains(q.toLowerCase());
            if (start != null && !start.isBlank()) ok &= line.compareTo(start) >= 0;
            if (end != null && !end.isBlank()) ok &= line.compareTo(end) <= 0;
            if (ok) sb.append(line).append('\n');
        }
        return sb.toString();
    }

    public synchronized void clear() {
        try { java.nio.file.Files.deleteIfExists(LOG); }
        catch (IOException ignored) {}
    }
}
