package app;

import java.io.IOException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Re-generate and re-push the allow-list on demand. */
@WebServlet(urlPatterns = {"/admin/publish"})
public class PublishServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("text/plain; charset=utf-8");
        try {
            resp.getWriter().println(
                    AllowListPublisher.push(getServletContext()));
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_BAD_GATEWAY, e.getMessage());
        }
    }
}
