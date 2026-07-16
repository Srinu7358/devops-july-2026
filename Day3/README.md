# Day 3

## Lab1 - Replicate a session across two Apache instances

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

## Lab2 - App Server push plugin to restrict access to the Web server

Pre-flight cleanup (clear stale instances from a previous run)
```
sudo systemctl stop tomcat-webtier tomcat-apptier 2>/dev/null
sudo pkill -f '/srv/webtier'; sudo pkill -f '/srv/apptier'
sleep 2
sudo ss -ltnp | grep -E ':(9091|9092|9015|9016)' || echo "clear to start"
```

Read your Tomcat paths
```
systemctl cat tomcat-node1 | grep -E "CATALINA_HOME|JAVA_HOME|User"
export CATALINA_HOME=/opt/tomcat11
export TC_USER=tomcat
```

Create webtier and apptier tomcat instances
```
for inst in webtier apptier; do
  sudo mkdir -p /srv/$inst/{conf,logs,temp,webapps,work,bin}
  sudo cp -r $CATALINA_HOME/conf/* /srv/$inst/conf/
done
sudo chown -R $TC_USER:$TC_USER /srv/webtier /srv/apptier
```

Configure Web-tier ports
```
sudo sed -i 's/port="8100"/port="9091"/' /srv/webtier/conf/server.xml
sudo sed -i 's/<Server port="8007"/<Server port="9015"/' /srv/webtier/conf/server.xml
sudo sed -i '/<Connector port="8443"/,/\/>/d' /srv/webtier/conf/server.xml
grep -nE '<Server port=|<Connector port=' /srv/webtier/conf/server.xml
```

Configure App-tier ports
```
sudo sed -i 's/port="8100"/port="9092"/' /srv/apptier/conf/server.xml
sudo sed -i 's/<Server port="8007"/<Server port="9016"/' /srv/apptier/conf/server.xml
sudo sed -i '/<Connector port="8443"/,/\/>/d' /srv/apptier/conf/server.xml
grep -nE '<Server port=|<Connector port=' /srv/apptier/conf/server.xml
```

Bind to loopback
```
sudo sed -i 's#<Connector port="9091" protocol="HTTP/1.1"#<Connector port="9091" address="127.0.0.1" protocol="HTTP/1.1"#' /srv/webtier/conf/server.xml
sudo sed -i 's#<Connector port="9092" protocol="HTTP/1.1"#<Connector port="9092" address="127.0.0.1" protocol="HTTP/1.1"#' /srv/apptier/conf/server.xml
```

Check ports free
```
sudo ss -ltnp | grep -E ':(9091|9092|9015|9016)' && echo CLASH || echo "ports free"
```

Install systemd units (edit CATALINA_HOME/JAVA_HOME/User in both first)
```
sed -i 's#/opt/tomcat\b#/opt/tomcat11#g' systemd/tomcat-webtier.service systemd/tomcat-apptier.service
sudo cp systemd/tomcat-webtier.service /etc/systemd/system/
sudo cp systemd/tomcat-apptier.service /etc/systemd/system/
sudo systemctl daemon-reload
```

Build your application 
```
cd web-tier && mvn -q clean package && cd ..
cd app-tier && mvn -q clean package && cd ..
```

Verify descriptor in WAR (must print WEB-INF/web.xml)
```
unzip -l web-tier/target/ROOT.war | grep -i web.xml
unzip -l app-tier/target/ROOT.war | grep -i web.xml
```

Deploy web tier
```
sudo rm -rf /srv/webtier/webapps/ROOT /srv/webtier/webapps/ROOT.war
sudo cp web-tier/target/ROOT.war /srv/webtier/webapps/
sudo chown $TC_USER:$TC_USER /srv/webtier/webapps/ROOT.war
```

Deploy app tier
```
sudo rm -rf /srv/apptier/webapps/ROOT /srv/apptier/webapps/ROOT.war
sudo cp app-tier/target/ROOT.war /srv/apptier/webapps/
sudo chown $TC_USER:$TC_USER /srv/apptier/webapps/ROOT.war
```

