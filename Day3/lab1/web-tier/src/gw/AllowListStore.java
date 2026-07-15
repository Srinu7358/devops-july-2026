package gw;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Holds the current allow-list pushed by the app tier.
 * One instance per web-tier webapp classloader.
 */
public final class AllowListStore {

    private static final AllowListStore INSTANCE = new AllowListStore();

    private volatile Set<String> paths = Collections.emptySet();

    private AllowListStore() {
    }

    public static AllowListStore get() {
        return INSTANCE;
    }

    public void replace(Collection<String> incoming) {
        Set<String> next = new LinkedHashSet<>();
        for (String p : incoming) {
            String t = p.trim();
            if (!t.isEmpty()) {
                next.add(t);
            }
        }
        this.paths = Collections.unmodifiableSet(next);
    }

    public Set<String> all() {
        return paths;
    }

    /** Allow an exact match or any child path under an approved prefix. */
    public boolean allows(String requestPath) {
        for (String entry : paths) {
            if (requestPath.equals(entry)
                    || requestPath.startsWith(entry + "/")) {
                return true;
            }
        }
        return false;
    }
}
