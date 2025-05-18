package com.blackflower.backgammon_computernetworks.server;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 *
 * @author emirs
 */
public final class LegacyMessage {

    /* ---------- INSTANCE ---------- */
    private final String type;
    private final Map<String, String> fields = new LinkedHashMap<>();

    public LegacyMessage(String type) { this.type = type; }

    /* ---------- Fluent builder ---------- */
    public LegacyMessage put(String k, Object v) {
        fields.put(k, String.valueOf(v));
        return this;
    }

    /* ---------- Accessors ---------- */
    public String type()  { return type; }
    public String get(String k)           { return fields.get(k);        }
    public int    getInt(String k)        { return Integer.parseInt(get(k)); }
    public boolean has(String k)          { return fields.containsKey(k); }

    /* ---------- (De)serialisation ---------- */
    public String encode() {
        StringJoiner sj = new StringJoiner("|");
        sj.add(type);
        fields.forEach((k,v) -> sj.add(k + "=" + v));
        return sj.toString();
    }

    public static LegacyMessage decode(String line) {
        String[] parts = line.split("\\|");
        LegacyMessage msg = new LegacyMessage(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            int eq = parts[i].indexOf('=');
            if (eq < 0) continue;
            msg.fields.put(parts[i].substring(0, eq), parts[i].substring(eq + 1));
        }
        return msg;
    }

    @Override public String toString() { return encode(); }
}
