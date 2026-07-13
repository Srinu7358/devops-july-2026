package org.tektutor;

import javax.enterprise.context.ApplicationScoped;

/**
 * A CDI bean. The container creates and manages it, so no code calls
 * "new GreetingService()". @ApplicationScoped means one shared instance
 * exists for the whole application.
 */
@ApplicationScoped
public class GreetingService {

    public String greet(String name) {
	String who = (name == null || name.trim().isEmpty()) ? "guest" : name;
        return "Hello " + who + " from Tomcat";
    }
}
