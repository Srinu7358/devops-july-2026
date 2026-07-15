package org.tektutor.web;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@WebServlet(urlPatterns = {"/", "/web/*"})
public class WebTierServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");

        PrintWriter out = resp.getWriter();
        out.println("==============================================");
        out.println(" WEB TIER");
        out.println("==============================================");
        out.println(" Served by  : WEB TIER");
        out.println(" Local port : " + req.getLocalPort());   // 9091 (real socket)
        out.println(" Server port: " + req.getServerPort());  // 80 (because of proxyPort)
        out.println(" Client IP  : " + req.getRemoteAddr());
        out.println(" Scheme     : " + req.getScheme());
        out.println(" Path       : " + req.getRequestURI());
        out.println(" Time       : " + now);
        out.println("==============================================");
    }
}
