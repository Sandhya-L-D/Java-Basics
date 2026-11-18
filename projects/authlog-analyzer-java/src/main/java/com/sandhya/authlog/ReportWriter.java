package com.sandhya.authlog;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.*;

public final class ReportWriter {
    public static void write(Path directory,LogReader.Result input,List<Alert> alerts,int threshold,long seconds) throws IOException {
        Files.createDirectories(directory);
        List<String> csv=new ArrayList<String>();csv.add("type,source,user,first_event,last_event,failure_count");
        StringBuilder rows=new StringBuilder();
        for(Alert a:alerts){
            csv.add(Csv.row(a.type,safeCell(a.source),safeCell(a.user),a.first.toString(),a.last.toString(),Integer.toString(a.failures)));
            rows.append("<tr><td>").append(escape(a.type)).append("</td><td>").append(escape(a.source)).append("</td><td>")
                .append(escape(a.user)).append("</td><td>").append(a.first).append("</td><td>").append(a.last)
                .append("</td><td>").append(a.failures).append("</td></tr>");
        }
        Files.write(directory.resolve("alerts.csv"),csv,StandardCharsets.UTF_8);
        List<String> warnings=new ArrayList<String>(input.warnings);
        if(input.invalidRows>input.warnings.size())warnings.add("Further invalid rows omitted from this report: "+(input.invalidRows-input.warnings.size()));
        Files.write(directory.resolve("warnings.txt"),warnings,StandardCharsets.UTF_8);
        String html="<!doctype html><html lang='en'><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'>"+
            "<title>AuthLog Analyzer Report</title><style>body{font:16px system-ui,sans-serif;background:#f5f7fb;color:#17243b;margin:0;padding:32px}"+
            "main{max-width:1200px;margin:auto}h1{font-size:32px}p{line-height:1.6}.stats{display:flex;gap:20px;flex-wrap:wrap;margin:24px 0}"+
            ".stat{background:white;border:1px solid #cdd7e7;padding:18px;min-width:160px;border-radius:8px}.stat strong{display:block;font-size:32px}"+
            ".scroll{overflow:auto}table{border-collapse:collapse;background:white;width:100%}th,td{border:1px solid #d7dfeb;padding:12px;text-align:left}"+
            "th{background:#17325b;color:white}tr:nth-child(even){background:#f0f4fa}footer{margin-top:28px;color:#4b5b73}</style><main>"+
            "<p>JAVA SECURITY LOG ANALYSIS</p><h1>Authentication activity report</h1><p>Rule: "+threshold+" failures for the same source and user within "+seconds+
            " seconds. Times are UTC. The window includes both endpoints.</p><div class='stats'><div class='stat'><strong>"+input.events.size()+
            "</strong>Valid events</div><div class='stat'><strong>"+alerts.size()+"</strong>Heuristic alerts</div><div class='stat'><strong>"+input.invalidRows+
            "</strong>Invalid rows skipped</div></div><h2>Alerts for review</h2><p>Repeated failures can be legitimate mistakes. A later successful login is a review signal, not proof of compromise.</p>"+
            "<div class='scroll'><table><thead><tr><th>Rule</th><th>Source</th><th>User</th><th>First event</th><th>Last event</th><th>Failures</th></tr></thead><tbody>"+
            rows+"</tbody></table></div>"+(alerts.isEmpty()?"<p>No events met the configured rules.</p>":"")+
            "<footer>Batch analysis of the supplied CSV only. No network connections, live monitoring or automated blocking. See warnings.txt for rejected rows.</footer></main></html>";
        Files.write(directory.resolve("report.html"),html.getBytes(StandardCharsets.UTF_8));
    }
    static String escape(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
    // Prevent untrusted identifiers becoming formulas when CSV is opened in spreadsheets.
    static String safeCell(String s){return !s.isEmpty()&&"=+-@\t\r".indexOf(s.charAt(0))>=0?"'"+s:s;}
}