Start (web tier first)
```
sudo systemctl start tomcat-webtier; sleep 5
sudo systemctl start tomcat-apptier; sleep 5
systemctl is-active tomcat-webtier tomcat-apptier          # both must say active
sudo ss -ltnp | grep -E ':(9091|9092|9015|9016)'           # all four must listen
```

Confirm push
```
sudo grep -i "push plugin" /srv/apptier/logs/*.$(date +%F).log
curl -s http://127.0.0.1:9091/admin/allowlist              # real proof: prints the two paths
```

Push manually if empty
```
curl -s http://127.0.0.1:9092/admin/publish
```

Test A (expect app-tier text)
```
curl -s http://127.0.0.1:9091/api/products
curl -s http://127.0.0.1:9091/api/orders
```

Test B (200 direct, 403 via gateway)
```
curl -si http://127.0.0.1:9092/api/internal/metrics | head -1
curl -si http://127.0.0.1:9091/api/internal/metrics | head -1
```

One shot test
```
./scripts/verify.sh
```

Test C (dynamic control)
```
sudo sed -i 's#/api/products,/api/orders#/api/products#' /srv/apptier/webapps/ROOT/WEB-INF/web.xml
sleep 2
curl -s http://127.0.0.1:9092/admin/publish
curl -s http://127.0.0.1:9091/admin/allowlist
curl -si http://127.0.0.1:9091/api/orders | head -1
```

Restore
```
sudo sed -i 's#<param-value>/api/products</param-value>#<param-value>/api/products,/api/orders</param-value>#' /srv/apptier/webapps/ROOT/WEB-INF/web.xml
sleep 2
curl -s http://127.0.0.1:9092/admin/publish
```

Teardown to avoid conflicts on your next lab exercises
```
# Stop and disable the services
sudo systemctl stop tomcat-webtier tomcat-apptier 2>/dev/null
sudo systemctl disable tomcat-webtier tomcat-apptier 2>/dev/null

# Kill any leftover JVMs
sudo pkill -f '/srv/webtier'; sudo pkill -f '/srv/apptier'
sleep 2
sudo ss -ltnp | grep -E ':(9091|9092|9015|9016)' || echo "all ports free"

# Remove the systemd unit files
sudo rm -f /etc/systemd/system/tomcat-webtier.service
sudo rm -f /etc/systemd/system/tomcat-apptier.service
sudo systemctl daemon-reload
sudo systemctl reset-failed tomcat-webtier tomcat-apptier 2>/dev/null

# Remove the instance directories
sudo rm -rf /srv/webtier /srv/apptier
sudo a2disconf pushgate 2>/dev/null

# Remove the Apache proxy config (only if you added it)
sudo rm -f /etc/apache2/conf-available/pushgate.conf
sudo systemctl reload apache2 2>/dev/null || true

# Verify nothing is left
systemctl list-units --all 'tomcat-webtier*' 'tomcat-apptier*' --no-pager
sudo ss -ltnp | grep -E ':(9091|9092|9015|9016)' || echo "clean"
ls -d /srv/webtier /srv/apptier 2>/dev/null || echo "instances removed"
```

## Lab3 - Push-plugin access control with two Tomcat instances

Pre-flight cleanup (clear stale instances from a previous run)
```
sudo systemctl stop tomcat-webgw tomcat-appsvc 2>/dev/null
sudo pkill -f '/srv/webgw'; sudo pkill -f '/srv/appsvc'
sleep 2
sudo ss -ltnp | grep -E ':(8080|9090|8005|9095)' || echo "clear to start"
```

Read your Tomcat paths
```
systemctl cat tomcat-node1 | grep -E "CATALINA_HOME|JAVA_HOME|User"
export CATALINA_HOME=/opt/tomcat11
export TC_USER=tomcat
```

Create instances
```
for inst in webgw appsvc; do
  sudo mkdir -p /srv/$inst/{conf,logs,temp,webapps,work,bin}
  sudo cp -r $CATALINA_HOME/conf/* /srv/$inst/conf/
done
sudo chown -R $TC_USER:$TC_USER /srv/webgw /srv/appsvc
```

