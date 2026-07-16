package app;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * The push plugin. On startup the app tier generates the allow-list from its
 * declared public paths and pushes it to the web tier.
 */
@WebListener
public class AllowListPublisher implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent ev) {
        ServletContext ctx = ev.getServletContext();
        try {
            ctx.log("push plugin: " + push(ctx));
        } catch (Exception e) {
            ctx.log("push plugin: initial push failed (" + e.getMessage()
                    + "). Run /admin/publish once the web tier is up.");
        }
    }

    static String push(ServletContext ctx) throws Exception {
        String webTierUrl = param(ctx, "webTierPushUrl",
                "http://127.0.0.1:9091/admin/allowlist");
        String token = param(ctx, "pushToken", "s3cr3t-web-tier");
        String csv = param(ctx, "publicPaths", "");

        StringBuilder body = new StringBuilder();
        for (String p : csv.split(",")) {
            String t = p.trim();
            if (!t.isEmpty()) {
                body.append(t).append('\n');
            }
        }

        HttpClient c = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(webTierUrl))
                .timeout(Duration.ofSeconds(5))
                .header("X-Push-Token", token)
                .header("Content-Type", "text/plain; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> resp = c.send(
                req, HttpResponse.BodyHandlers.ofString());
        return "pushed to web tier, reply " + resp.statusCode()
                + " (" + resp.body().trim() + ")";
    }

    static String param(ServletContext ctx, String name, String dflt) {
        String v = ctx.getInitParameter(name);
        return (v == null || v.isEmpty()) ? dflt : v;
    }
}
