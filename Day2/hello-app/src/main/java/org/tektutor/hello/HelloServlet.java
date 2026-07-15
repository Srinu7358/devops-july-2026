package org.tektutor.hello;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@WebServlet(urlPatterns = {"/", "/hello"})
public class HelloServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

	    //Reads from bin/setenv.sh
	    String nodeName = System.getProperty("node.name", "UNKNOWN");

	    //Reads from catalina.sh
	    String catalinaBase = System.getProperty("catalina.base", "UNKNOWN");
	    String catalinaHome = System.getProperty("catalina.home", "UNKNOWN");

	    long maxHeapMB = Runtime.getRuntime().maxMemory() / ( 1024 * 1024 );
	    String pid = ManagementFactory.getRuntimeMXBean().getName();
	    String now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

	    resp.setContentType("text/plain");
	    resp.setCharacterEncoding("UTF-8");

	    PrintWriter out = resp.getWriter();
	   
	    out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++");
	    out.println(" Hello from Tomcat 11" ); 
	    out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++");
	    out.println(" Node name         : " + nodeName );
	    out.println(" HTTP port         : " + req.getLocalPort() );
	    out.println(" CATALINA_HOME     : " + catalinaHome );
	    out.println(" CATALINA_BASE     : " + catalinaBase );
	    out.println(" Max head (MB)     : " + maxHeapMB );
	    out.println(" JVM               : " + pid );
	    out.println(" Server info       : " + getServletContext().getServerInfo() );
	    out.println(" Context path      : " + req.getContextPath() );
	    out.println(" Time              : " + now );
	    out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++");
    }
}
