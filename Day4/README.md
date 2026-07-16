# Day 4

## Lab - App Server push plugin to restrict access to the Web server

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

## Info - Configuration Management Tool
<pre>
- are used by System Administrator or DevOps engineers to automate their administrative activities
- the assumption is, you already have a machine with OS ( Physical Server, Virtual Machine or a public cloud ec2 or Azure VM, etc )
- on the machine with OS, if you wish to install, configure then configuration management tools are very useful to automate
- what activities can be automated ?
  - software installations, uninstations, update, upgrade, patches
  - user management
  - network management, etc
- examples
  - Puppet
  - Chef
  - Salt (SaltStack)
  - Ansible
</pre>

## Puppet Overview
<pre>
- is the oldest configuration management tool
- it follows client/server architecture
- every configuration management supports a specific domain specific language (DSL) to automate stuffs 
- the DSL used by Puppet is Puppet language ( a proprietary declarative language )
- Puppet installation is very complex and time consuming
- learning curve is quite steep
- uses proprietary tools on the servers that needs to be managed by chef
- Puppet architecture is very complex
</pre>


## Chef Overview
<pre>
- is a configuration management tool
- it follows client/server architecture
- the domain specific language (DSL) - the automation language used by Chef
- the DSL used by Chef is Ruby ( scripting language )
- Chef installation is very complex and time consuming as Puppet
- Chef provides loads of tools, hence its very powerful and confusing
- learning curve is quite steep
- uses proprietary tools on the servers that needs to be managed by chef
- chef architecture is very complex
</pre>

## Ansible Overview
<pre>
- is a configuration management tool
- it is agent-less
- easy to learn
- easy to install
- follows simple architecture
- ansible nodes
  - these are servers we can perform automation using ansible 
  - dependent softwares
    - Unix/Linux/Mac Server
      - Python
      - SSH Server
    - Windows Server
      - Powershell
      - WinRM
- Ansible Controller Machine
  - the machine where ansible is installed is called Ansible Controller Machine(ACM)
  - it could a laptop/desktop
  - officially Ansible is only supported on Linux machines, but it works in Unix/Mac
  - Windows machine can't be used as a Ansible Controller Machine
  - Windows machine can be managed by Ansible
- Inventory
  - is a plain text file which follows an INI style format
  - captures connectivity details, IP address/hostname, username, password, ssh-key's etc
- comes in 3 flavours
  1. Ansible core - open source variant supports only command line
  2. AWX - supports webconsole, opensource, built on top of Ansible core
  3. Red Hat Ansible Tower - enterprise commercial product, built on top of AWX 
</pre>

## Ansible Core
<pre>
- this is developed in Python by Michael Deehan
- Michael Deehan is a former employee of Red Hat
- Michael Deehan founded a company called Ansible Inc and developed Ansible core as an open source product
- perfect alternate to Puppet/Chef
- supports only command line
- very well documented open source product
- agent less
- can be installed in Linux, Unix and Mac
- can manage Windows, Linux, Mac, etc., ansible nodes
- doesn't support role based access ( can't create different types of ansible users )
- doesn't historial logging mechanism
</pre>

## AWX
<pre>
- is developed on top of Ansible core
- supports webconsole but no command line
- it can be installed on a centralized server within your organization
- can be accessed from web browser only
- supports role based access control
- supports logs for each playbook execution
- you don't get any support
- can't develop ansible playbook, you can only run them
- which means we need ansible core to develop/write playbook
</pre>

## Red Hat Ansible Tower
<pre>
- it is developed on top of AWX
- functionally AWX and Ansible Tower(Ansible Automation Platform) are same
- you will world-wide support from Red Hat (an IBM company)
- which means we need ansible core to develop/write playbook
</pre>

## Ansible Modules
<pre>
- ansible supports many built-in ansible modules to automate
- for instance 
  - file module helps in creating files and folders with specific permissions
  - copy module helps in copying from/to ACM to ansible nodes and vice versa
  - all unix/linux/mac ansible modules are developed as Python scripts
  - all windows ansible modules are developed as Powershell scripts
  - we can also write out own custom ansible modules, when there is no readily available module to automate certain rare stuffs
</pre>

## Ansible Plugins
<pre>
- ansible plugins helps us extend the core functionality of ansible
- for instance
  - become plugin helps us perform certain tasks as sudo(administrative) users
</pre>

## Ansible Roles
<pre>
- is way we could follow best practices and ensure our automation code can be reused across many ansible playbooks
- ansible roles can't be executed directly, while they can be invoked via ansible playbooks
- ansible roles can be downloaded and installed via ansible-galaxy tool
- we could also develop our own ansible role
- For example
  - we could develop an ansible role to install Oracle Database in Windows 2016/2019 Server, Ubuntu Linux, etc
</pre>

## Ansible Collections
<pre>
- is a reusable code that has many different kinds of reusable code in ansible
- it could have one or more roles, custom modules, plugins, filters, etc.,
- it's a way we could package and distribute all the related playbooks, modules, plugins, etc in a single collection
</pre>


## Lab - Install docker community edition in Ubuntu
```
# Add Docker's official GPG key:
sudo apt update
sudo apt install ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

# Add the repository to Apt sources:
sudo tee /etc/apt/sources.list.d/docker.sources <<EOF
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}")
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
EOF

sudo apt update

sudo apt install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin -y

sudo usermod -aG docker $USER
newgrp $USER
sudo su student
docker --version
docker images
```

