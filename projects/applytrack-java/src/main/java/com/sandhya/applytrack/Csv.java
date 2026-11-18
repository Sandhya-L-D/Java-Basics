package com.sandhya.applytrack;
import java.util.*;

/** Small strict CSV codec. Quoted commas/quotes supported; fields stay on one line. */
public final class Csv {
    private Csv() {}
    public static List<String> parse(String line) {
        List<String> fields = new ArrayList<String>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false, closed = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\r' || c == '\n') throw new IllegalArgumentException("Multiline fields are not supported");
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i+1) == '"') { value.append('"'); i++; }
                    else { quoted = false; closed = true; }
                } else value.append(c);
            } else if (c == ',') {
                fields.add(value.toString()); value.setLength(0); closed = false;
            } else if (closed) {
                throw new IllegalArgumentException("Unexpected character after closing quote");
            } else if (c == '"') {
                if (value.length() != 0) throw new IllegalArgumentException("Quote inside an unquoted field");
                quoted = true;
            } else value.append(c);
        }
        if (quoted) throw new IllegalArgumentException("Unclosed CSV quote");
        fields.add(value.toString());
        return fields;
    }
    public static String row(String... fields) {
        StringBuilder out = new StringBuilder();
        for (String value : fields) {
            if (value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0)
                throw new IllegalArgumentException("Multiline fields are not supported");
            if (out.length() > 0) out.append(',');
            out.append('"').append(value.replace("\"", "\"\"")).append('"');
        }
        return out.toString();
    }
}
