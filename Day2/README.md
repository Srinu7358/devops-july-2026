# Day 2

## Info - Reverse Proxy Server
<pre>
- Reverse Proxy servers can help forward the user request to different web/app server
- Reverse Proxy server optionally can I support https(TLS Termination)
- Transport Layer Security(TLS) - is nothing but HTTPS
- Reverse Proxy Servers are used to 
  - add an additional layer of security
  - load balancing
  - high availability (HA)
  - fault tolerance
  - security reason
- If your intent of Reverse Proxy server is just to support https, Tomcat also can do that
- Tomcat doesn't support reverse proxy, hence we will have to depend one of the below options
  - nginx ( opensource )
  - F5 ( Enterprise variant of Nginx - Need a license )
  - HAProxy ( opensource )
  - Apache Httpd ( we are going to use this one - this is opensource )
</pre>  

## Lab - Setup a reverse proxy in front of a Tomcat Webserver and Tomcat AppServer instances
Lets check the below before we proceed
<pre>
sudo su -
ls -l /opt/tomcat11/bin/catalina.sh
ls -l /opt/tomcat11/lib/catalina.jar
ls -l /opt/tomcat11/lib/servlet-api.jar
ls -l /opt/tomcat11/lib/catalina-ha.jar
ls -l /opt/tomcat11/lib/catalina-tribes.jar

sudo ss -tlnp | grep -E ':(9081|9082|9083|9091|9092|9005|9006|9007|9015|9016|4000|4001|4002)\b'
echo "If see any port listed, it means some application uses the ports we would need to peform this lab exercise"

mvn --version
apache2 -v || sudo apt install -y apache2
</pre>

Let's create the tomcat user and create node1, node2 and node3 directories
```
sudo useradd -r -s /usr/bin/nologin tomcat 2>/dev/null || echo "user tomcat already exists"

for N in node1 node2 node3; do
  sudo mkdir -p /srv/$N/{bin,conf,logs,webapps,work,temp}

  sudo cp /opt/tomcat11/conf/web.xml             /srv/$N/conf/
  sudo cp /opt/tomcat11/conf/context.xml         /srv/$N/conf/
  sudo cp /opt/tomcat11/conf/logging.properties  /srv/$N/conf/
  sudo cp /opt/tomcat11/conf/catalina.properties /srv/$N/conf/
  sudo cp /opt/tomcat11/conf/tomcat-users.xml    /srv/$N/conf/
done

sudo chown -R tomcat:tomcat /srv/node1 /srv/node2 /srv/node3
sudo chmod 750 -R tomcat:tomcat /srv/node1/conf /srv/node2/conf /srv/node3/conf 
```

Let's configure the /srv/node1/conf/server.xml
```
<?xml version="1.0" encoding="UTF-8"?>
<Server port="9005" shutdown="SHUTDOWN">
  <Listener className="org.apache.catalina.startup.VersionLoggerListener" />
  <Listener className="org.apache.catalina.core.ThreadLocalLeakPreventionListener" />
  <Service name="Catalina">
    <Connector port="9081" protocol="HTTP/1.1"
               connectionTimeout="20000"
               maxThreads="150" />
    <Engine name="Catalina" defaultHost="localhost" jvmRoute="node1">
      <Host name="localhost"  appBase="webapps"
            unpackWARs="true" autoDeploy="true">
        <Valve className="org.apache.catalina.valves.AccessLogValve" directory="logs"
               prefix="node1_access" suffix=".log"
               pattern="NODE1 %h %t $quot;%r&quot; %s %b %D" />
      </Host>
    </Engine>
  </Service>
</Server>
```

Let's configure the /srv/node2/conf/server.xml
```
<?xml version="1.0" encoding="UTF-8"?>
<Server port="9005" shutdown="SHUTDOWN">
  <Listener className="org.apache.catalina.startup.VersionLoggerListener" />
  <Listener className="org.apache.catalina.core.ThreadLocalLeakPreventionListener" />
  <Service name="Catalina">
    <Connector port="9081" protocol="HTTP/1.1"
               connectionTimeout="20000"
               maxThreads="150" />
    <Engine name="Catalina" defaultHost="localhost" jvmRoute="node2">
      <Host name="localhost"  appBase="webapps"
            unpackWARs="true" autoDeploy="true">
        <Valve className="org.apache.catalina.valves.AccessLogValve" directory="logs"
               prefix="node2_access" suffix=".log"
               pattern="NODE2 %h %t $quot;%r&quot; %s %b %D" />
      </Host>
    </Engine>
  </Service>
</Server>
```

Let's configure the /srv/node3/conf/server.xml
```
<?xml version="1.0" encoding="UTF-8"?>
<Server port="9005" shutdown="SHUTDOWN">
  <Listener className="org.apache.catalina.startup.VersionLoggerListener" />
  <Listener className="org.apache.catalina.core.ThreadLocalLeakPreventionListener" />
  <Service name="Catalina">
    <Connector port="9081" protocol="HTTP/1.1"
               connectionTimeout="20000"
               maxThreads="150" />
    <Engine name="Catalina" defaultHost="localhost" jvmRoute="node3">
      <Host name="localhost"  appBase="webapps"
            unpackWARs="true" autoDeploy="true">
        <Valve className="org.apache.catalina.valves.AccessLogValve" directory="logs"
               prefix="node3_access" suffix=".log"
               pattern="NODE3 %h %t $quot;%r&quot; %s %b %D" />
      </Host>
    </Engine>
  </Service>
</Server>
```



## Lab - Setup up a three-instance Tomcat topology
<pre>
  
</pre>

## Lab - Tomcat Clustering and Session Replication
<pre>
  
</pre>
