package gw;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Front door: proxy approved paths to the app tier, block the rest. */
@WebFilter(urlPatterns = {"/*"})
public class GatewayFilter implements Filter {

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private String backendBase;

    @Override
    public void init(FilterConfig cfg) {
        String b = cfg.getServletContext().getInitParameter("backendBase");
        backendBase = (b == null || b.isEmpty())
                ? "http://127.0.0.1:9092" : b;
    }

    @Override
    public void doFilter(ServletRequest sreq, ServletResponse sresp,
            FilterChain chain) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) sreq;
        HttpServletResponse resp = (HttpServletResponse) sresp;
        String path = req.getRequestURI();

        if (path.equals("/admin/allowlist")) {
            chain.doFilter(sreq, sresp);
            return;
        }

        if (!AllowListStore.get().allows(path)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.setContentType("text/plain; charset=utf-8");
            resp.getWriter().println("blocked at web tier: " + path);
            return;
        }

        String qs = req.getQueryString();
        String target = backendBase + path + (qs == null ? "" : "?" + qs);
        byte[] body = req.getInputStream().readAllBytes();

        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(target))
                .timeout(Duration.ofSeconds(5))
                .method(req.getMethod(), body.length == 0
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofByteArray(body));
        String ct = req.getContentType();
        if (ct != null) {
            b.header("Content-Type", ct);
        }

        try {
            HttpResponse<byte[]> br = client.send(
                    b.build(), HttpResponse.BodyHandlers.ofByteArray());
            resp.setStatus(br.statusCode());
            br.headers().firstValue("Content-Type")
                    .ifPresent(resp::setContentType);
            resp.getOutputStream().write(br.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            resp.sendError(HttpServletResponse.SC_BAD_GATEWAY,
                    "app tier unreachable");
        }
    }
}
