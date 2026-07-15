package org.tektutor.app;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@WebServlet("/*")
public class ApiServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        PrintWriter out = resp.getWriter();
        out.printf("{%n");
        out.printf("  \"tier\": \"APP\",%n");
        out.printf("  \"localPort\": %d,%n", req.getLocalPort());
        out.printf("  \"serverPort\": %d,%n", req.getServerPort());
        out.printf("  \"clientIp\": \"%s\",%n", req.getRemoteAddr());
        out.printf("  \"scheme\": \"%s\",%n", req.getScheme());
        out.printf("  \"contextPath\": \"%s\",%n", req.getContextPath());
        out.printf("  \"requestUri\": \"%s\",%n", req.getRequestURI());
        out.printf("  \"time\": \"%s\"%n", now);
        out.printf("}%n");
    }
}
