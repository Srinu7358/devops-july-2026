#!/bin/bash
# Stands up the two-tier reverse proxy lab end to end.
# Assumes: /opt/tomcat11 exists, dist/ROOT.war and dist/api.war are built.
# Run:  sudo ./scripts/setup.sh
set -e

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

if [ ! -f "$ROOT/dist/ROOT.war" ] || [ ! -f "$ROOT/dist/api.war" ]; then
  echo "WARs not found in dist/. Run ./scripts/build.sh first (no sudo)."
  exit 1
fi

echo "=== 1. Users ==="
id tcweb >/dev/null 2>&1 || useradd -r -s /usr/sbin/nologin tcweb
id tcapp >/dev/null 2>&1 || useradd -r -s /usr/sbin/nologin tcapp

echo "=== 2. CATALINA_BASE directories ==="
for TIER in web app; do
  mkdir -p /srv/$TIER/{bin,conf,logs,webapps,work,temp}
  for f in web.xml context.xml logging.properties catalina.properties tomcat-users.xml; do
    cp /opt/tomcat11/conf/$f /srv/$TIER/conf/
  done
done

echo "=== 3. Config files ==="
cp "$ROOT/config/web-server.xml" /srv/web/conf/server.xml
cp "$ROOT/config/app-server.xml" /srv/app/conf/server.xml
cp "$ROOT/config/web-setenv.sh"  /srv/web/bin/setenv.sh
cp "$ROOT/config/app-setenv.sh"  /srv/app/bin/setenv.sh
chmod 750 /srv/web/bin/setenv.sh /srv/app/bin/setenv.sh

echo "=== 4. Deploy WARs ==="
cp "$ROOT/dist/ROOT.war" /srv/web/webapps/
cp "$ROOT/dist/api.war"  /srv/app/webapps/

echo "=== 5. Ownership ==="
chown -R tcweb:tcweb /srv/web
chown -R tcapp:tcapp /srv/app
chmod 750 /srv/web/conf /srv/app/conf

echo "=== 6. systemd units ==="
cp "$ROOT/config/tomcat-web.service" /etc/systemd/system/
cp "$ROOT/config/tomcat-app.service" /etc/systemd/system/
systemctl daemon-reload
systemctl enable --now tomcat-web tomcat-app
sleep 12

echo "=== 7. httpd ==="
a2enmod proxy proxy_http headers rewrite >/dev/null 2>&1
cp "$ROOT/config/tiers.conf" /etc/apache2/sites-available/tiers.conf
a2dissite 000-default >/dev/null 2>&1 || true
a2ensite tiers >/dev/null 2>&1
apachectl configtest
systemctl reload apache2

echo
echo "=== Done. Verifying each tier directly (before httpd) ==="
curl -s http://127.0.0.1:9091/ | grep "Served by" || echo "WEB TIER not answering"
curl -s http://127.0.0.1:9092/api/orders | grep tier || echo "APP TIER not answering"

echo
echo "=== Now test the routing through httpd on port 80 ==="
echo "  curl http://localhost/          -> WEB TIER"
echo "  curl http://localhost/api/orders -> APP TIER"
