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

## Lab - Setup up a three-instance Tomcat topology
<pre>
  
</pre>

## Lab - Tomcat Clustering and Session Replication
<pre>
  
</pre>
