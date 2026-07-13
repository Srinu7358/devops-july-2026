package org.tektutor;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * A servlet. The servlet container (Tomcat) creates this instance and drives
 * its lifecycle. One instance, many request threads, so keep per-request data
 * in local variables, never fields.
 *
 * The @Inject field is filled by the CDI container (Weld) after construction
 * and before init(). No Weld, no injection, and doGet throws NullPointerException.
 */
@WebServlet("/hello")
public class HelloServlet extends HttpServlet {

    @Inject
    private GreetingService greetingService;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String name = req.getParameter("name");

        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(greetingService.greet(name));
    }
}
