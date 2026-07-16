# Day 4

## Info - IIS as the web and application host for .NET
<pre>
- IIS (Internet Information Services) is Windows built-in web server
- For .NET it plays two roles at once
  - the web server that terminates HTTP/HTTPS and serves static content and 
  - the process host that launches and supervises your application's worker processes
- The rough Tomcat parallel, since you've been living in that world
  - IIS is like Apache httpd and the servlet container's process management fused into one Windows service
  
- Application pools
  - An application pool is the isolation and process boundary
  - Each pool runs one or more worker processes (w3wp.exe), and sites assigned to a pool share 
    that process space
  - Pools are how you isolate apps from each other on the same server, one crashing app doesn't take 
    down the others
  
- CLR version
  - Historically this selected the .NET Framework runtime (v2.0 vs v4.0). 
  - The critical modern gotcha: for ASP.NET Core, you set this to "No Managed Code." ASP.NET Core doesn't 
    load the .NET Framework CLR into the worker, the app runs its own .NET (Core) runtime out of process or 
    in a native module, so the pool shouldn't load a managed runtime at all
  - Setting a CLR version for a Core app is a common misconfiguration
  
- Pipeline mode
  - Integrated vs Classic. 
    - Integrated (the default and correct choice for anything modern) merges the IIS and ASP.NET request 
      pipelines so managed modules see every request. 
    - Classic mode emulates old IIS 6 behavior and exists only for legacy apps.
  
- Identity
  - The Windows account the worker runs as. ApplicationPoolIdentity is the default and the right choice for 
    most cases, a virtual, per-pool account (IIS AppPool\<PoolName>) with minimal rights, so you grant file/DB 
    access to that specific identity
  - Alternatives are NetworkService, LocalService, LocalSystem (avoid, over-privileged), or a specific domain/local 
    account when the app needs particular network or database credentials.
  - This is the direct analog of the tomcat user you've been running instances as.

- Recycling
  - IIS periodically tears down and restarts worker processes to shed leaked memory and stale state. 
  - Triggers include a fixed time interval (default was 29 hours / 1740 minutes), specific clock times, 
    a request count, or private-memory / virtual-memory thresholds. 
  - Recycling overlaps by default (new worker spins up before the old one drains) so it's normally graceful. 
  - The thing to know
    - recycling drops in-process session state and anything cached in memory, which is exactly the failure 
      mode you just spent hours on with Tomcat session replication, same lesson, different host. 
    - For Core apps recycling matters less since state usually lives outside the worker.

