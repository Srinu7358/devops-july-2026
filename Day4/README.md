# Day 4

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

sudo apt install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

sudo usermod -aG docker $USER
newgrp $USER

docker --verison
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
