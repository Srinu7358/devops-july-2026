package gw;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** /gateway/active shows the live set; /gateway/conf shows the file on disk. */
@WebServlet(urlPatterns = {"/gateway/active", "/gateway/conf"})
public class StatusServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("text/plain; charset=utf-8");
        if (req.getRequestURI().endsWith("/conf")) {
            var f = PluginAllowStore.get().confFile();
            if (f != null && Files.exists(f)) {
                resp.getWriter().write(
                        Files.readString(f, StandardCharsets.UTF_8));
            } else {
                resp.getWriter().println("(no plugin-allow.conf on disk)");
            }
        } else {
            for (String p : PluginAllowStore.get().active()) {
                resp.getWriter().println(p);
            }
        }
    }
}
