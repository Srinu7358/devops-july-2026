# Day 1

## Info - Servlet Overview
<pre>
- A Servlet class is a Java class that handles HTTP requests
- It becomes a servlet by extending HttpServlet and getting registered with the container
- HttpServlet gives you the method-dispatch machinery, so you override doGet, doPost, and
  the rest instead of writing raw request handling
- @WebServlet("/hello") registers the class and maps it to a URL pattern
- Servlet is created and controlled by the Servlet container
</pre>

## Info - Bean Overview
<pre>
- A Bean is a different of Object
- It is a plain Java class whose lifecycle a container manages for you,
  you never write new(allocate/delete) for it
- In the CDI(Context Dependency Injection) word, most classes qualify as beans automatically once CDI is switched on
</pre>

## Info - Apache Tomcat Overview
<pre>
- Apache Tomcat is an open source web server and servlet container maintained by the Apache Software Foundation
- It runs Java web applications by implementing the Jakarta Servlet, Jakarta Server Pages(JSP),
  Jakarta Expression Language and WebSocket specifications
- Tomcat is not a full JAVA EE application server
- It handles the web tier( Servlets and JSP, but it doesn't ship with EJB, JMS, or full CDI support like
  WildFly or WebSphere do )
- Most teams pick Tomcat because it stays lightweight and starts fast.
- You run it when your application  needs a servlet container and nothing heavier
</pre>