## Lab - Building a Custom Docker Image to use as an Ansible Node
```
cd ~/devops-july-2026
git pull
cd Day4/ansible/CustomDockerImageForAnsibleNode
ssh-keygen # Accept all defaults by hitting enter whenever it prompts something
cp ~/.ssh/id_ed25519.pub authorized_keys
docker build -t tektutor/ubuntu-ansible-node:1.0 .
docker images | grep ubuntu-ansible
```
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/cdd2e1ae-2fc5-4591-afed-c15fe69e4fbb" />
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/3597ec8c-9a98-42df-8af6-0ea2a95a6d91" />
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/6baa5991-37e3-40c3-af15-4c343e51ac74" />

## Lab - Let's create 2 ubuntu2 ansible node containers
```
docker images | grep ubuntu-ansible
docker run -d --name ubuntu1 --hostname ubuntu1 -p 2001:22 -p 8001:80 tektutor/ubuntu-ansible-node:1.0
docker run -d --name ubuntu2 --hostname ubuntu2 -p 2002:22 -p 8002:80 tektutor/ubuntu-ansible-node:1.0

# Check if the ansible node containers are running
docker ps
```
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/67b3433c-6e54-4477-a6d1-180982f69fff" />

## Lab - Test your ansible node containers for ssh connectivity without password
Try SSH connection to ubuntu1 ansible node container
```
ssh -p 2001 root@localhost
exit
```

Try SSH connection to ubuntu1 ansible node container
```
ssh -p 2002 root@localhost
exit
```
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/252336d3-34ba-42a5-8fda-fa13208b4971" />
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/e46148c4-24c4-455c-b4b5-87ff414fb76b" />

## Lab - Install Ansible core in Ubuntu
```
sudo apt install ansible-core -y
ansible --version
```
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/25182dce-7514-4bbd-a843-b6c2461e7395" />
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/aa6e01d2-1b7e-429d-8d50-03a6289312f1" />

## Lab - See if ansible is able your ansible node containers
This exericse confirms if ansible is able to communicate with the ubuntu1 and ubuntu2 ansible nodes.
```
cd ~/devops-july-2026
git pull
cd Day4/ansible/inventory

# Run ansible ad-hoc command to ping ubuntu1 and ubuntu2 ansible nodes
ansible -i hosts all -m ping
```
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/bb74664c-f4a6-419d-9375-dc8fb12c28fa" />

## Lab - Running an ansible playbook that installs apache2 server in Ubuntu ansible nodes
```
cd ~/devops-july-2026
git pull
cd Day4/ansible/playbook
cat hosts
cat install-apache-playbook.yml

ansible-playbook -i hosts install-apache-playbook.yml
```

<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/1928f1bf-c8e0-47a3-819b-4306489462c0" />
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/18a3a1ea-2868-432c-bd31-574db437b1cc" />

Accessing the web pages from apache running on ubuntu1 and ubuntu2 ansible nodes
```
curl http://localhost:8001
curl http://localhost:8002
```
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/0b8a73ae-d4fe-4b12-9f7c-fc7d9b1bcce6" />
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/7461ea62-9f6e-46b1-8caa-3a14584cb754" />

## Lab - Setup Ansible Tower (AWX - Opensource)
Install minikube
```
curl -LO https://github.com/kubernetes/minikube/releases/latest/download/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube && rm minikube-linux-amd64
minikube start --cpus=4 --memory=8g

curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"

kubectl get storageclass          # need "standard (default)"
kubectl get svc -A -o jsonpath='{range .items[*]}{range .spec.ports[*]}{.nodePort}{"\n"}{end}{end}' | sort -n | uniq
```

Deploy AWX operator
```
git clone https://github.com/ansible/awx-operator.git
cd awx-operator
git checkout 2.19.1
make deploy IMG=quay.io/ansible/awx-operator:2.19.1

kubectl -n awx set image deploy/awx-operator-controller-manager \
  kube-rbac-proxy=quay.io/brancz/kube-rbac-proxy:v0.15.0
kubectl -n awx rollout status deploy/awx-operator-controller-manager
kubectl -n awx logs deploy/awx-operator-controller-manager -c awx-manager --tail=5
```

Create the AWX Instance
```
cat <<'EOF' | kubectl apply -f -
apiVersion: awx.ansible.com/v1beta1
kind: AWX
metadata:
  name: awx
  namespace: awx
spec:
  service_type: nodeport
  nodeport_port: 30090
EOF
```

Wait and check
```
kubectl -n awx get pods -w
# Login
minikube ip
kubectl -n awx get secret awx-admin-password -o jsonpath='{.data.password}' | base64 -d; echo
```

When reconcile fails, this is how you could get it fixed
```
kubectl -n awx patch awx awx --type=merge -p '{"spec":{"no_log":false}}'
kubectl -n awx logs -f deploy/awx-operator-controller-manager -c awx-manager | grep -A40 'Apply Resources'
kubectl -n awx patch awx awx --type=merge -p '{"spec":{"no_log":true}}'
```

Setup your password
```
kubectl -n awx create secret generic awx-admin-password \
  --from-literal=password='awx@123'
```

<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/7f40dabd-bff7-4674-9bff-5320627e6104" />
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/88a2cb4e-829f-41a0-8c78-842dc82c09b4" />
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/222f50c2-f0ed-4b63-ba21-2375375eb43f" />

## Lab - Install Ansible Tower CLI tool
To install tower-cli in Ubuntu
```
sudo apt update && sudo apt install -y python3 python3-pip
pip install ansible-tower-cli --break-system-packages --upgrade
```

## Lab - Invoking Ansible Tower Job Template from command-line ( used for Test automation )
```
tower-cli config username admin
tower-cli config password wv8zQPujAdLauJt32U3wznpnJyKuiCcN
tower-cli config verify_ssl false
tower-cli config insecure true
tower-cli config host http://192.168.49.2:30090
tower-cli login admin
tower-cli project list
tower-cli job list
tower-cli tower-cli job launch -J "Invoke Install Apache Playbook"
```