Configure Web-tier ports (8080 HTTP, 8005 shutdown)
```
sudo sed -i 's/port="8100"/port="8080"/' /srv/webgw/conf/server.xml
sudo sed -i 's/<Server port="8007"/<Server port="8005"/' /srv/webgw/conf/server.xml
sudo sed -i '/<Connector port="8443"/,/\/>/d' /srv/webgw/conf/server.xml
grep -nE '<Server port=|<Connector port=' /srv/webgw/conf/server.xml
```

Configure App-tier ports (9090 HTTP, 9095 shutdown)
```
sudo sed -i 's/port="8100"/port="9090"/' /srv/appsvc/conf/server.xml
sudo sed -i 's/<Server port="8007"/<Server port="9095"/' /srv/appsvc/conf/server.xml
sudo sed -i '/<Connector port="8443"/,/\/>/d' /srv/appsvc/conf/server.xml
grep -nE '<Server port=|<Connector port=' /srv/appsvc/conf/server.xml
```

Bind to loopback
```
sudo sed -i 's#<Connector port="8080" protocol="HTTP/1.1"#<Connector port="8080" address="127.0.0.1" protocol="HTTP/1.1"#' /srv/webgw/conf/server.xml
sudo sed -i 's#<Connector port="9090" protocol="HTTP/1.1"#<Connector port="9090" address="127.0.0.1" protocol="HTTP/1.1"#' /srv/appsvc/conf/server.xml
```

Check ports free
```
sudo ss -ltnp | grep -E ':(8080|9090|8005|9095)' && echo CLASH || echo "ports free"
```

Install systemd units (home already set to /opt/tomcat11)
```
sudo cp systemd/tomcat-webgw.service /etc/systemd/system/
sudo cp systemd/tomcat-appsvc.service /etc/systemd/system/
sudo systemctl daemon-reload
```

Build your application
```
cd ~/devops-july-2026
git pull
cd Day3/pluginconf-srv

cd web-tier && mvn -q clean package && cd ..
cd app-tier && mvn -q clean package && cd ..
```

Verify WAR contents (web tier must show web.xml AND plugin-allow.conf)
```
unzip -l web-tier/target/ROOT.war | grep -iE 'web.xml|plugin-allow.conf'
unzip -l app-tier/target/ROOT.war | grep -i web.xml
```

Deploy web tier
```
sudo rm -rf /srv/webgw/webapps/ROOT /srv/webgw/webapps/ROOT.war
sudo cp web-tier/target/ROOT.war /srv/webgw/webapps/
sudo chown $TC_USER:$TC_USER /srv/webgw/webapps/ROOT.war
```

Deploy app tier
```
sudo rm -rf /srv/appsvc/webapps/ROOT /srv/appsvc/webapps/ROOT.war
sudo cp app-tier/target/ROOT.war /srv/appsvc/webapps/
sudo chown $TC_USER:$TC_USER /srv/appsvc/webapps/ROOT.war
```

Start (web tier first)
```
sudo systemctl start tomcat-webgw; sleep 5
sudo systemctl start tomcat-appsvc; sleep 5
systemctl is-active tomcat-webgw tomcat-appsvc          # both must say active
sudo ss -ltnp | grep -E ':(8080|9090|8005|9095)'        # all four must listen
```

Confirm the app tier hosts all three paths (all 200 direct)
```
curl -s http://127.0.0.1:9090/shop
curl -s http://127.0.0.1:9090/reports
curl -s http://127.0.0.1:9090/admin
```

Confirm the push wrote the file (app tier auto-pushed /shop,/reports on startup)
```
sudo grep -i "push plugin" /srv/appsvc/logs/*.$(date +%F).log
curl -s http://127.0.0.1:8080/gateway/conf         # file shows /shop and /reports
curl -s http://127.0.0.1:8080/gateway/active       # empty until you reload
```

