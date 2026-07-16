package gw;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Receives the pushed allow-list. POST replaces it; GET lists it. */
@WebServlet(name = "allowList", urlPatterns = {"/admin/allowlist"})
public class AllowListServlet extends HttpServlet {

    private String token() {
        String t = getServletContext().getInitParameter("pushToken");
        return t == null ? "" : t;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("text/plain; charset=utf-8");
        PrintWriter out = resp.getWriter();
        for (String p : AllowListStore.get().all()) {
            out.println(p);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String supplied = req.getHeader("X-Push-Token");
        if (!token().equals(supplied)) {
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
        AllowListStore.get().replace(lines);
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("text/plain; charset=utf-8");
        resp.getWriter().println(
                "stored " + AllowListStore.get().all().size() + " paths");
    }
}
