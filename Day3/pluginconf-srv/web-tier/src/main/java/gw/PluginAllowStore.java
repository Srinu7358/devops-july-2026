package gw;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Backs the gateway allow-list with an on-disk file, WEB-INF/plugin-allow.conf.
 * push() writes the file. reload() re-reads the file into the live in-memory
 * set. A push alone does NOT change routing until a reload is performed.
 */
public final class PluginAllowStore {

    private static final PluginAllowStore INSTANCE = new PluginAllowStore();

    private volatile Set<String> active = Collections.emptySet();
    private volatile Path confFile;

    private PluginAllowStore() {
    }

    public static PluginAllowStore get() {
        return INSTANCE;
    }

    public void setConfFile(Path p) {
        this.confFile = p;
    }

    public Path confFile() {
        return confFile;
    }

    /** Write the pushed list to disk. Does not touch the live set. */
    public synchronized void writeConf(List<String> lines) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# plugin-allow.conf - one allowed path per line\n");
        for (String l : lines) {
            String t = l.trim();
            if (!t.isEmpty()) {
                sb.append(t).append('\n');
            }
        }
        Files.writeString(confFile, sb.toString(), StandardCharsets.UTF_8);
    }

    /** Re-read the file into the live set. Returns the number of paths loaded. */
    public synchronized int reload() throws IOException {
        Set<String> next = new LinkedHashSet<>();
        if (confFile != null && Files.exists(confFile)) {
            for (String l : Files.readAllLines(confFile, StandardCharsets.UTF_8)) {
                String t = l.trim();
                if (!t.isEmpty() && !t.startsWith("#")) {
                    next.add(t);
                }
            }
        }
        this.active = Collections.unmodifiableSet(next);
        return active.size();
    }

    public Set<String> active() {
        return active;
    }

    public boolean allows(String requestPath) {
        for (String e : active) {
            if (requestPath.equals(e) || requestPath.startsWith(e + "/")) {
                return true;
            }
        }
        return false;
    }
}
