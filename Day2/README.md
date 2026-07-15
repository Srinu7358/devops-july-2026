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

## Lab - Setup a 3 instance Tomcat Topology

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
sudo chmod 750 -R /srv/node1/conf /srv/node2/conf /srv/node3/conf 
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
<Server port="9006" shutdown="SHUTDOWN">
  <Listener className="org.apache.catalina.startup.VersionLoggerListener" />
  <Listener className="org.apache.catalina.core.ThreadLocalLeakPreventionListener" />
  <Service name="Catalina">
    <Connector port="9082" protocol="HTTP/1.1"
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
<Server port="9007" shutdown="SHUTDOWN">
  <Listener className="org.apache.catalina.startup.VersionLoggerListener" />
  <Listener className="org.apache.catalina.core.ThreadLocalLeakPreventionListener" />
  <Service name="Catalina">
    <Connector port="9083" protocol="HTTP/1.1"
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

Let's configure the setenv.sh for node1, node2 and node3 tomcat servers

/srv/node1/bin/setenv.sh
```
#!/bin/bash
export JAVA_HOME=$(readlink -f $(which java) | sed "s:/bin/java::")

export CATALINA_OPTS="-Xms256m -Xmx512m \
  -XX:+UseG1GC \
  -Djava.awt.headless=true \
  -Dnode.name=node1"
```

/srv/node2/bin/setenv.sh
```
#!/bin/bash
export JAVA_HOME=$(readlink -f $(which java) | sed "s:/bin/java::")

export CATALINA_OPTS="-Xms256m -Xmx512m \
  -XX:+UseG1GC \
  -Djava.awt.headless=true \
  -Dnode.name=node2"
```

/srv/node3/bin/setenv.sh
```
#!/bin/bash
export JAVA_HOME=$(readlink -f $(which java) | sed "s:/bin/java::")

export CATALINA_OPTS="-Xms256m -Xmx512m \
  -XX:+UseG1GC \
  -Djava.awt.headless=true \
  -Dnode.name=node3"
```

Let's update the ownerships and permissions
```
sudo chown tomcat:tomcat /srv/node1/bin/setenv.sh \
                         /srv/node2/bin/setenv.sh \
                         /srv/node3/bin/setenv.sh

sudo chmod 750 /srv/node1/bin/setenv.sh \
               /srv/node2/bin/setenv.sh \
               /srv/node3/bin/setenv.sh
```

Let's configure the service files
/etc/systemd/system/tomcat-node1.service
```
[Unit]
Description=Apache Tomcat 11 - node1 (HTTP 9081)
After=network.target

[Service]
Type=forking
User=tomcat
Group=tomcat

Environment="CATALINA_HOME=/opt/tomcat11"
Environment="CATALINA_BASE=/srv/node1"
Environment="CATALINA_PID=/srv/node1/temp/tomcat.pid"

ExecStart=/opt/tomcat11/bin/startup.sh
ExecStop=/opt/tomcat11/bin/shutdown.sh

Restart=on-failure
RestartSec=10
SuccessExitStatus=143
[Install]
WantedBy=multi-user.target
```

/etc/systemd/system/tomcat-node2.service
```
[Unit]
Description=Apache Tomcat 11 - node2 (HTTP 9082)
After=network.target

[Service]
Type=forking
User=tomcat
Group=tomcat

Environment="CATALINA_HOME=/opt/tomcat11"
Environment="CATALINA_BASE=/srv/node2"
Environment="CATALINA_PID=/srv/node2/temp/tomcat.pid"

ExecStart=/opt/tomcat11/bin/startup.sh
ExecStop=/opt/tomcat11/bin/shutdown.sh

Restart=on-failure
RestartSec=10
SuccessExitStatus=143
[Install]
WantedBy=multi-user.target
```

/etc/systemd/system/tomcat-node3.service
```
[Unit]
Description=Apache Tomcat 11 - node3 (HTTP 9082)
After=network.target

[Service]
Type=forking
User=tomcat
Group=tomcat

Environment="CATALINA_HOME=/opt/tomcat11"
Environment="CATALINA_BASE=/srv/node3"
Environment="CATALINA_PID=/srv/node3/temp/tomcat.pid"

ExecStart=/opt/tomcat11/bin/startup.sh
ExecStop=/opt/tomcat11/bin/shutdown.sh

Restart=on-failure
RestartSec=10
SuccessExitStatus=143
[Install]
WantedBy=multi-user.target
```

