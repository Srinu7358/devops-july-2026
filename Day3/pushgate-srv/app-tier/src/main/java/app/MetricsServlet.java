package app;

import java.io.IOException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Hosted by the app tier but never pushed. The web tier must block it. */
@WebServlet(urlPatterns = {"/api/internal/metrics"})
public class MetricsServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("text/plain; charset=utf-8");
        resp.getWriter().println(
                "app-tier: internal metrics [private, hosted but not approved]");
    }
}
