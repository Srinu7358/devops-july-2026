# Day 3

## Lab - Replicate a session across two Apache instances

We will setup 2 tomcat instances one that acts as a web tier and the other as app tier
<pre>
Tomcat Web - 8080
Tomcat App - 9080
</pre>

App tier endpoints
<pre>
/api/products - reachable via web tier
/api/orders   - reachable via web tier
/api/internal/metrics - not reachable via web tier
</pre>

Let's create 2 instances from one Tomcat installation
```
export CATALINA_HOME=/opt/tomcat11
for n in webtier apptier; do
mkdir -p /opt/tomcat11/$n/{conf,logs,temp,webapps,work,bin}
cp -r $CATALINA_HOME/conf/* /opt/tomcat/$n/conf/
done
```

Configure the ports to non-conflicts ports
```
<Server port="9005" shutdown="SHUTDOWN">

<Connector port="9080" protocol="HTTP/1.1"
connectionTimeout="20000"
address="127.0.0.1"
redirectPort="9443"/>

<Connector port="8080" protocol="HTTP/1.1"
connectionTimeout="20000"
address="127.0.0.1"
redirectPort="8443"/>
```

Build the web and app tier
```
export CATALINA_HOME=/opt/tomcat11
cd web-tier && ./build.sh && cd ..
cd app-tier && ./build.sh && cd ..

# Deploy
cp web-tier/ROOT.war /opt/tomcat/webtier/webapps/
cp app-tier/ROOT.war /opt/tomcat/apptier/webapps/
```

Start the web tier first
```
CATALINA_BASE=/opt/tomcat/webtier $CATALINA_HOME/bin/catalina.sh start
CATALINA_BASE=/opt/tomcat/apptier $CATALINA_HOME/bin/catalina.sh start
```

Confirm the push landed
```
grep -i "push plugin" /opt/tomcat/apptier/logs/catalina.out
# push plugin: pushed to web tier, reply 200 (stored 2 paths
```

Read the allow-list
```
curl http://127.0.0.1:8080/admin/allowlist
# /api/products
# /api/orders
curl http://127.0.0.1:9080/admin/publish
```

Test the web tier forwards only allowed list
```
curl http://127.0.0.1:8080/api/products
# app-tier: products list [public]
curl http://127.0.0.1:8080/api/orders
# app-tier: orders list [public
```

Unapproved list must be denied
```
# This should not work, should return 404
curl -i http://127.0.0.1:8080/api/internal/metrics

# This should work
curl -i http://127.0.0.1:9080/api/internal/metrics
```


Test if app tier controls access dynamically
```
curl http://127.0.0.1:9080/admin/publish
curl http://127.0.0.1:8080/admin/allowlist
# /api/products
curl -i http://127.0.0.1:8080/api/orders
# HTTP/1.1 403
# blocked at web tier: /api/orders
```

Stop both instances
```
CATALINA_BASE=/opt/tomcat/apptier $CATALINA_HOME/bin/catalina.sh stop
CATALINA_BASE=/opt/tomcat/webtier $CATALINA_HOME/bin/catalina.sh stop
```