- Sites and bindings
  - A site is a top-level unit with its own root path and one or more bindings
  - Under a site you can have applications (each mapped to a pool and a virtual path) and virtual directories 
    (path aliases to content elsewhere on disk).

  - A binding is how IIS decides which site answers a request. 
  - A binding is the tuple: protocol + IP address + port + host header (plus an optional SNI/certificate for HTTPS). 
  - Examples: 
    - http/*:80 catches all host names on port 80
    - https/*:443 with host header shop.example.com and a bound certificate serves just that host over TLS
</pre>

## Lab - Deploy a Hello World application to IIS

Make sure you installed .Net 8 and the correct bundle
```
https://builds.dotnet.microsft.com/dotnet/aspnetcore/Runtime/8.0.29/dotnet-hosting-8.0.29-win.exe
```

Confirm the box and admin rights
```
Get-ComputerInfo | Select WindowsProductName, OsVersion, OsServerLevel
whoami /groups | findstr /i "S-1-5-32-544"    # Administrators present
```

Install the IIS role FIRST (before the Hosting Bundle)
```
Install-WindowsFeature -Name Web-Server -IncludeManagementTools
# useful sub-features for this lab
Install-WindowsFeature Web-Mgmt-Console, Web-Scripting-Tools
Get-Service W3SVC                              # should be Running
```

Install the .NET SDK (to build the app)
```
# If winget is present (Desktop Experience usually has it):
winget install Microsoft.DotNet.SDK.8
# If winget is NOT available (common on Server Core), download the SDK installer:
#   https://dotnet.microsoft.com/download/dotnet/8.0  -> SDK x64 -> run silently:
#   Start-Process .\dotnet-sdk-8.x.x-win-x64.exe -ArgumentList '/quiet /norestart' -Wait
dotnet --version
```

Install the .NET Hosting Bundle AFTER IIS (registers the ASP.NET Core Module)
```
winget install Microsoft.DotNet.HostingBundle.8
# or download "ASP.NET Core Hosting Bundle" from the same .NET 8 page and:
#   Start-Process .\dotnet-hosting-8.x.x-win.exe -ArgumentList '/quiet /norestart' -Wait

# reload IIS so ANCM (AspNetCoreModuleV2) registers
net stop was /y
net start w3svc
```

Confirm the ASP.NET Core Module registered
```
Import-Module WebAdministration
Get-WebGlobalModule | Where-Object { $_.Name -like "AspNetCore*" }
# expect: AspNetCoreModuleV2
```

Create the minimal app
```
if (-not (Test-Path C:\labs)) { New-Item -ItemType Directory C:\labs }
Set-Location C:\labs
dotnet new web -o HelloIIS
Set-Location C:\labs\HelloIIS
```

Sanity-check locally, then stop
```
dotnet run
# browse the shown http://localhost:5xxx, confirm "Hello World!", then Ctrl+C
```

Publish a Release build
```
dotnet publish -c Release -o C:\labs\HelloIIS\publish
Get-ChildItem C:\labs\HelloIIS\publish        # note web.config is generated here
```

Create an application pool set to No Managed Code
```
New-WebAppPool -Name "HelloIISPool"
Set-ItemProperty IIS:\AppPools\HelloIISPool -Name managedRuntimeVersion -Value ""   # No Managed Code
Set-ItemProperty IIS:\AppPools\HelloIISPool -Name managedPipelineMode  -Value 0     # Integrated
Get-ItemProperty IIS:\AppPools\HelloIISPool | Select name, managedRuntimeVersion, managedPipelineMode
```

Grant the pool identity read/execute on the publish folder
```
icacls C:\labs\HelloIIS\publish /grant "IIS AppPool\HelloIISPool:(OI)(CI)RX" /T
```

Create a site bound to port 8088 pointing at the publish folder
```
New-WebSite -Name "HelloIIS" `
            -PhysicalPath "C:\labs\HelloIIS\publish" `
            -ApplicationPool "HelloIISPool" `
            -Port 8088
Start-WebSite -Name "HelloIIS"
Get-WebSite -Name "HelloIIS"
```

Open the firewall for port 8088
```
New-NetFirewallRule -DisplayName "IIS 8088" -Direction Inbound -Protocol TCP -LocalPort 8088 -Action Allow
```

Browse and confirm
```
(Invoke-WebRequest http://localhost:8088 -UseBasicParsing).Content    # Hello World!
(Invoke-WebRequest http://localhost:8088 -UseBasicParsing).StatusCode # 200
```

Read the generated web.config
```
Get-Content C:\labs\HelloIIS\publish\web.config
```

Verify the whole setu
```
Get-Service W3SVC
Get-ItemProperty IIS:\AppPools\HelloIISPool | Select name, state, managedRuntimeVersion
Get-WebBinding -Name "HelloIIS"
Get-WebGlobalModule | ? { $_.Name -like "AspNetCore*" }
```

Teardown (clean site, pool, port before the next lab)
```
Remove-WebSite -Name "HelloIIS"
Remove-WebAppPool -Name "HelloIISPool"
Remove-NetFirewallRule -DisplayName "IIS 8088" -ErrorAction SilentlyContinue
# Remove-Item C:\labs\HelloIIS -Recurse -Force
```

## Info - MSI Overview
<pre>
- An MSI is not a script
- It's a relational database in a single .msi file
- a set of tables (built on the old COM structured-storage format) that describe the desired 
  end state of an installation
  - the files to lay down
  - the registry keys to write
  - shortcuts, services, and the ordered actions to get there. 
  - The Windows Installer service (msiexec.exe / msiserver) reads that database 
    and executes it transactionally.
</pre>

## Lab - Install and verify an MSI silently

Confirm admin rights and .NET SDK (SDK came with the IIS lab)
```
whoami /groups | findstr /i "S-1-5-32-544"     # Administrators present
dotnet --version                                # SDK present (needed only to build the sample)
```

Install the WiX build tool
```
dotnet tool install --global wix
wix --version
```

Create the WiX source for a trivial payload
```
if (-not (Test-Path C:\labs\msi)) { New-Item -ItemType Directory C:\labs\msi -Force }
Set-Location C:\labs\msi

# a trivial file to install
"HelloMSI payload $(Get-Date)" | Out-File -Encoding utf8 C:\labs\msi\hello.txt

# generate a stable UpgradeCode once and reuse it
$UpgradeCode = [guid]::NewGuid().ToString()
$UpgradeCode | Out-File C:\labs\msi\upgradecode.txt
"UpgradeCode = $UpgradeCode"
```

Write HelloMSI.wxs (paste the UpgradeCode from above into UpgradeCode="...")
```
@'
<Wix xmlns="http://wixtoolset.org/schemas/v4/wxs">
  <Package Name="HelloMSI" Manufacturer="TekTutor"
           Version="1.0.0.0" UpgradeCode="PUT-UPGRADECODE-HERE">
    <MajorUpgrade DowngradeErrorMessage="A newer version is already installed." />
    <MediaTemplate EmbedCab="yes" />

    <Feature Id="Main" Title="HelloMSI" Level="1">
      <ComponentGroupRef Id="AppFiles" />
    </Feature>
  </Package>

  <Fragment>
    <StandardDirectory Id="ProgramFiles64Folder">
      <Directory Id="INSTALLFOLDER" Name="HelloMSI" />
    </StandardDirectory>
  </Fragment>

  <Fragment>
    <ComponentGroup Id="AppFiles" Directory="INSTALLFOLDER">
      <Component>
        <File Source="C:\labs\msi\hello.txt" />
      </Component>
    </ComponentGroup>
  </Fragment>
</Wix>
'@ | Out-File -Encoding utf8 C:\labs\msi\HelloMSI.wxs

# inject the real UpgradeCode
(Get-Content C:\labs\msi\HelloMSI.wxs) `
  -replace 'PUT-UPGRADECODE-HERE', (Get-Content C:\labs\msi\upgradecode.txt) |
  Set-Content C:\labs\msi\HelloMSI.wxs
Get-Content C:\labs\msi\HelloMSI.wxs
```

Build the MSI
```
Set-Location C:\labs\msi
wix build HelloMSI.wxs -o C:\labs\msi\HelloMSI.msi
Get-Item C:\labs\msi\HelloMSI.msi
```

Set the target MSI
```
$Msi = "C:\labs\msi\HelloMSI.msi"
$Log = "C:\labs\msi\install.log"
```

Silent install with a full verbose log
```
$p = Start-Process msiexec.exe `
       -ArgumentList "/i `"$Msi`" /qn /norestart /l*v `"$Log`"" `
       -Wait -PassThru
"msiexec exit code: $($p.ExitCode)"
```

Interpret the exit code (0 and 3010 are BOTH success)
```
switch ($p.ExitCode) {
  0     { "SUCCESS (no reboot needed)" }
  3010  { "SUCCESS (reboot required)"  }
  1602  { "FAIL: user cancelled" }
  1603  { "FAIL: fatal error during install (read the log)" }
  1618  { "FAIL: another install already in progress" }
  1619  { "FAIL: MSI could not be opened (bad path?)" }
  1620  { "FAIL: MSI could not be opened (bad package)" }
  default { "Exit code $($p.ExitCode) - look it up / read the log" }
}
```
Parse the verbose log for the outcome
```
# the line that reports the overall result
Select-String -Path $Log -Pattern "Installation success or error status" | Select -Last 1
# any failed action shows "Return value 3" (this is what triggers rollback)
Select-String -Path $Log -Pattern "Return value 3"
# the human summary near the end
Select-String -Path $Log -Pattern "Product: .* Installation (completed|failed)"
```

Confirm the payload actually landed
```
Test-Path "C:\Program Files\HelloMSI\hello.txt"     # expect True
Get-Content "C:\Program Files\HelloMSI\hello.txt"
```

Confirm the product is registered with Windows Installer
```
Get-CimInstance Win32_Product -Filter "Name='HelloMSI'" |
  Select Name, Version, IdentifyingNumber       # IdentifyingNumber = ProductCode
# (Win32_Product is slow but fine for a lab; note the ProductCode GUID it returns)
```

Uninstall by pointing /x at the MSI file
```
$ulog = "C:\labs\msi\uninstall.log"
$u = Start-Process msiexec.exe `
       -ArgumentList "/x `"$Msi`" /qn /norestart /l*v `"$ulog`"" `
       -Wait -PassThru
"uninstall exit code: $($u.ExitCode)"     # 0 or 3010 = success
```

Verify removal
```
Test-Path "C:\Program Files\HelloMSI\hello.txt"     # expect False
Get-CimInstance Win32_Product -Filter "Name='HelloMSI'"   # expect no rows
Select-String -Path $ulog -Pattern "Installation success or error status" | Select -Last 1
```

Teardown (remove lab files; product is already uninstalled)
```
# uninstall tool only; leaves nothing installed on the box
# dotnet tool uninstall --global wix
Remove-Item C:\labs\msi -Recurse -Force
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

# This will show your ansible admin user password
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
# Delete existing awx-admin-password secret
kubectl delete secret awx-admin-password -n awx

kubectl -n awx create secret generic awx-admin-password \
  --from-literal=password='awx@123'
```

Login credentials for Ansible Tower is
<pre>
username : admin
password : awx@123
</pre>

<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/7f40dabd-bff7-4674-9bff-5320627e6104" />
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/88a2cb4e-829f-41a0-8c78-842dc82c09b4" />
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/222f50c2-f0ed-4b63-ba21-2375375eb43f" />

#### Troubleshooting minikube start failures
```
minikube delete
sudo su student
docker images
minikube start --cpus=4 --memory=8g --driver=docker
```

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
tower-cli job launch -J "Invoke Install Apache Playbook"
```
