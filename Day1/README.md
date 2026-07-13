# Day 1

## URLs for Feed and pre-test links - Access from RPS Cloud Lab machine
<pre>
https://notepad.pw/DevOps13
</pre>  

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

#### What Apache Tomcat does ?
<pre>
- When a request hits Tomcat, it maps the URL to a Servlet, run your Java code, and retuns the response.
- It manages the HTTP connection, request threading, session state, and the Servlet lifecycle
- You deploy an application as a WAR file or an exploded directory under webapps
</pre>

#### Core Components<pre>
<pre>
- Server
  - the top-level element, that represents the whole Tomcat instance
  - Listens on port 8005 for shutdown commands by default
- Service
  - groups one or more Connectors with a Single Engine
  
- Connector
  - accepts client connections on a port
  - The HTTP/1.1 connector defautls to port 8080
  - You can add an AJP connector or an HTTP/2 connector

- Engine
  - the request-processing pipeline that handles all requests for a Service
  
- Host
  - a virtual host, like www.tektutor.org, mapped to a set of applications

- Context
  - a single web application, mapped to a URL path such /myapp
</pre>

## Info - Tomcat High-Level Architecture
![tomcat](tomcat-architecture.svg)

## Info - Tomcat Directory Layout
<pre>
bin 
- startup and shutdown scripts 
- catalina.sh
- startup.sh
- shutdown.sh
conf
- it has configurations,including server.xml, web.xml, tomcat-users.xml 
webapps
- deployed applications can be found here ( all the wars files )
logs
- catalina.out and access.logs
lib
- Tomcat and shared libraries
temp and work
- scratchspace and compiled JSPs
</pre>

## Info - Apache Tomcat 9 specifics
<pre>
- Apache Tomcat 9 uses javax.* namespace in Servlet applications
- Oracle made Java EE as an opensource project by donating it to Eclipse Foundation
- Requires minimum JDK 8
- Tomcat 9 supports
  - Servlet specification v4.0
  - JSP v2.3
  - WebSocket v1.1
  - Weld 3.x
  - HTTP/2 ( by default )
</pre>

Tomcat 9 pom.xml
```
<dependency>
  <groupId>javax.servlet</groupId>
  <artifactId>javax.servlet-api</artifactId>
  <version>4.0.1</version>
  <scope>provided</scope>
</dependency>
```

## Info - Tomcat 10 specifics
<pre>
- Apache 10 uses jakarta.* namespace due to the Java trademark issue
- The Servelet that was built for Tomcat9 will not work in Tomcat 10.x or 11.x
- Requires minimum JDK 11
- Tomcat 10 supports
  - Jakarta Servlet specification v3.1
  - WebSocket v2.1
  - Weld 5.x
  - HTTP/2 ( Default )
</pre>  

Tomcat 10 pom.xml
```
<dependency>
  <groupId>jakarta.servlet</groupId>
  <artifactId>jakarta.servlet-api</artifactId>
  <version>6.0</version>
  <scope>provided</scope>
</dependency>
```

## Info - Tomcat 11 specifics
<pre>
- Tomcat 11 targets Jakarta EE 11 and requires Java 17 a minimum requirement
- It uses the jakarta.* namespace not the older javax.*
- Applications written for Tomcat 9 or earlier will not run unmodified, since the package rename breaks
  import statements
- this matters when you migrate legacy apps
- Change the HTTP port by editing the Connector port attribute in server.xml from 8080 to 8081
- Set JVM memory by adding CATALINAT_OPTS="Xms512m" -Xmx1024m" to bin/setenv.sh
- Enable HTTPS by configuring an SSL Connector with a keystore
- one operational detail worth noting
  - if you run Tomcat under a dedicated tomcat user
  - set file ownership correctly on the install directory
  - wrong ownership causes setenv.sh to be skipped silently and CATALINS_OPTS shows up blank at
  ownership correctly on the install directory
</pre>

## Vim editor basics
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/6a949b46-b89d-485f-b97e-92b88796135f" />


## Lab - Install Tomcat9 in Ubuntu 

Open a terminal 1, type the below commands
```
sudo apt update
sudo apt install -y openjdk-17-jdk
java -version
javac -version
```

Create a dedicated folder for Tomcat9 in Terminal 1
```
sudo mkdir -p /opt/tomcat9
sudo useradd -r -m -U -d /opt/tomcat9 -s /bin/false tomcat
```

Download and Install Tomcat9 in Terminal 1
```
cd /tmp
wget https://dlcdn.apache.org/tomcat/tomcat-9/v9.0.120/bin/apache-tomcat-9.0.120.tar.gz
sudo tar -xzf apache-tomcat-9.0.120.tar.gz -C /opt/tomcat9 --strip-components=1
```

Change ownership of /opt/tomcat9 folder to tomcat user in Terminal 1
```
sudo chown -R tomcat:tomcat /opt/tomcat9
sudo chmod -R u+x /opt/tomcat9/bin
```

Configure the JVM settings in Terminal 1
```
sudo tee /opt/tomcat9/bin/setenv.sh > /dev/null <<'EOF'
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export CATALINA_OPTS="-Xms512m -Xmx1024m"
EOF
```

Change the ownership of setenv.sh script to tomcat user in Terminal 1
```
sudo chown tomcat:tomcat /opt/tomcat9/bin/setenv.sh
sudo chmod +x /opt/tomcat9/bin/setenv.sh
```

