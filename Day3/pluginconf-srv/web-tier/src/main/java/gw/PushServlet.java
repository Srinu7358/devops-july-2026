package gw;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Receives a pushed allow-list and writes it to plugin-allow.conf on disk. */
@WebServlet(urlPatterns = {"/gateway/push"})
public class PushServlet extends HttpServlet {

    private String token() {
        String t = getServletContext().getInitParameter("pushToken");
        return t == null ? "" : t;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        if (!token().equals(req.getHeader("X-Push-Token"))) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "bad push token");
            return;
        }
        List<String> lines = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                req.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                lines.add(line);
            }
        }
        PluginAllowStore.get().writeConf(lines);
        resp.setContentType("text/plain; charset=utf-8");
        resp.getWriter().println("written to plugin-allow.conf ("
                + lines.size() + " line(s)). Run /gateway/reload to apply.");
    }
}