Reload the gateway to apply the pushed list
```
curl -s http://127.0.0.1:8080/gateway/reload -H "X-Push-Token: s3cr3t-web-tier"
curl -s http://127.0.0.1:8080/gateway/active       # now shows /shop, /reports
```

Test: /shop and /reports pass, /admin is blocked at the web tier
```
curl -si http://127.0.0.1:8080/shop    | head -1   # 200
curl -si http://127.0.0.1:8080/reports | head -1   # 200
curl -si http://127.0.0.1:8080/admin   | head -1   # 403
```

Add /admin to the allow-list (edit app-tier config), then push again
```
sudo sed -i 's#<param-value>/shop,/reports</param-value>#<param-value>/shop,/reports,/admin</param-value>#' /srv/appsvc/webapps/ROOT/WEB-INF/web.xml
sleep 2
curl -s http://127.0.0.1:9090/ops/publish
curl -s http://127.0.0.1:8080/gateway/conf         # file now includes /admin
curl -si http://127.0.0.1:8080/admin | head -1      # STILL 403 (not reloaded yet)
```

Reload, then confirm /admin now works through the web tier
```
curl -s http://127.0.0.1:8080/gateway/reload -H "X-Push-Token: s3cr3t-web-tier"
curl -si http://127.0.0.1:8080/admin | head -1      # 200
```

One-shot check
```
./scripts/verify.sh
```

## Lab4 - NAS Mounts across a Tomcat Fleet (NFS)

Install the NFS server and client packages
```
sudo apt-get update
sudo apt-get install -y nfs-kernel-server nfs-common rpcbind
```

Create the export directories
```
sudo mkdir -p /srv/nfs/appbin /srv/nfs/deploy /srv/nfs/logs
```

Set ownership so the tomcat fleet can read/write
```
sudo chown -R tomcat:tomcat /srv/nfs/deploy /srv/nfs/logs
sudo chmod 0775 /srv/nfs/deploy /srv/nfs/logs
sudo chmod 0755 /srv/nfs/appbin
# seed a marker so you can see the share is live
echo "shared binary drop" | sudo tee /srv/nfs/appbin/README.txt >/dev/null
```

Configure the exports (single host: export to 127.0.0.1)
```
sudo tee -a /etc/exports >/dev/null <<'EOF'
/srv/nfs/appbin  127.0.0.1(ro,sync,no_subtree_check)
/srv/nfs/deploy  127.0.0.1(rw,sync,no_subtree_check,no_root_squash)
/srv/nfs/logs    127.0.0.1(rw,sync,no_subtree_check,no_root_squash)
EOF
cat /etc/exports
```

Apply the exports and start the server
```
sudo exportfs -ra
sudo systemctl enable --now nfs-kernel-server
sudo systemctl is-active nfs-kernel-server
```

Verify the exports are published
```
sudo exportfs -v
showmount -e 127.0.0.1
```

Create the client mount points
```
sudo mkdir -p /mnt/appbin /mnt/deploy /mnt/logs
```

Mount manually with mount -t nfs (verify before making persistent)
```
sudo mount -t nfs 127.0.0.1:/srv/nfs/appbin /mnt/appbin
sudo mount -t nfs 127.0.0.1:/srv/nfs/deploy /mnt/deploy
sudo mount -t nfs 127.0.0.1:/srv/nfs/logs   /mnt/logs
```

Confirm the mounts and that the share content is visible
```
findmnt -t nfs
cat /mnt/appbin/README.txt        # proves the read-only binary share works
echo "hello from client" | sudo tee /mnt/deploy/test.txt >/dev/null
ls -l /srv/nfs/deploy/test.txt    # written through the mount, lands in the export
```

Make the mounts persistent in /etc/fstab with _netdev
```
sudo tee -a /etc/fstab >/dev/null <<'EOF'
127.0.0.1:/srv/nfs/appbin  /mnt/appbin  nfs  ro,hard,bg,_netdev,nofail,timeo=50,retrans=3  0 0
127.0.0.1:/srv/nfs/deploy  /mnt/deploy  nfs  rw,soft,bg,_netdev,nofail,timeo=30,retrans=3  0 0
127.0.0.1:/srv/nfs/logs    /mnt/logs    nfs  rw,soft,bg,_netdev,nofail,timeo=30,retrans=3  0 0
EOF
```