Run Tomcat9 as a linux service in Terminal 1
```
sudo tee /etc/systemd/system/tomcat9.service > /dev/null << 'EOF'
[Unit]
Description=Apache Tomcat 9
After=network.target

[Service]
Type=forking

User=tomcat
Group=tomcat

Environment="JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64"
Environment="CATALINA_HOME=/opt/tomcat9"
Environment="CATALINA_BASE=/opt/tomcat9"
Environment="CATALINA_PID=/opt/tomcat9/temp/tomcat.pid"

ExecStart=/opt/tomcat9/bin/startup.sh
ExecStop=/opt/tomcat9/bin/shutdown.sh

Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF
```

Start the service in Terminal 1
```
# The command below will ensure the new and existing service configurations are re-loaded 
sudo systemctl daemon-reload

# The command below will ensure, the next time the linux machine is rebooted, this service will be started automatic
sudo systemctl enable tomcat9

# In the current session, though we enabled the service we need manually start the service
sudo systemctl start tomcat9

# Command below, tells you current status of the service
sudo systemctl status tomcat9
```

Test in Terminal 1
```
curl http://localhost:8080
```

Watch live log in Terminal 2
```
sudo tail -f /opt/tomcat9/logs/catalina.out
```

## Lab - Deploying a Hello World Servlet into Tomcat 9
Install Maven Build Tool in Ubuntu
```
sudo apt update && sudo apt install -y maven
mvn --version
```

Clone the TekTutor Training Repository
```
cd ~
git clone https://github.com/tektutor/devops-july-2026.git
cd devops-july-2026
```

Compiling the Servlet application
```
cd ~/devops-july-2026
git pull
cd Day1/tomcat9/hello-servlet
tree
mvn clean package

# In case you got compilation error
git pull
mvn clean package
```
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/f94225fb-9b73-4af5-8fd1-d4f9b9a785f9" />
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/19cf5565-b29e-4e6f-8708-4962ba94cd72" />


Deploy this servlet application into Apache Tomcat v9
```
cd ~/devops-july-2026/Day1/tomcat9/hello-servlet
sudo cp target/hello-tomcat-servlet.war /opt/tomcat9/webapps/
```

Test your application, in the below command replace 'Jegan' with your name
```
curl "http://localhost:8080/hello-tomcat-servlet/hello?name=Jegan"
```
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/de2211a2-441c-4a13-a78b-fc8d3e999748" />

## Lab - Install gedit editor ( use this in the place of vim editor - this works like Windows notepad )
```
sudo apt update && sudo apt install -y gedit
```

## Lab - Install Tomcat 10 in Ubuntu
In the Linux Terminal, type the below commands
```
sudo mkdir -p /opt/tomcat10
sudo useradd -r -m -U -d /opt/tomcat10 -s /bin/false tomcat10
```

Download and Install Apache Tomcat 10
```
cd /tmp
wget https://downloads01-he-fi.apache.org/tomcat/tomcat-10/v10.1.57/bin/apache-tomcat-10.1.57.tar.gz
sudo tar -xzf apache-tomcat-10.1.57.tar.gz -C /opt/tomcat10 --strip-components=1
```

Manage ownership
```
sudo chown -R tomcat10:tomcat10 /opt/tomcat10
sudo chmod -R u+x /opt/tomcat10/bin

JDK=$(readlink -f "$(which java)" | sed 's:/bin/java::')
sudo tee /opt/tomcat10/bin/setenv.sh > /dev/null <<EOF
export JAVA_HOME=$JDK
export CATALINA_OPTS="-Xms512m -Xmx1024m"
EOF
```

Manage the ownership
```
sudo chown tomcat10:tomcat10 /opt/tomcat10/bin/setenv.sh
sudo chmod +x /opt/tomcat10/bin/setenv.sh
sudo cat /opt/tomcat10/bin/setenv.sh
```

Change the port so it doesn't conflict with tomcat9
```
sudo vim /opt/tomcat10/conf/server.xml
# Change the shutdown port from 8005 to 8006
# Change the HTTP connector port from 8080 to 8090
# If AJP port is uncommented from 8009 to 8010
```

Create a service for Tomcat 10
```
sudo tee /etc/systemd/system/tomcat10.service > /dev/null << 'EOF'
[Unit]
Description=Apache Tomcat 10.1
After=network.target

[Service]
Type=forking

User=tomcat10
Group=tomcat10

Environment="JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64"
Environment="CATALINA_HOME=/opt/tomcat10"
Environment="CATALINA_BASE=/opt/tomcat10"
Environment="CATALINA_PID=/opt/tomcat10/temp/tomcat.pid"

ExecStart=/opt/tomcat10/bin/startup.sh
ExecStop=/opt/tomcat10/bin/shutdown.sh

Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF
```

Start the service in Terminal 1
```
sudo systemctl daemon-reload
sudo systemctl enable tomcat10
sudo systemctl start tomcat10
sudo systemctl status tomcat10
```
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/ed44aef9-f948-4b46-b15e-2970b41240c8" />

Test in Terminal 1
```
curl http://localhost:8090
```
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/32b9973c-cde5-481a-ab82-1ac664727502" />

Watch live log in Terminal 2
```
sudo tail -f /opt/tomcat10/logs/catalina.out
```
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/9cf8812f-a338-48ce-9a15-25ef86646f30" />

