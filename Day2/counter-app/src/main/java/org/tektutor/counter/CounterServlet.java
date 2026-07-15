package org.tektutor.counter;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@WebServlet(urlPatterns = {"/", "/count"})
public class CounterServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(true);

        // Integer is Serializable. EVERY attribute you put into a replicated
        // session must be Serializable, all the way down through its fields,
        // or replication fails at runtime with a NotSerializableException
        // that appears in catalina.out AFTER the response was already sent.
        Integer hits = (Integer) session.getAttribute("hits");
        if (hits == null) {
            hits = 0;
        }
        hits = hits + 1;
        session.setAttribute("hits", hits);

        String nodeName = System.getProperty("node.name", "UNKNOWN");
        String now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();
        out.println("=================================================");
        out.println(" SESSION COUNTER");
        out.println("=================================================");
        out.println(" Served by node : " + nodeName);
        out.println(" HTTP port      : " + request.getLocalPort());
        out.println(" Hit count      : " + hits);
        out.println(" Session ID     : " + session.getId());
        out.println(" Session is new : " + session.isNew());
        out.println(" Time           : " + now);
        out.println("=================================================");
    }
}