Let's manage the services of node1, node2 and node3
```
sudo sed -i 's/\$quot;/\&quot;/g' /srv/node1/conf/server.xml
sudo sed -i 's/\$quot;/\&quot;/g' /srv/node2/conf/server.xml
sudo sed -i 's/\$quot;/\&quot;/g' /srv/node3/conf/server.xml

sudo systemctl daemon-reload
sudo systemctl enable tomcat-node1 tomcat-node2 tomcat-node3
sudo systemctl start tomcat-node1 tomcat-node2 tomcat-node3
sudo systemctl status tomcat-node1 tomcat-node2 tomcat-node3 --no-pager | grep -E "Active"
```

Check all the ports
```
sudo ss -tlnp | grep -E ":(9081|9082|9083|9005|9006|9007)\b"
```

Let's build our application
```
exit # make sure you run this as non-admin (student) user
cd ~/devops-july-2026
git pull
cd Day2/hello-app
mvn clean package
```

Let's deploy and confirm each nodes responds on its own port
```
cd ~/devops-july-2026/Day2/hello-app
for N in node1 node2 node3; do
  sudo cp target/hello.war /srv/$N/webapps/
  sudo chown tomcat:tomcat /srv/$N/webapps/hello.war
done

sleep 15 # autoDeploy scans every 10 seconds

ls -d /srv/node1/webapps/hello.war /srv/node2/webapps/hello.war /srv/node3/webapps/hello.war

curl http://localhost:9081/hello/
curl http://localhost:9082/hello/
curl http://localhost:9083/hello/

# Check the logs
curl -s http://localhost:9081/hello/ > /dev/null
curl -s http://localhost:9082/hello/ > /dev/null
curl -s http://localhost:9083/hello/ > /dev/null

sudo tail -1 /srv/node1/logs/node1_access.$(date +%F).log
sudo tail -1 /srv/node2/logs/node2_access.$(date +%F).log
sudo tail -1 /srv/node3/logs/node3_access.$(date +%F).log

# Stop one node and test HA

sudo systemctl stop tomcat-node2

curl -s http://localhost:9081/hello/ | grep "Node name" # works
curl -s http://localhost:9082/hello/                    # connection refused
curl -s http://localhost:9083/hello/ | grep "Node name" # works

sudo systemctl start tomcat-node2
```
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/20aea7b8-f990-4428-a4a2-f00c72faea5b" />
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/bb1855ea-64eb-415d-8eda-695b5a74195d" />

<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/1ba84055-5a41-42ef-9aeb-7be6eb45b6c5" />

<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/78482014-45af-470a-a0c5-985dd3914833" />

## Lab - Load Balancing with Sticky Sessions
Pre-requisite - you should have completed the previous exercise.
<pre>
- In the previous lab, we created 3 independent instances of tomcat
- We accessed those web servers independently using their respective ports
- In this lab, we are going to add a load-balancer in front of those 3 tomcat node instances using Httpd
</pre>

Let's build the counter application
```
cd ~/devops-july-2026
git pull
cd Day2/counter-app
mvn clean package
```
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/a4967458-b048-4de7-abbe-bc86cb844bab" />
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/f77e1aec-4b4a-4973-84dd-ea401a803584" />


Deploy the counter application into tomcat-node1, tomcat-node2 and tomcat-node3 servers
```
for N in node1 node2 node3; do
  sudo cp target/counter.war /srv/$N/webapps/
  sudo chown tomcat:tomcat /srv/$N/webapps/counter.war
done
```


Let's verify if our counter application is running in tomcat-node1, tomcat-node2 and tomcat-node3 servers
```
sleep 15

curl http://localhost:9081/counter/count
curl http://localhost:9082/counter/count
curl http://localhost:9083/counter/count
```
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/137d1973-5250-40e9-87a6-cbaa28c9836d" />

Let's enable the httpd modules to setup httpd as the load balancer
```
sudo a2enmod proxy proxy_http proxy_balancer lbmethod_byrequests slotmem_shm headers status
```
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/682c25da-fd3a-40e5-904a-23128c7479cf" />

