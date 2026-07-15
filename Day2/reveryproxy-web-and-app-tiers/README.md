# Reverse Proxy Lab: Web Tier + App Tier

httpd on port 80 routing `/api/*` to an app-tier Tomcat and everything else to a
web-tier Tomcat, both behind one hostname. Two Tomcat instances share one
`CATALINA_HOME`.

```
                         :80
  browser  ─────────────► httpd (reverse proxy)
                            │
              ┌─────────────┴──────────────┐
         / and /web/*                  /api/*
              ▼                            ▼
      Tomcat WEB TIER :9091        Tomcat APP TIER :9092
      shutdown 9015                shutdown 9016
      CATALINA_BASE=/srv/web       CATALINA_BASE=/srv/app
      user tcweb                   user tcapp
      ROOT.war (serves /)          api.war (serves /api)
              └──── shared CATALINA_HOME=/opt/tomcat11 ────┘
```

Ports are in the 9000 range so they do not collide with existing Tomcat
instances on 8080/8090/8100.

## Prerequisites

- `/opt/tomcat11` exists (a Tomcat 11 install), Java 17+
- Apache httpd installed (`sudo apt install apache2`)
- Either Maven, OR the build script falls back to `javac` using
  `/opt/tomcat11/lib/servlet-api.jar`

## Contents

```
web-tier/          Maven project, WebTierServlet, builds ROOT.war (serves /)
app-tier/          Maven project, ApiServlet, builds api.war (serves /api)
config/            All server.xml, setenv.sh, systemd units, httpd vhost
scripts/build.sh   Build both WARs (Maven, or javac fallback)
scripts/setup.sh   Stand up the whole lab end to end
scripts/test-routing.sh   Prove routing from the access logs
```

## Quick start

```bash
# 1. Build the WARs (no sudo needed)
./scripts/build.sh

# 2. Stand everything up
sudo ./scripts/setup.sh

# 3. Prove the routing
./scripts/test-routing.sh
```

## What you should see

```bash
curl http://localhost/            # -> WEB TIER  (plain text)
curl http://localhost/api/orders  # -> APP TIER  (JSON)
```

Then check the two access logs. An `/api` request appears **only** in the app
tier's log. The web tier log stays silent for it. That silence is the proof
that httpd routed by path, and that the web tier never touched the request.

```bash
sudo tail /srv/web/logs/web_access.$(date +%F).log
sudo tail /srv/app/logs/app_access.$(date +%F).log
```

## Manual build (if you skip build.sh)

With Maven:
```bash
cd web-tier && mvn clean package   # produces target/ROOT.war
cd ../app-tier && mvn clean package # produces target/api.war
```

Without Maven, against Tomcat's own servlet API:
```bash
SERVLET_API=/opt/tomcat11/lib/servlet-api.jar
mkdir -p build/web/WEB-INF/classes
javac -cp $SERVLET_API --release 17 -d build/web/WEB-INF/classes \
      web-tier/src/main/java/org/tektutor/web/WebTierServlet.java
(cd build/web && jar -cf ../../ROOT.war .)
```

## Key configuration notes

- **`ProxyPass /api` is listed BEFORE `ProxyPass /`** in tiers.conf. First match
  wins. Reverse them and every request goes to the web tier with no error.
- **`proxyName` / `proxyPort`** on each Connector make `getServerPort()` return
  80, so the app never leaks its real port to the browser.
- **`RemoteIpValve` before `AccessLogValve`** so the log records the real client
  IP, not httpd's.
- **`address="127.0.0.1"`** binds each Tomcat to loopback. Only httpd can reach
  them. To fully close the bypass, also `sudo ufw deny 9091/tcp` and
  `9092/tcp`.
- **`setenv.sh` must be owned by the service user** (tcweb / tcapp). If root
  owns it, the JVM options are silently ignored. setup.sh handles this.

## Teardown

```bash
sudo systemctl disable --now tomcat-web tomcat-app
sudo rm /etc/systemd/system/tomcat-web.service /etc/systemd/system/tomcat-app.service
sudo systemctl daemon-reload
sudo a2dissite tiers && sudo a2ensite 000-default && sudo systemctl reload apache2
sudo rm -rf /srv/web /srv/app
```
