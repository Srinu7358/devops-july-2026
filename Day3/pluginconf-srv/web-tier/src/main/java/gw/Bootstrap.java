package gw;

import java.nio.file.Paths;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/** Resolve WEB-INF/plugin-allow.conf and load it once at startup. */
@WebListener
public class Bootstrap implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent ev) {
        ServletContext ctx = ev.getServletContext();
        String real = ctx.getRealPath("/WEB-INF/plugin-allow.conf");
        if (real == null) {
            ctx.log("gateway: cannot resolve plugin-allow.conf "
                    + "(is the WAR unpacked?)");
            return;
        }
        PluginAllowStore store = PluginAllowStore.get();
        store.setConfFile(Paths.get(real));
        try {
            int n = store.reload();
            ctx.log("gateway: loaded " + n + " path(s) from plugin-allow.conf");
        } catch (Exception e) {
            ctx.log("gateway: initial load failed: " + e.getMessage());
        }
    }
}