Let's configure the httpd configuration to include our tomcat-node1, tomcat-node2 and tomcat-node3 servers
/etc/apache2/sites-available/tomcat-cluster.conf
```
<VirtualHost *:80>

    ServerName localhost

    ProxyRequests Off
    ProxyPreserveHost On

    # =================================================================
    #  The balancer. One BalancerMember per Tomcat node.
    #
    #  route=nodeN MUST match jvmRoute=nodeN in that node's server.xml,
    #  exactly, character for character. That is the entire mechanism
    #  behind sticky sessions: Tomcat writes the route into the session
    #  cookie, and httpd reads it back out.
    # =================================================================
    <Proxy "balancer://tomcatcluster">

        BalancerMember "http://127.0.0.1:9081" route=node1
        BalancerMember "http://127.0.0.1:9082" route=node2
        BalancerMember "http://127.0.0.1:9083" route=node3

        ProxySet stickysession=JSESSIONID|jsessionid
        ProxySet lbmethod=byrequests

    </Proxy>

    ProxyPass        /counter  balancer://tomcatcluster/counter
    ProxyPassReverse /counter  balancer://tomcatcluster/counter

    RequestHeader set X-Forwarded-Proto "http"

    # =================================================================
    #  Live view of every balancer member and its state.
    #  Locked to localhost. Never expose this.
    # =================================================================
    <Location "/balancer-manager">
        SetHandler balancer-manager
        Require ip 127.0.0.1
        Require ip ::1
    </Location>

    # The ! means "do NOT proxy this path". Without this line, httpd
    # forwards /balancer-manager to Tomcat and Tomcat returns 404.
    ProxyPass /balancer-manager !

    ErrorLog  ${APACHE_LOG_DIR}/cluster_error.log
    CustomLog ${APACHE_LOG_DIR}/cluster_access.log combined

</VirtualHost>
```
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/02d7c219-bd50-44e7-9d2b-03bb8f64f8b6" />

Let's makes sure the httpd does't server its html pages by disabling them
```
sudo a2dissite 000-default tiers 2>/dev/null
sudo a2ensite tomcat-cluster
sudo apachectl configtest        # must print exactly: Syntax OK
sudo systemctl reload apache2
```
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/ed7c5e56-8892-421d-b44f-48fa4a3a45da" />

Verify if the tomcat-node1, tomcat-node2 and tomcat-node3 all report ok under 
loadbalancer on your web browser
```
http://localhost/balancer-manager
```
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/70adb7a1-28db-4326-9c2e-b1a8d3868cae" />

Demonstrates sticky sessions work
```
rm -f /tmp/cookies.txt

for i in $(seq 1 5); do
  curl -s -c /tmp/cookies.txt -b /tmp/cookies.txt http://localhost/counter/count \
    | grep -E "Served by node|Hit count"
  echo "---"
done

grep JSESSIONID /tmp/cookies.txt
```
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/a407a0ee-963e-4946-b4e3-6bc18f0f7686" />

Note
<pre>
- Each request that lands on the same Tomcat node bumps up the counter
</pre>  

Let's kill the node that takes most request
```
rm -f /tmp/cookies.txt

for i in $(seq 1 7); do
  curl -s -c /tmp/cookies.txt -b /tmp/cookies.txt http://localhost/counter/count > /dev/null
done

curl -s -c /tmp/cookies.txt -b /tmp/cookies.txt http://localhost/counter/count
```
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/f7bf65a6-3875-4ad8-be39-ce84f58ae71e" />

Now, kill the node reported by the above curl
```
sudo systemctl stop tomcat-node2

# Same cookie file. Same user. Nothing changed on the client side at all.
curl -s -c /tmp/cookies.txt -b /tmp/cookies.txt http://localhost/counter/count
```
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/48efd572-fc76-4074-ad03-a287eae5c382" />

Note
<pre>
- The apache httpd loadbalancer nicely handled the node failure
- The Loadbalancer detected the node2 was down and failed over to node3
</pre> 

Bring back the node2
```
sudo systemctl start tomcat-node2 
sleep 15
curl -s -c /tmp/cookies.txt -b /tmp/cookies.txt http://localhost/counter/count
curl -s -c /tmp/cookies.txt -b /tmp/cookies.txt http://localhost/counter/count
curl -s -c /tmp/cookies.txt -b /tmp/cookies.txt http://localhost/counter/count
```
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/cd73a190-523d-4171-bbd2-d99a56a87227" />


## Lab - Tomcat Clustering and Session Replication
<pre>
  
</pre>