Test the fstab entries without rebooting
```
sudo umount /mnt/appbin /mnt/deploy /mnt/logs
sudo mount -a
findmnt -t nfs                    # all three should reappear from fstab
```

Soft vs hard mounts (know which to use where)
```
# hard  (fstab default): I/O retries forever. If the NAS stalls, a process
#        touching the mount blocks indefinitely and cannot be killed cleanly.
#        Safe for data integrity on writes; dangerous for availability.
# soft  : gives up after timeo*retrans, returns an I/O error instead of hanging.
#         Safe for availability; risks a truncated write on an interrupted op.
#
# Guidance used above:
#   appbin  -> ro,hard   read-only, integrity matters, never being written
#   deploy  -> rw,soft   a NAS stall must not wedge a deploy or a boot
#   logs    -> rw,soft   a NAS stall must not wedge Tomcat logging or startup
#
# timeo is tenths of a second: timeo=30 = 3s per try, retrans=3 = 3 tries.
```

Demonstrate the hung-mount risk
```
# stop the NAS to simulate an outage
sudo systemctl stop nfs-kernel-server

# soft mount: fails fast with an error (a few seconds), does NOT hang
timeout 20 ls /mnt/logs; echo "soft mount returned rc=$?"

# hard mount: blocks. timeout kills the ls so your shell survives the demo
timeout 8 ls /mnt/appbin; echo "hard mount returned rc=$? (124 = it hung)"

# bring the NAS back
sudo systemctl start nfs-kernel-server
```

Why a hung mount can block Tomcat startup, and how the fstab above prevents it
```
# The risk: if a Tomcat instance keeps webapps, logs, or CATALINA_BASE on a
# HARD NFS mount and the NAS is down at boot, any startup step that stats that
# path blocks forever. systemd waits on remote-fs, and the Tomcat unit (which
# runs After=network/remote-fs) never starts. One dead NAS wedges the fleet.
#
# The mitigations, all present in the fstab lines above:
#   _netdev  order the mount after the network is up
#   nofail   boot proceeds even if the mount fails; the unit is not blocked
#   bg       retry the mount in the background instead of blocking foreground
#   soft     for writable shares, return an error instead of hanging forever
#
# Extra safety for the Tomcat units: make them tolerant, not dependent.
#   In each unit add:  After=remote-fs.target   (order, not hard requirement)
#   Keep logs on 'soft' so a stalled NAS degrades logging instead of startup.
```

Wire the fleet to the shared folders (optional integration with node1)
```
# shared deployment drop: copy a WAR into the drop folder, deploy from there
sudo cp /srv/node1/webapps/ROOT.war /mnt/deploy/ 2>/dev/null || true
# a node can deploy by copying from the shared drop:
# sudo cp /mnt/deploy/app.war /srv/node1/webapps/

# shared logs: point node1's access logs at the shared mount
sudo mkdir -p /mnt/logs/node1
sudo chown tomcat:tomcat /mnt/logs/node1
# in /srv/node1/conf/server.xml, set the AccessLogValve directory to
#   directory="/mnt/logs/node1"   then restart node1
```

Verify the whole setup
```
findmnt -t nfs
df -h -t nfs
showmount -e 127.0.0.1
ls -l /mnt/appbin /mnt/deploy /mnt/logs
```

Teardown (important: unmount and clean fstab before the next lab, or a stale NFS entry can hang a future boot)
```
sudo umount -f -l /mnt/appbin /mnt/deploy /mnt/logs 2>/dev/null
sudo sed -i '\#127.0.0.1:/srv/nfs/#d' /etc/fstab
sudo sed -i '\#/srv/nfs/#d' /etc/exports
sudo exportfs -ra
# optional: stop the server and remove the data
# sudo systemctl disable --now nfs-kernel-server
# sudo rm -rf /srv/nfs
findmnt -t nfs || echo "no nfs mounts left"
```
