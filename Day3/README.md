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

## Lab5 - Mount NAS and scale to 3 Tomcat servers

Pre-flight (clean any prior NFS state so a stale mount can't hang this run)
```
sudo umount -f -l /export/shared 2>/dev/null; sudo umount -f -l /mnt/shared 2>/dev/null
sudo sed -i '\#/export/shared#d' /etc/fstab
findmnt -t nfs || echo "no nfs mounts"
```

Create and seed the export
```
sudo mkdir -p /export/shared
sudo chown -R tomcat:tomcat /export/shared
sudo chmod 0775 /export/shared
echo "shared appBase drop folder" | sudo tee /export/shared/README.txt >/dev/null
```

Export /export/shared over NFS (single host: export to loopback + local subnet)
```
sudo tee -a /etc/exports >/dev/null <<'EOF'
/export/shared  127.0.0.1(rw,sync,no_subtree_check,no_root_squash)
EOF
sudo exportfs -ra
sudo systemctl enable --now nfs-kernel-server
sudo exportfs -v
showmount -e 127.0.0.1
```

Mount on ONE Tomcat host manually first (verify before fstab)
```
sudo mkdir -p /mnt/shared
sudo mount -t nfs 127.0.0.1:/export/shared /mnt/shared
findmnt /mnt/shared
cat /mnt/shared/README.txt          # proves the mount works
```

Add the mount to /etc/fstab with _netdev
```
sudo tee -a /etc/fstab >/dev/null <<'EOF'
127.0.0.1:/export/shared  /mnt/shared  nfs  rw,soft,bg,_netdev,nofail,timeo=30,retrans=3  0 0
EOF
sudo umount /mnt/shared
sudo mount -a
findmnt /mnt/shared                 # reappears from fstab
```

Confirm Ansible reaches the fleet (inventory should already have tomcat_nodes)
```
ansible --version
ansible tomcat_nodes -m ping
ansible tomcat_nodes --list-hosts
```

If tomcat_nodes is not defined yet, create a minimal inventory
```
mkdir -p ~/devops-july-2026/Day3/nas-ansible
cat > ~/devops-july-2026/Day3/nas-ansible/inventory.ini <<'EOF'
[tomcat_nodes]
node1 ansible_host=127.0.0.1
node2 ansible_host=127.0.0.1
node3 ansible_host=127.0.0.1

[tomcat_nodes:vars]
ansible_connection=local
EOF
cat ~/devops-july-2026/Day3/nas-ansible/inventory.ini
```

Write the Ansible play to mount the share fleet-wide
```
cat > ~/devops-july-2026/Day3/nas-ansible/mount-nas.yml <<'EOF'
---
- name: Mount shared NFS export on the Tomcat fleet
  hosts: tomcat_nodes
  become: true
  vars:
    nfs_server: "127.0.0.1"
    nfs_export: "/export/shared"
    mount_point: "/mnt/shared"
  tasks:
    - name: Ensure nfs-common is installed
      ansible.builtin.package:
        name: nfs-common
        state: present

    - name: Ensure mount point exists
      ansible.builtin.file:
        path: "{{ mount_point }}"
        state: directory
        mode: "0775"

    - name: Mount the export and persist it in /etc/fstab
      ansible.posix.mount:
        path: "{{ mount_point }}"
        src: "{{ nfs_server }}:{{ nfs_export }}"
        fstype: nfs
        opts: "rw,soft,bg,_netdev,nofail,timeo=30,retrans=3"
        state: mounted
EOF
cat ~/devops-july-2026/Day3/nas-ansible/mount-nas.yml
```

Make sure the posix collection is present (provides ansible.posix.mount)
```
ansible-galaxy collection install ansible.posix
```

Run the play against the fleet
```
cd ~/devops-july-2026/Day3/nas-ansible
ansible-playbook -i inventory.ini mount-nas.yml
```

Verify the mount landed on all three nodes
```
ansible tomcat_nodes -b -m shell -a "findmnt /mnt/shared"
ansible tomcat_nodes -b -m shell -a "cat /mnt/shared/README.txt"
```

Prepare the shared appBase (one folder all three nodes deploy from)
```
sudo mkdir -p /export/shared/webapps
sudo chown -R tomcat:tomcat /export/shared/webapps
sudo chmod 0775 /export/shared/webapps
ls -ld /mnt/shared/webapps          # visible through every node's mount
```

Point each instance appBase at the shared folder
```
for n in node1 node2 node3; do
  sudo sed -i 's#appBase="webapps"#appBase="/mnt/shared/webapps" unpackWARs="false" autoDeploy="true"#' \
    /srv/$n/conf/server.xml
  grep -n 'appBase=' /srv/$n/conf/server.xml
done
```

Restart the fleet so the new appBase takes effect
```
for n in node1 node2 node3; do sudo systemctl restart tomcat-node$n; sleep 4; done
for n in node1 node2 node3; do
  echo -n "node$n: "; systemctl is-active tomcat-node$n
done
```

Drop ONE WAR into the shared folder and watch it deploy fleet-wide
```
# use any WAR you have; example uses the plugin-conf app-tier build
sudo cp ~/devops-july-2026/Day3/pluginconf-srv/app-tier/target/ROOT.war \
        /mnt/shared/webapps/shared.war
sudo chown tomcat:tomcat /mnt/shared/webapps/shared.war
sleep 8
```

Confirm all three nodes served the single dropped WAR
```
for p in 9081 9082 9083; do
  echo -n "port $p /shared/shop: "
  curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:$p/shared/shop
done
```

Verify the whole setup
```
ansible tomcat_nodes -b -m shell -a "findmnt /mnt/shared"
ls -l /mnt/shared/webapps
for p in 9081 9082 9083; do
  echo -n "port $p: "; curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:$p/shared/shop
done
```

Teardown (unmount fleet-wide and clean fstab before the next lab)
```
# revert appBase and restart
for n in node1 node2 node3; do
  sudo sed -i 's#appBase="/mnt/shared/webapps" unpackWARs="false" autoDeploy="true"#appBase="webapps"#' \
    /srv/$n/conf/server.xml
  sudo systemctl restart tomcat-node$n
done

# unmount on the fleet and remove fstab entry via Ansible
ansible tomcat_nodes -b -m ansible.posix.mount -a \
  "path=/mnt/shared state=absent"

# stop and clean the server side
sudo umount -f -l /mnt/shared 2>/dev/null
sudo sed -i '\#/export/shared#d' /etc/fstab /etc/exports
sudo exportfs -ra
findmnt -t nfs || echo "no nfs mounts left"
```

## Lab6 - Deploying applications into Appian (CD) 

Understand what "install" means here (read first, nothing to run)
```
# Appian Community Edition is a HOSTED cloud site, not a local install.
# There is no apt-get / installer / container you self-host on tektutor.
# You REQUEST a site and Appian provisions it at https://<yoursite>.appiancloud.com
#
# Keep it alive: log in at least once every 5 days or the site is shut down
# (a backup is kept for 28 days, after which the data is deleted).
```

Request your Appian Community Edition site
```
# 1. Create an Appian Community account:      https://community.appian.com
#    Verify email, set up MFA.
# 2. From "My Learning Journey", request your Community Edition environment.
# 3. Wait for the activation email, then log in to your site URL:
#      https://<yoursite>.appiancloud.com/suite/
# 4. Record your site domain; you will use it in every API call below.
export APPIAN_HOST="https://<yoursite>.appiancloud.com"
```

Enable the deployment APIs and create a service account + API key (in the site UI)
```
# In the Appian site, open the Admin Console (gear menu) and:
#   Infrastructure  -> enable the Deployment REST APIs (incoming + outgoing)
#   API Keys        -> create a service account and generate an API key
# Copy the key value once; you cannot see it again.
export APPIAN_API_KEY="<paste-the-api-key>"
```

Build the application to deploy (in the site UI)
```
# In Appian Designer:
#   Create a new Application (this is the container of design objects).
#   Add at least one object (e.g. an Interface or a Constant) so the package
#   has content to move.
#   Create a Package inside the application and add your object(s) to it.
# An Appian deployment moves a PACKAGE between environments.
```

Manual path first: Compare and Deploy (do this once to see the model)
```
# In Appian Designer, open the application -> Deploy -> Compare and Deploy
# (or Export) to move the package to a target environment or export a .zip.
# This is the human/manual equivalent of the API workflow below.
# Community Edition is a single site, so "target" may be the same site or an
# exported package file used to seed Git (next section).
```

Confirm the API is reachable (GET works regardless of Admin Console toggles)
```
# List deployments. GET endpoints are always available once the key exists.
curl -s "$APPIAN_HOST/suite/deployment-management/v2/deployments" \
  -H "appian-api-key: $APPIAN_API_KEY" | head
```

Get the package UUID you will deploy (Application Package Details)
```
# In Appian Designer, open the package and copy its UUID from the URL / details
# panel, OR use the Application Package Details view. Save it:
export PKG_UUID="<package-uuid>"
```

Automated path - Step 1: EXPORT the package
```
# Action-Type header selects export. Body identifies the package(s) by UUID.
# NOTE: the exact JSON schema varies by Appian version; confirm against
#   $APPIAN_HOST docs before relying on this in a pipeline.
curl -s -X POST "$APPIAN_HOST/suite/deployment-management/v2/deployments" \
  -H "appian-api-key: $APPIAN_API_KEY" \
  -H "Action-Type: export" \
  -F 'json={
        "name": "export-from-cli",
        "packageUuids": ["'"$PKG_UUID"'"]
      };type=application/json'
# The response returns a deployment UUID. Save it:
export DEP_UUID="<deployment-uuid-from-response>"
```

Automated path - Step 2: check EXPORT results and download resources
```
curl -s "$APPIAN_HOST/suite/deployment-management/v2/deployments/$DEP_UUID" \
  -H "appian-api-key: $APPIAN_API_KEY"
# When status is COMPLETED, the response links the exported package .zip and
# any DB scripts / plug-ins / import-customization files as resources.
```

Automated path - Step 3: INSPECT the package before importing
```
# Upload the exported package .zip to /inspections; inspecting first is the
# Appian-recommended safety step before any import.
curl -s -X POST "$APPIAN_HOST/suite/deployment-management/v2/inspections" \
  -H "appian-api-key: $APPIAN_API_KEY" \
  -F 'zipFile=@/path/to/exported-package.zip'
export INSPECT_UUID="<inspection-uuid-from-response>"
```

Automated path - Step 4: get INSPECT results
```
curl -s "$APPIAN_HOST/suite/deployment-management/v2/inspections/$INSPECT_UUID" \
  -H "appian-api-key: $APPIAN_API_KEY"
# Confirm there are no blocking problems before you import.
```

Automated path - Step 5: IMPORT the package
```
curl -s -X POST "$APPIAN_HOST/suite/deployment-management/v2/deployments" \
  -H "appian-api-key: $APPIAN_API_KEY" \
  -H "Action-Type: import" \
  -F 'json={ "name": "import-from-cli" };type=application/json' \
  -F 'zipFile=@/path/to/exported-package.zip'
export IMPORT_UUID="<deployment-uuid-from-response>"
```

Automated path - Step 6: get deployment RESULTS
```
curl -s "$APPIAN_HOST/suite/deployment-management/v2/deployments/$IMPORT_UUID" \
  -H "appian-api-key: $APPIAN_API_KEY"
# Poll until status is COMPLETED or FAILED.
```

Automated path - Step 7: get the deployment LOG
```
curl -s "$APPIAN_HOST/suite/deployment-management/v2/deployments/$IMPORT_UUID/log" \
  -H "appian-api-key: $APPIAN_API_KEY"
# Detailed per-object results for troubleshooting.
```

Automated Version Manager: store package files in Git
```
# Set up a Git repo that the pipeline exports into and imports from.
mkdir -p ~/devops-july-2026/Day4/appian-avm && cd $_
git init
mkdir -p packages
# After each export (Step 2), commit the .zip so Git is the source of truth:
# cp /path/to/exported-package.zip packages/
# git add packages/ && git commit -m "export: <package name> <increment>"
#
# Appian's Automated Version Manager (AVM) automates this: it versions the
# exported package files into Git so deployments are reproducible from source.
```

Configure the post-deployment process (in the site UI)
```
# Admin Console -> configure a post-deployment process to run automatically
# after an EXTERNAL (API) deployment finishes. Build a Process Model in the
# site, then select it as the post-deployment process. It runs on its own
# after Step 5 import completes (e.g. smoke checks, notifications, cache warmup).
```

Model the Jenkins pipeline from Appian's devops-quickstart
```
# Appian publishes a reference pipeline. Clone and read it, then adapt the
# stages to the curl calls above (export -> inspect -> import -> results -> log).
git clone https://github.com/appian/devops-quickstart.git \
  ~/devops-july-2026/Day4/devops-quickstart
cd ~/devops-july-2026/Day4/devops-quickstart
ls   # review the Jenkinsfile and scripts; map each stage to a v2 endpoint
```

Verify the whole flow
```
# 1. GET /deployments lists your export and import deployments.
curl -s "$APPIAN_HOST/suite/deployment-management/v2/deployments" \
  -H "appian-api-key: $APPIAN_API_KEY"
# 2. The imported objects appear in the target application in Appian Designer.
# 3. The post-deployment process shows an execution in the site's monitoring.
# 4. Git has the committed package .zip for the increment.
```

## Lab6 - Deploy an Appian application across environments

Understand the environment model (read first)
```
# Community Edition = ONE hosted site. There is no separate second environment.
# This lab models "two environments" as: export a package to a .zip artifact,
# then inspect+import that artifact back. Every endpoint is exercised.
# If your class has TWO Community sites, set a second host and import there
# instead; the steps are identical apart from the target URL.
export SRC_HOST="https://<yoursite>.appiancloud.com"
export TGT_HOST="$SRC_HOST"     # same site; change to a 2nd site if you have one
```

Set your API credentials (from the Admin Console)
```
# Admin Console -> Infrastructure: enable the Deployment REST APIs.
# Admin Console -> API Keys: create a service account, generate a key.
export APPIAN_API_KEY="<paste-the-api-key>"
export TGT_API_KEY="$APPIAN_API_KEY"   # change if the target is a 2nd site
```

Build the small application (in Appian Designer)
```
# Create a new Application (the container of design objects), then add:
#   1 Record Type   (e.g. "Ticket", backed by an Appian Data Store or synced)
#   1 Interface     (e.g. "TicketSummary", displays record data)
#   1 Process Model (e.g. "CreateTicket", one node that writes a record)
# Create a Package in the application and add all three objects to it.
# Copy the package UUID from the package details:
export PKG_UUID="<package-uuid>"
```

Manual export from the UI (Compare and Deploy / Export)
```
# In Appian Designer: open the application -> Deploy -> Export (or
# Compare and Deploy). Download the exported package .zip.
# Save it as the v1 artifact:
mkdir -p ~/devops-july-2026/Day4/appian-xenv/packages
mv ~/Downloads/<exported>.zip \
   ~/devops-july-2026/Day4/appian-xenv/packages/ticket-app-v1.zip
```

Export again with the API (same package, now scripted)
```
curl -s -X POST "$SRC_HOST/suite/deployment-management/v2/deployments" \
  -H "appian-api-key: $APPIAN_API_KEY" \
  -H "Action-Type: export" \
  -F 'json={
        "name": "export-ticket-app",
        "packageUuids": ["'"$PKG_UUID"'"]
      };type=application/json'
# Response returns a deployment UUID:
export DEP_UUID="<deployment-uuid-from-response>"
```

Poll the export until COMPLETED, then download the package .zip
```
curl -s "$SRC_HOST/suite/deployment-management/v2/deployments/$DEP_UUID" \
  -H "appian-api-key: $APPIAN_API_KEY"
# When COMPLETED, the response links the exported package .zip resource.
# Download it (URL/field name is in that response) and save as the API artifact:
curl -s -L "<package-zip-resource-url>" \
  -H "appian-api-key: $APPIAN_API_KEY" \
  -o ~/devops-july-2026/Day4/appian-xenv/packages/ticket-app-api.zip
```

Inspect the package against the TARGET before importing
```
curl -s -X POST "$TGT_HOST/suite/deployment-management/v2/inspections" \
  -H "appian-api-key: $TGT_API_KEY" \
  -F 'zipFile=@'"$HOME"'/devops-july-2026/Day4/appian-xenv/packages/ticket-app-api.zip'
export INSPECT_UUID="<inspection-uuid-from-response>"
```

Read inspection results (must be clean before import)
```
curl -s "$TGT_HOST/suite/deployment-management/v2/inspections/$INSPECT_UUID" \
  -H "appian-api-key: $TGT_API_KEY"
# Confirm no blocking problems. Inspect-before-import is Appian's rule.
```

Import to the target environment
```
curl -s -X POST "$TGT_HOST/suite/deployment-management/v2/deployments" \
  -H "appian-api-key: $TGT_API_KEY" \
  -H "Action-Type: import" \
  -F 'json={ "name": "import-ticket-app-v1" };type=application/json' \
  -F 'zipFile=@'"$HOME"'/devops-july-2026/Day4/appian-xenv/packages/ticket-app-api.zip'
export IMPORT_UUID="<deployment-uuid-from-response>"
```

Read the deployment result and log
```
curl -s "$TGT_HOST/suite/deployment-management/v2/deployments/$IMPORT_UUID" \
  -H "appian-api-key: $TGT_API_KEY"                       # poll to COMPLETED
curl -s "$TGT_HOST/suite/deployment-management/v2/deployments/$IMPORT_UUID/log" \
  -H "appian-api-key: $TGT_API_KEY"                       # per-object detail
```

Put the package artifacts under Automated Version Manager (Git)
```
cd ~/devops-july-2026/Day4/appian-xenv
git init
git add packages/ticket-app-v1.zip packages/ticket-app-api.zip
git commit -m "ticket-app v1: manual + API export artifacts"
git tag ticket-app-v1
# Each future export commits a new versioned .zip; Git is the rollback source.
```

Wire export -> inspect -> import into a Jenkins job
```
# Store APPIAN_API_KEY and TGT_API_KEY as Jenkins credentials (not in the file).
cat > ~/devops-july-2026/Day4/appian-xenv/Jenkinsfile <<'EOF'
pipeline {
  agent any
  environment {
    SRC_HOST = 'https://<yoursite>.appiancloud.com'
    TGT_HOST = 'https://<yoursite>.appiancloud.com'
    PKG_UUID = '<package-uuid>'
    APPIAN_API_KEY = credentials('appian-src-api-key')
    TGT_API_KEY    = credentials('appian-tgt-api-key')
  }
  stages {
    stage('Export') {
      steps {
        sh '''
          DEP=$(curl -s -X POST "$SRC_HOST/suite/deployment-management/v2/deployments" \
            -H "appian-api-key: $APPIAN_API_KEY" -H "Action-Type: export" \
            -F 'json={"name":"jenkins-export","packageUuids":["'"$PKG_UUID"'"]};type=application/json' \
            | tee export.json)
          echo "$DEP"
          # parse the deployment uuid, poll until COMPLETED, download the .zip
          # (use jq; field names per your Appian version's docs)
        '''
      }
    }
    stage('Inspect') {
      steps {
        sh '''
          curl -s -X POST "$TGT_HOST/suite/deployment-management/v2/inspections" \
            -H "appian-api-key: $TGT_API_KEY" \
            -F 'zipFile=@package.zip' | tee inspect.json
          # fail the stage if inspection reports blocking problems
        '''
      }
    }
    stage('Import') {
      steps {
        sh '''
          curl -s -X POST "$TGT_HOST/suite/deployment-management/v2/deployments" \
            -H "appian-api-key: $TGT_API_KEY" -H "Action-Type: import" \
            -F 'json={"name":"jenkins-import"};type=application/json' \
            -F 'zipFile=@package.zip' | tee import.json
          # poll the import uuid to COMPLETED, then fetch /log; fail on FAILED
        '''
      }
    }
    stage('Archive to AVM') {
      steps {
        sh 'cp package.zip packages/ticket-app-${BUILD_NUMBER}.zip && git add packages && git commit -m "ci: ticket-app build ${BUILD_NUMBER}" || true'
      }
    }
  }
}
EOF
echo "Jenkinsfile written"
```

Reference the Appian devops-quickstart pipeline (adapt, don't reinvent)
```
git clone https://github.com/appian/devops-quickstart.git \
  ~/devops-july-2026/Day4/devops-quickstart
# Map its stages to the four above. It shows the polling + jq parsing patterns
# for the deployment UUID and status that the stubs above leave as comments.
```

Make a v2 change, export, and deploy (so you have something to roll back FROM)
```
# In Designer, make a small change to the Interface, re-export via the API
# (same Export steps), save as packages/ticket-app-v2.zip, import to target.
git add packages/ticket-app-v2.zip && git commit -m "ticket-app v2" && git tag ticket-app-v2
```

Roll back by redeploying the previous version from AVM
```
# Rollback in Appian = re-import the PREVIOUS package version. Pull it from Git.
cd ~/devops-july-2026/Day4/appian-xenv
git checkout ticket-app-v1 -- packages/ticket-app-v1.zip

# inspect the old version against the target first
curl -s -X POST "$TGT_HOST/suite/deployment-management/v2/inspections" \
  -H "appian-api-key: $TGT_API_KEY" \
  -F 'zipFile=@packages/ticket-app-v1.zip'
# then re-import v1 to revert the target
curl -s -X POST "$TGT_HOST/suite/deployment-management/v2/deployments" \
  -H "appian-api-key: $TGT_API_KEY" -H "Action-Type: import" \
  -F 'json={ "name": "rollback-to-v1" };type=application/json' \
  -F 'zipFile=@packages/ticket-app-v1.zip'
# read the log to confirm the revert
```

Verify the whole flow
```
# 1. GET /deployments lists export, import(v1), import(v2), rollback(v1).
curl -s "$TGT_HOST/suite/deployment-management/v2/deployments" \
  -H "appian-api-key: $TGT_API_KEY"
# 2. In Designer, the interface shows v1 content again after rollback.
# 3. Git tags ticket-app-v1 and ticket-app-v2 both hold their .zip artifacts.
# 4. Jenkins shows a green run through Export -> Inspect -> Import -> Archive.
```
