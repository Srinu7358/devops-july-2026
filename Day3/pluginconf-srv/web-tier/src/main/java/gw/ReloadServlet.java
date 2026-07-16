package gw;

import java.io.IOException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Re-reads plugin-allow.conf into the live routing set. */
@WebServlet(urlPatterns = {"/gateway/reload"})
public class ReloadServlet extends HttpServlet {

    private String token() {
        String t = getServletContext().getInitParameter("pushToken");
        return t == null ? "" : t;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        if (!token().equals(req.getHeader("X-Push-Token"))) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "bad push token");
            return;
        }
        resp.setContentType("text/plain; charset=utf-8");
        try {
            int n = PluginAllowStore.get().reload();
            resp.getWriter().println("reloaded, " + n + " path(s) active: "
                    + PluginAllowStore.get().active());
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    e.getMessage());
        }
    }
}
