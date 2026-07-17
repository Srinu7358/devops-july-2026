# Day 5

## Info - Container Orchestration Platform
<pre>
- provides an environment with all the required features to make your application Highly Available
- it provides in-built monitoring features, self-healing features
- it supports scale up/down your application on-demand either manually or automatically based performance metrics
- it supports rolling update
  - upgrading/downgrading your application from one version to other without any downtime
- it supports features to expose your applicaiton only for applications that runs on the container orchestration platforms
  or to external access
- it provides in-built load balancing
- the application that you wish to run inside container orchestration platform, should be containerized in a
  custom container image
- this platform only manages containerized application workloads
- examples
  - Docker SWARM
  - Kubernetes
  - Red Hat Openshift
</pre>

## Info - Red Hat Openshift
<pre>
- is a Contantainer Orchestration Platform developed by Red Hat
- it is a Red Hat's distribution of Kubernetes
- it is developed on top of open source Google Kubernetes
- Red Hat Openshift supports all the features of Google Kubernetes + many additional useful features
- New features added in Openshift
  - Webconsole (GUI)
  - User Management
  - Route to expose application for external access with a user-friendly url
  - S2I ( Source to Image )
    - Deploying application from source code
    - BuildConfig
      - declarative definition to build and deploy your application container image and/or application 
    - ImageStream ( Custom Image with your application and its dependencies )
</pre>

## Demo - Red Hat Openshift
Starting Code Ready Container ( Single Node Openshift Cluster )
```
crc start
```
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/ae271c73-6563-45f7-aeab-52330395efb4" />
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/76e301ae-a8a5-439b-98fc-f929b3cce2ce" />

Listing the nodes in the Openshift cluster
```
oc get nodes
```

Creating a new project to deploy our application into it
```
oc new-project jegan
```
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/12d319dd-fe41-49e3-b06a-6cb6d6ce4fc9" />

Deploy our first application into openshift cluster
```
oc create deploy nginx --image=docker.io/bitnami/nginx:latest --replicas=3
```

Listing all deployments
```
oc get deployments
oc get deployment
oc get deploy
```

Listing all replicasets
```
oc get replicasets
oc get replicaset
oc get rs
```

Listing all pods
```
oc get pods
oc get pod
oc get po
```

Finding the IP addresses of Pods
```
oc get pods -o wide
```
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/59629a73-79ee-4a68-817b-4ece438f9363" />

Create an internal service for nginx deployment
```
oc expose deploy/nginx --type=ClusterIP --port=8080
```

List services
```
oc get services
oc get service
oc get svc
```

Descibe service to find more details about the service
```
oc describe svc/nginx
```

<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/58e60c8e-f57e-46b6-950c-7ec19698bb72" />

Let's create an external to make the nginx accessible for outside world
```
oc expose svc/nginx
```

List the routes
```
oc get routes
oc get route
```

Accessing the route
```
curl http://nginx-jegan.apps-crc.testing
```
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/d82c3cda-2fd5-4bac-9de4-3e9263c7ff97" />

Rolling update to upgrade your application from 1.0 to 2.0
```
oc get pods -o json | grep image

oc set image deploy/hello nginx=docker.io/tektutor/nginx:2.0

oc rollout status deploy/hello
oc rollout history deploy/hello

oc get pods
oc get pods -o json | grep image
```

<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/f6ece236-c31c-4fff-8179-db9c5e70f075" />
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/e9f40916-3e81-4743-ba86-38a622374d96" />
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/dee9ab9e-2bcd-4bbd-a423-cc4fbbc3f487" />

## Lab - Source to Image (S2I) Docker Strategy - deploy application into openshift using GitHub source code
```
oc delete project jegan
oc new-project jegan
oc new-app --name=hello https://github.com/tektutor/spring-ms.git --strategy=docker
oc expose svc/hello

oc logs -f buildconfig/hello

oc get imagestreams
oc get buildconfigs
oc describe buildconfig/hello
```

<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/4fcded39-e42d-4619-8795-bb684c043a6f" />
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/31fc7259-2cf7-41a1-993b-0f8ad6717099" />
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/483f817c-2da5-4515-8da1-45533e1e6498" />

<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/b085c39e-4c53-4f95-9002-6a6af386414f" />

## Info - Single Sign-On with OpenLDAP and Keycloak Overview
<pre>
- OpenLDAP is where identities live
- Keycloak is what applications talk to for login
- Keycloak federates (reads) users from OpenLDAP and then speaks modern SSO protocols 
  to your apps, so your apps never touch L2.0DAP directly and never see passwords
- What SSO actually means here?
  - Single Sign-On means a user authenticates once and then reaches many applications 
    without logging in again
  - The apps don't each check the password, they delegate authentication to a central 
    identity provider (IdP) and accept a token as proof
  - The value is one credential, one login, central control of who can access what, 
    and apps that hold no passwords
- OpenLDAP: the directory (the identity store)
  - LDAP (Lightweight Directory Access Protocol) is a protocol and data model for a hierarchical 
    directory of entries, users, groups, org units
  - OpenLDAP is the open-source server that implements it
  - Think of it as the read-optimized database of "who exists."
- Keycloak: the identity provider (the SSO brain)
  - Keycloak is an open-source IdP and access-management server
  - It's the piece that turns "a directory of users" into "single sign-on for web and API apps
  - It speaks the protocols apps actually integrate with, and it federates the directory behind the scenes
- The protocols: how apps get told "this user is authenticated"
  - This is the layer OpenLDAP can't provide and Keycloak exists to add
  - OpenID Connect (OIDC) the modern default. 
    - Built on OAuth 2.0, it issues signed JWT tokens (an ID token proving identity, an access token for authorization). 
    - Web apps, SPAs, mobile, and APIs almost always use OIDC today.
    - The app redirects the user to Keycloak, the user logs in, Keycloak redirects back with a token, 
      the app validates the token's signature against Keycloak's public key and trusts it.
  - SAML 2.0 the older XML-based standard, still common in enterprise and for legacy apps
    - Same delegation idea, different envelope (XML assertions instead of JWTs).
</pre>

# Lab - Setup OpenLDAP and load users

#### Pre-flight (Docker present, port and name free)

```bash
docker --version
docker rm -f openldap 2>/dev/null
sudo ss -ltnp | grep -E ':(389|636)\b' && echo "PORT IN USE" || echo "ports free"
```

#### Run OpenLDAP and create the organization

```bash
# LDAP_ORGANISATION + LDAP_DOMAIN seed the base org and base DN.
# base DN derived from the domain: tektutor.org -> dc=tektutor,dc=org
docker run -d --name openldap \
  -p 389:389 -p 636:636 \
  -e LDAP_ORGANISATION="TekTutor" \
  -e LDAP_DOMAIN="tektutor.org" \
  -e LDAP_ADMIN_PASSWORD="Admin@123" \
  osixia/openldap:1.5.0
sleep 5
docker ps --filter name=openldap
```

#### Confirm the base tree exists

```bash
# admin bind DN is cn=admin,<baseDN>
docker exec openldap ldapsearch -x -H ldap://localhost \
  -D "cn=admin,dc=tektutor,dc=org" -w "Admin@123" \
  -b "dc=tektutor,dc=org" -s base
# should return the dc=tektutor,dc=org entry (objectClass organization/dcObject)
```

#### Write the LDIF: organizational units, groups, and users

```bash
cat > /tmp/tektutor.ldif <<'EOF'
# --- organizational units (containers) ---
dn: ou=people,dc=tektutor,dc=org
objectClass: organizationalUnit
ou: people

dn: ou=groups,dc=tektutor,dc=org
objectClass: organizationalUnit
ou: groups

# --- users (inetOrgPerson) ---
dn: uid=jegan,ou=people,dc=tektutor,dc=org
objectClass: inetOrgPerson
uid: jegan
cn: Jegan Swaminathan
sn: Swaminathan
mail: jegan@tektutor.org
userPassword: Passw0rd!

dn: uid=sriram,ou=people,dc=tektutor,dc=org
objectClass: inetOrgPerson
uid: sriram
cn: Sriram Jeganathan
sn: Jeganathan
mail: sriram@tektutor.org
userPassword: Passw0rd!

dn: uid=asha,ou=people,dc=tektutor,dc=org
objectClass: inetOrgPerson
uid: asha
cn: Asha Rao
sn: Rao
mail: asha@tektutor.org
userPassword: Passw0rd!

# --- groups (groupOfNames; members are full DNs) ---
dn: cn=developers,ou=groups,dc=tektutor,dc=org
objectClass: groupOfNames
cn: developers
member: uid=jegan,ou=people,dc=tektutor,dc=org
member: uid=sriram,ou=people,dc=tektutor,dc=org

dn: cn=admins,ou=groups,dc=tektutor,dc=org
objectClass: groupOfNames
cn: admins
member: uid=jegan,ou=people,dc=tektutor,dc=org
EOF
cat /tmp/tektutor.ldif
```

#### Load the LDIF

```bash
# copy into the container and add it as the admin user
docker cp /tmp/tektutor.ldif openldap:/tmp/tektutor.ldif
docker exec openldap ldapadd -x -H ldap://localhost \
  -D "cn=admin,dc=tektutor,dc=org" -w "Admin@123" \
  -f /tmp/tektutor.ldif
# expect: adding new entry ... for each dn
```

#### Verify: all users resolve

```bash
docker exec openldap ldapsearch -x -H ldap://localhost \
  -D "cn=admin,dc=tektutor,dc=org" -w "Admin@123" \
  -b "ou=people,dc=tektutor,dc=org" "(objectClass=inetOrgPerson)" uid cn mail
# expect three entries: jegan, sriram, asha
```

#### Verify: a specific user by uid

```bash
docker exec openldap ldapsearch -x -H ldap://localhost \
  -D "cn=admin,dc=tektutor,dc=org" -w "Admin@123" \
  -b "ou=people,dc=tektutor,dc=org" "(uid=jegan)"
```

#### Verify: groups and their members resolve

```bash
docker exec openldap ldapsearch -x -H ldap://localhost \
  -D "cn=admin,dc=tektutor,dc=org" -w "Admin@123" \
  -b "ou=groups,dc=tektutor,dc=org" "(objectClass=groupOfNames)" cn member
# developers -> jegan, sriram ; admins -> jegan
```

#### Verify: which groups a user belongs to (reverse lookup)

```bash
# find every group whose member is jegan's DN
docker exec openldap ldapsearch -x -H ldap://localhost \
  -D "cn=admin,dc=tektutor,dc=org" -w "Admin@123" \
  -b "ou=groups,dc=tektutor,dc=org" \
  "(member=uid=jegan,ou=people,dc=tektutor,dc=org)" cn
# expect: developers and admins
```

#### Verify: a user can bind with their own password (real auth check)

```bash
# bind AS the user, not as admin. Success proves the password works.
docker exec openldap ldapsearch -x -H ldap://localhost \
  -D "uid=sriram,ou=people,dc=tektutor,dc=org" -w "Passw0rd!" \
  -b "dc=tektutor,dc=org" "(uid=sriram)" uid cn
# a returned entry = bind succeeded = credentials valid
```



# Lab - Configure Keycloak for SSO and policy

#### Pre-flight (OpenLDAP from the previous lab must be running)

```bash
docker ps --filter name=openldap        # must be Up; if not, redo the LDAP lab
docker --version
docker network ls | grep ssolab || docker network create ssolab
# put OpenLDAP on the shared network so Keycloak can reach it by name
docker network connect ssolab openldap 2>/dev/null || true
sudo ss -ltnp | grep -E ':(8080|4180|4181|9001|9002)\b' && echo "PORT IN USE" || echo "ports free"
```

#### Run Keycloak (dev mode is fine for a lab)

```bash
# Keycloak 26 uses KC_BOOTSTRAP_ADMIN_* for the first admin.
# The old KEYCLOAK_ADMIN_* names are deprecated and may be ignored.
docker rm -f keycloak 2>/dev/null
docker run -d --name keycloak --network ssolab \
  -p 8080:8080 \
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin \
  -e KC_BOOTSTRAP_ADMIN_PASSWORD='Admin@123' \
  quay.io/keycloak/keycloak:26.0 start-dev
sleep 20
# confirm the bootstrap admin was created before using kcadm
docker logs keycloak 2>&1 | grep -i "admin user" | head -1
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/   # expect 200/302
```

#### Log in with kcadm (the admin CLI inside the container)

```bash
docker exec keycloak /opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:8080 --realm master \
  --user admin --password 'Admin@123'
```

#### Create the realm

```bash
docker exec keycloak /opt/keycloak/bin/kcadm.sh create realms \
  -s realm=tektutor -s enabled=true
```

#### Federate the realm to OpenLDAP as the user store

```bash
# connection URL uses the container name on the shared network,
# NOT a hardcoded bridge IP. Both containers are on ssolab.
docker exec keycloak /opt/keycloak/bin/kcadm.sh create \
  components -r tektutor \
  -s name=openldap \
  -s providerId=ldap \
  -s providerType=org.keycloak.storage.UserStorageProvider \
  -s 'config.priority=["0"]' \
  -s 'config.editMode=["READ_ONLY"]' \
  -s 'config.vendor=["other"]' \
  -s 'config.connectionUrl=["ldap://openldap:389"]' \
  -s 'config.usersDn=["ou=people,dc=tektutor,dc=org"]' \
  -s 'config.bindDn=["cn=admin,dc=tektutor,dc=org"]' \
  -s 'config.bindCredential=["Admin@123"]' \
  -s 'config.usernameLDAPAttribute=["uid"]' \
  -s 'config.rdnLDAPAttribute=["uid"]' \
  -s 'config.uuidLDAPAttribute=["entryUUID"]' \
  -s 'config.userObjectClasses=["inetOrgPerson"]' \
  -s 'config.searchScope=["2"]'
# note the returned component id if you want to reference it later
```

#### Add a group mapper so LDAP groups become Keycloak groups

```bash
# find the ldap component id
LDAP_ID=$(docker exec keycloak /opt/keycloak/bin/kcadm.sh get components -r tektutor \
  --query name=openldap --fields id --format csv --noquotes | tail -1)
echo "ldap component: $LDAP_ID"

docker exec keycloak /opt/keycloak/bin/kcadm.sh create components -r tektutor \
  -s name=group-mapper \
  -s providerId=group-ldap-mapper \
  -s providerType=org.keycloak.storage.ldap.mappers.LDAPStorageMapper \
  -s "parentId=$LDAP_ID" \
  -s 'config.mode=["READ_ONLY"]' \
  -s 'config."groups.dn"=["ou=groups,dc=tektutor,dc=org"]' \
  -s 'config."group.name.ldap.attribute"=["cn"]' \
  -s 'config."group.object.classes"=["groupOfNames"]' \
  -s 'config."membership.ldap.attribute"=["member"]' \
  -s 'config."membership.attribute.type"=["DN"]' \
  -s 'config."user.roles.retrieve.strategy"=["LOAD_GROUPS_BY_MEMBER_ATTRIBUTE"]'
```

#### Sync users and groups from OpenLDAP

```bash
docker exec keycloak /opt/keycloak/bin/kcadm.sh create \
  "user-storage/$LDAP_ID/sync?action=triggerFullSync" -r tektutor

# verify users federated in
docker exec keycloak /opt/keycloak/bin/kcadm.sh get users -r tektutor \
  --fields username,email
# expect jegan, sriram, asha

# verify groups federated in
docker exec keycloak /opt/keycloak/bin/kcadm.sh get groups -r tektutor \
  --fields name
# expect developers, admins
```

#### Register application 1 as an OIDC client (app1, proxy on 4180)

```bash
docker exec keycloak /opt/keycloak/bin/kcadm.sh create clients -r tektutor \
  -s clientId=app1 \
  -s enabled=true \
  -s protocol=openid-connect \
  -s publicClient=false \
  -s standardFlowEnabled=true \
  -s 'redirectUris=["http://localhost:4180/oauth2/callback"]' \
  -s 'webOrigins=["http://localhost:4180"]'

# capture app1 internal id, then its secret straight into an env var
A1=$(docker exec keycloak /opt/keycloak/bin/kcadm.sh get clients -r tektutor \
  --query clientId=app1 --fields id --format csv --noquotes | tail -1)
APP1_CLIENT_SECRET=$(docker exec keycloak /opt/keycloak/bin/kcadm.sh get \
  "clients/$A1/client-secret" -r tektutor --fields value --format csv --noquotes | tail -1)
echo "APP1_CLIENT_SECRET=$APP1_CLIENT_SECRET"   # sanity check, must not be empty
```

#### Register application 2 as an OIDC client (app2, proxy on 4181)

```
docker exec keycloak /opt/keycloak/bin/kcadm.sh create clients -r tektutor \
  -s clientId=app2 \
  -s enabled=true \
  -s protocol=openid-connect \
  -s publicClient=false \
  -s standardFlowEnabled=true \
  -s 'redirectUris=["http://localhost:4181/oauth2/callback"]' \
  -s 'webOrigins=["http://localhost:4181"]'

A2=$(docker exec keycloak /opt/keycloak/bin/kcadm.sh get clients -r tektutor \
  --query clientId=app2 --fields id --format csv --noquotes | tail -1)
APP2_CLIENT_SECRET=$(docker exec keycloak /opt/keycloak/bin/kcadm.sh get \
  "clients/$A2/client-secret" -r tektutor --fields value --format csv --noquotes | tail -1)
echo "APP2_CLIENT_SECRET=$APP2_CLIENT_SECRET"   # sanity check, must not be empty

# If your kcadm version ignores --fields on this endpoint and prints full JSON,
# use this fallback instead (grabs the "value" field):
#   APP2_CLIENT_SECRET=$(docker exec keycloak /opt/keycloak/bin/kcadm.sh get \
#     "clients/$A2/client-secret" -r tektutor | grep -oP '"value"\s*:\s*"\K[^"]+')
#
# IMPORTANT: keep $A1, $A2, APP1_CLIENT_SECRET, APP2_CLIENT_SECRET in the SAME
# shell you use to run the proxies below. New terminal = re-run these captures.
```

#### Add a groups claim to the token so the proxy can enforce policy

```
# add a group-membership mapper on each client so the ID token carries "groups"
# full.path=false makes the claim value "developers" (not "/developers"),
# which is what --allowed-group=developers expects. Keep these paired.
#
# NOTE: protocol-mapper config values are PLAIN STRINGS (claim.name=groups),
# NOT JSON arrays. The ["value"] array form is only for COMPONENT config
# (the LDAP provider and its mappers). Using ["value"] here fails with
# "Cannot parse the JSON [unknown_error]". The quoting on the LEFT of =
# stays, to protect the dots in the key names.
for CID in $A1 $A2; do
docker exec keycloak /opt/keycloak/bin/kcadm.sh create \
  "clients/$CID/protocol-mappers/models" -r tektutor \
  -s name=groups \
  -s protocol=openid-connect \
  -s protocolMapper=oidc-group-membership-mapper \
  -s 'config."claim.name"=groups' \
  -s 'config."full.path"=false' \
  -s 'config."id.token.claim"=true' \
  -s 'config."access.token.claim"=true' \
  -s 'config."userinfo.token.claim"=true'
done

# confirm the mapper landed on each client
docker exec keycloak /opt/keycloak/bin/kcadm.sh get \
  "clients/$A1/protocol-mappers/models" -r tektutor --fields name,protocolMapper
```

#### Define the policy: allow only the "developers" group

```
# There are two valid ways to enforce "only developers":
#   (a) at the proxy  -> oauth2-proxy checks the groups claim (used below)
#   (b) in Keycloak   -> client authorization policy on a group
# We use (a) because it protects the app edge and needs no per-app resource
# modeling. The token already carries "groups" from the mapper above.
echo "policy = oauth2-proxy allowed_groups=developers (enforced at the proxy)"
```

#### Run two tiny backend apps to protect (plain hello servers)

```
# http-echo listens on :5678 by default. We force it to :80 so the
# published port mapping and the proxy upstream port agree.
docker run -d --name app1 --network ssolab -p 9001:80 \
  hashicorp/http-echo -listen=:80 -text="APP1 backend: you are in"
docker run -d --name app2 --network ssolab -p 9002:80 \
  hashicorp/http-echo -listen=:80 -text="APP2 backend: you are in"
```

#### Protect app1 with oauth2-proxy, restricted to the developers group

```
docker rm -f proxy-app1 2>/dev/null
COOKIE1=$(openssl rand -base64 24)
docker run -d --name proxy-app1 --network host \
  quay.io/oauth2-proxy/oauth2-proxy:latest \
  --provider=oidc \
  --oidc-issuer-url=http://localhost:8080/realms/tektutor \
  --client-id=app1 \
  --client-secret="$APP1_CLIENT_SECRET" \
  --redirect-url=http://localhost:4180/oauth2/callback \
  --upstream=http://localhost:9001 \
  --http-address=0.0.0.0:4180 \
  --cookie-secret="$COOKIE1" \
  --cookie-secure=false \
  --email-domain='*' \
  --scope="openid profile email" \
  --allowed-group=developers \
  --oidc-groups-claim=groups \
  --insecure-oidc-allow-unverified-email=true
```

#### Protect app2 the same way on 4181

```
# 1. confirm app2's secret var is populated (re-capture if empty or new shell)
echo "APP2_CLIENT_SECRET=$APP2_CLIENT_SECRET"

# if empty:
A2=$(docker exec keycloak /opt/keycloak/bin/kcadm.sh get clients -r tektutor \
  --query clientId=app2 --fields id --format csv --noquotes | tail -1)
APP2_CLIENT_SECRET=$(docker exec keycloak /opt/keycloak/bin/kcadm.sh get \
  "clients/$A2/client-secret" -r tektutor --fields value --format csv --noquotes | tail -1)
echo "APP2_CLIENT_SECRET=$APP2_CLIENT_SECRET"

docker rm -f proxy-app2 2>/dev/null
COOKIE2=$(openssl rand -base64 24)
echo "cookie len = ${#COOKIE2}"   # must be 32
docker run -d --name proxy-app2 --network host \
  quay.io/oauth2-proxy/oauth2-proxy:latest \
  --provider=oidc \
  --oidc-issuer-url=http://localhost:8080/realms/tektutor \
  --client-id=app2 \
  --client-secret="$APP2_CLIENT_SECRET" \
  --redirect-url=http://localhost:4181/oauth2/callback \
  --upstream=http://localhost:9002 \
  --http-address=0.0.0.0:4181 \
  --cookie-secret="$COOKIE2" \
  --cookie-secure=false \
  --email-domain='*' \
  --scope="openid profile email" \
  --allowed-group=developers \
  --oidc-groups-claim=groups \
  --insecure-oidc-allow-unverified-email=true
```

#### Test: a developer gets in, SSO carries to the second app

```
# In a browser (these flows need cookies/redirects, curl won't complete login):
#  1. open http://localhost:4180  -> redirected to Keycloak login
#  2. log in as  sriram / Passw0rd!   (member of developers)
#  3. you land on "APP1 backend: you are in"
#  4. open http://localhost:4181  -> NO second login (SSO session), APP2 shows
```

#### Test: a non-developer is denied by the policy

```
# asha is NOT in the developers group.
#  1. open a fresh/incognito browser -> http://localhost:4180
#  2. log in as  asha / Passw0rd!
#  3. Keycloak authenticates her, but oauth2-proxy sees no "developers" in the
#     groups claim and returns 403 -> access denied at the proxy
```

#### Verify the token actually carries the group (troubleshooting aid)

```
# with host networking the proxy reaches Keycloak on localhost too
curl -s http://localhost:8080/realms/tektutor/.well-known/openid-configuration \
  | head -c 300
# proxy logs show the allow/deny decision
docker logs --tail 20 proxy-app1
```

# Lab - Prove single sign-on and policy

#### Pre-flight (everything from the Keycloak lab must be up)

```
# Multiple --filter name= flags are OR'd and would hide some containers,
# so just list everything and eyeball it. Expect SIX containers Up:
# openldap, keycloak, app1, app2, proxy-app1, proxy-app2.
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

#### Confirm Keycloak discovery is reachable from a proxy (the usual failure point)

```
# The proxies run with --network host, so there is NO "keycloak" DNS name
# inside them. Use localhost:8080, which is what host networking exposes and
# what the browser also hits. The issuer printed here MUST equal the browser's.
docker exec proxy-app1 wget -qO- \
  http://localhost:8080/realms/tektutor/.well-known/openid-configuration \
  | tr ',' '\n' | grep -E '"issuer"|authorization_endpoint'
# issuer should read: http://localhost:8080/realms/tektutor
```

#### Use a browser on your workstation, not the headless server

```
# The OIDC flow needs redirects + cookies. If tektutor is headless, from your
# laptop open an SSH tunnel. ALL THREE ports are mandatory, not optional:
#   4180/4181 reach the proxies, 8080 reaches Keycloak for the login redirect.
# Drop 8080 and the redirect points at a Keycloak your laptop cannot reach.
#   ssh -L 4180:localhost:4180 -L 4181:localhost:4181 -L 8080:localhost:8080 \
#       jegan@<server>
# then browse http://localhost:4180 on your laptop.
```

#### Step 1 - Open the first app, get redirected, sign in, reach the app

```
# 1. Open a FRESH browser (or incognito) -> http://localhost:4180
# 2. oauth2-proxy sees no session -> redirects you to Keycloak login
# 3. Sign in as a developers-group user:   sriram / Passw0rd!
# 4. Keycloak authenticates against OpenLDAP (LDAP bind), issues a token
# 5. You land on: "APP1 backend: you are in"
```

#### SiteMinder equivalent (Step 1)

```
# Keycloak realm login page       = SiteMinder credential collector / login FCC
# oauth2-proxy in front of app1    = SiteMinder Web Agent on the web server
# "no session -> redirect to IdP"  = Web Agent finds no SMSESSION cookie,
#                                    redirects to the Policy Server login
# Keycloak authenticating via LDAP = SiteMinder Policy Server authenticating
#                                    against its LDAP user directory
# token/oauth2-proxy cookie issued = SiteMinder issues the SMSESSION cookie
```

#### Step 2 - Open the second app, confirm NO second login

```
# In the SAME browser (same session):
#   Open http://localhost:4181
#   You reach "APP2 backend: you are in" WITHOUT logging in again.
# That is single sign-on: Keycloak already has an SSO session for this browser,
# so it issues app2 a token silently.
```

#### SiteMinder equivalent (Step 2)

```
# Keycloak SSO session cookie = the SMSESSION cookie shared across agents in
#   the same cookie domain / policy domain
# app2's Web Agent sees a valid SMSESSION -> no re-auth, just an authz check
# This is exactly SiteMinder "single sign-on across agents in one domain"
```

#### Confirm the SSO session exists (evidence for Step 2)

```
# In browser dev tools -> Application -> Cookies for localhost:8080, you'll see
# Keycloak's session cookies (names vary by version, e.g. KEYCLOAK_SESSION,
# KEYCLOAK_IDENTITY, AUTH_SESSION_ID; some are httpOnly). Those are what let
# app2 skip the login. (SiteMinder analog: SMSESSION.)
```

#### Step 3 - Sign in as a user OUTSIDE the allowed group, confirm denial

```
# 1. Open a NEW incognito window (clean session) -> http://localhost:4180
# 2. Sign in as asha / Passw0rd!   (asha is NOT in the developers group)
# 3. Keycloak AUTHENTICATES her successfully (valid LDAP user)...
# 4. ...but oauth2-proxy checks the "groups" claim, sees no "developers",
#    and returns 403 Forbidden. Access denied at the proxy.
```

#### SiteMinder equivalent (Step 3)

```
# Keycloak login success + proxy 403 = SiteMinder AUTHENTICATION succeeds
#   (valid directory user) but AUTHORIZATION fails
# oauth2-proxy --allowed-group=developers = a SiteMinder authorization POLICY
#   that binds a realm/resource to a specific user directory GROUP
# "authenticated but not authorized" is the classic SiteMinder distinction:
#   the AuthN step passes, the AuthZ rule on the protected resource rejects
```

#### Prove the difference is authorization, not authentication (evidence for Step 3)

```
# Watch the proxy decision as asha is denied:
docker logs --tail 15 proxy-app1
# you should see the login succeed upstream but the group check fail ->
# a 403, confirming denial happened at the policy layer, not at login.
```

#### Reset between test users (so sessions don't bleed across cases)

```
# Prefer a NEW incognito window per identity. It is the most reliable reset
# and avoids a confusing case: after asha is denied at app1, her Keycloak SSO
# session still exists, so hitting app2 in the SAME window also 403s at the
# proxy (correct, but looks odd). A clean window per user removes the doubt.
#
# Fallback (Keycloak-side logout of the current SSO session). Modern Keycloak
# may show a confirmation page rather than logging out silently:
#   http://localhost:8080/realms/tektutor/protocol/openid-connect/logout
```

#### The three assertions this lab proves, stated plainly

```
# 1. Federation works:  an OpenLDAP user logs into an app via Keycloak.
# 2. SSO works:         one login reaches app1 AND app2, no second prompt.
# 3. Policy works:      a valid user outside the group is denied (AuthZ != AuthN).
```

#### Full SiteMinder mapping table, for your notes

```
# Keycloak / this lab            ->  SiteMinder
# ---------------------------------------------------------------
# Keycloak (IdP + token issuer)  ->  Policy Server
# Keycloak realm                 ->  Policy Domain
# oauth2-proxy (per app)         ->  Web Agent (per web server/app)
# OpenLDAP                       ->  User Directory (LDAP)
# Keycloak SSO session cookie    ->  SMSESSION cookie
# OIDC token / groups claim      ->  SiteMinder session + directory attributes
# --allowed-group=developers     ->  Authorization policy bound to a group
# realm login page               ->  credential collector (FCC/login page)
# authentication (LDAP bind)     ->  AuthN scheme (LDAP)
# group/policy check -> 403      ->  AuthZ rule denial on a protected resource
```

## Info - MCP Overview
<pre>
- MCP is a standard way to plug external tools and data into an AI assistant, 
  so the model can do things and read things beyond its training, through a uniform 
  interface instead of a bespoke integration per tool
- The problem it solves
  - Before MCP, every "give the AI access to X" was a custom integration
  - one glue layer for your database, another for GitHub, another for your filesystem, 
    each with its own auth, schema, and calling convention. 
  - N tools times M AI applications means N×M integrations
  - MCP makes it N+M
    - a tool author writes one MCP server, and any MCP-capable client can use it
    - It's deliberately analogous to what a common driver interface or a protocol like 
      LSP (Language Server Protocol) did, standardize the connector so the two sides stop 
      needing custom wiring
- The two roles
  - MCP server 
    - exposes capabilities
    - it wraps some external system, a database, an API, a filesystem, a ticketing system, 
      and presents it through the MCP interface
    - a server is usually small and single-purpose ("the GitHub server", "the Postgres server")
  - MCP client lives inside the AI application (the assistant, the IDE plugin, the agent runtime)
  - it connects to one or more servers, discovers what they offer, and lets the model invoke them
  - the client is the model's hands and eyes, the server is the thing being touched

- the AI model itself never speaks MCP directly
- the client mediates
  - it presents the server's capabilities to the model, and when the model decides to use one, 
    the client makes the actual MCP call and feeds the result back
- The three things a server exposes
  - Tools
    - actions the model can invoke, functions with side effects or computation
    - Create a GitHub issue
    - run this SQL query
    - send an email
  - Resources
    - data the model can read, addressed by URI
    - A file's contents
    - a database record
    - a document
  - Prompts
    - reusable, parameterized prompt templates the server offers
    - summarize this PR
    - review this code for security issues
</pre>

## Info - MCP (AI Agent)
<pre>​ 
- Model Context Protocol connects AI assistants to external tools and data
- An MCP server exposes tools, resources, and prompts, an AI client connects over stdio or HTTP
</pre>

# Lab - Build and call an MCP server (read-only Jenkins tools)

You build an MCP server that exposes two read-only tools, `get_build_status(job)`
and `list_deployments(env)`, backed by GET-only Jenkins REST calls. You connect it
to a client, ask a question that forces a tool call, inspect the exact call and
response, then work through the guardrails that separate read tools from write
tools.

Everything runs on one host. Set `JENKINS_MOCK=1` to complete the whole lab with
no live Jenkins. Swap in real credentials at the end to hit an actual server.

Verified on Python 3.12 with mcp 1.28, httpx 0.28, anthropic 0.117.

#### Prerequisites

```
sudo apt update && sudo apt install -y python3.14-venv node
python3 --version          # need 3.10 or newer
node --version             # optional, only for the MCP Inspector step 
```

#### Create the project and virtual environment

```
mkdir -p ~/devops-july-2026/Day5/mcp-jenkins-lab
cd ~/devops-july-2026/Day5/mcp-jenkins-lab
python3 -m venv .venv
. .venv/bin/activate
python -m pip install --upgrade pip
```
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/c6d5c09a-f9f3-4b71-ad90-b38dfa8f9ff5" />


#### Pin and install dependencies

```
cat > requirements.txt <<'EOF'
mcp==1.28.1
httpx==0.28.1
anthropic==0.117.0
google-genai==1.15.0
EOF
pip install -r requirements.txt

# Confirm the core import works
python -c "from mcp.server.fastmcp import FastMCP; import httpx; print('mcp + httpx OK')"
```
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/083589d5-3b81-471c-a2b0-8f1b99a60711" />
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/ef2041f6-26a9-462a-8833-afb7485ef6d2" />

#### Write the read-only Jenkins access layer

Every call here is a GET. There is no path that triggers, cancels, or edits
anything. `list_deployments` models "deployments to an env" as the build history
of a conventionally named `deploy-<env>` job.

```
cat > jenkins_client.py <<'EOF'
"""Read-only Jenkins access layer for the MCP server."""
import os
import httpx

JENKINS_URL = os.environ.get("JENKINS_URL", "http://localhost:8080").rstrip("/")
JENKINS_USER = os.environ.get("JENKINS_USER", "")
JENKINS_TOKEN = os.environ.get("JENKINS_TOKEN", "")
JENKINS_MOCK = os.environ.get("JENKINS_MOCK", "0") == "1"
TIMEOUT = float(os.environ.get("JENKINS_TIMEOUT", "10"))

_MOCK_BUILDS = {
    "payments-api": {"number": 412, "result": "SUCCESS", "building": False,
                     "url": "http://localhost:8080/job/payments-api/412/"},
    "web-frontend": {"number": 98, "result": "FAILURE", "building": False,
                     "url": "http://localhost:8080/job/web-frontend/98/"},
}
_MOCK_DEPLOYS = {
    "staging": [
        {"number": 55, "result": "SUCCESS", "timestamp": 1737000000000},
        {"number": 54, "result": "SUCCESS", "timestamp": 1736900000000},
    ],
    "prod": [
        {"number": 21, "result": "SUCCESS", "timestamp": 1737100000000},
        {"number": 20, "result": "FAILURE", "timestamp": 1737050000000},
    ],
}

def _auth():
    if JENKINS_USER and JENKINS_TOKEN:
        return (JENKINS_USER, JENKINS_TOKEN)
    return None

def _get(path: str, params: dict) -> dict:
    url = f"{JENKINS_URL}{path}"
    with httpx.Client(timeout=TIMEOUT, auth=_auth()) as c:
        r = c.get(url, params=params)
        r.raise_for_status()
        return r.json()

def build_status(job: str) -> dict:
    """Last build result for a job. Read-only."""
    if JENKINS_MOCK:
        data = _MOCK_BUILDS.get(job)
        if data is None:
            return {"job": job, "found": False, "reason": "unknown job (mock)"}
        return {"job": job, "found": True, **data}
    data = _get(f"/job/{job}/lastBuild/api/json",
                {"tree": "number,result,building,url"})
    return {
        "job": job, "found": True,
        "number": data.get("number"),
        "result": data.get("result") or ("RUNNING" if data.get("building") else "UNKNOWN"),
        "building": data.get("building", False),
        "url": data.get("url"),
    }

def deployments(env: str, limit: int = 5) -> dict:
    """Recent builds of the deploy-<env> pipeline. Read-only."""
    if JENKINS_MOCK:
        rows = _MOCK_DEPLOYS.get(env, [])[:limit]
        return {"env": env, "job": f"deploy-{env}", "count": len(rows), "deployments": rows}
    data = _get(f"/job/deploy-{env}/api/json",
                {"tree": f"builds[number,result,timestamp,url]{{0,{limit}}}"})
    return {
        "env": env, "job": f"deploy-{env}",
        "count": len(data.get("builds", [])),
        "deployments": data.get("builds", []),
    }
EOF
```

#### Smoke-test the Jenkins layer on its own (before any MCP)

```
JENKINS_MOCK=1 python -c "import jenkins_client as jk; print(jk.build_status('payments-api')); print(jk.deployments('prod'))"
# Expect: payments-api SUCCESS #412, and two prod deploy rows
```
<img width="1920" height="1200" alt="image" src="https://github.com/user-attachments/assets/0195ed18-f0b3-418e-b217-a9f8c3c6c9e8" />

#### Write the MCP server

Two tools, both read-only. The disabled `trigger_deploy` block at the bottom is
the write-tool reference you discuss in the guardrails section.

```
cat > server.py <<'EOF'
"""MCP server exposing read-only Jenkins tools over stdio."""
import json
from mcp.server.fastmcp import FastMCP
import jenkins_client as jk

mcp = FastMCP("jenkins-readonly")

@mcp.tool()
def get_build_status(job: str) -> str:
    """Get the status of the most recent build for a Jenkins job.

    Args:
        job: The Jenkins job name, e.g. "payments-api".
    Returns:
        JSON with the build number, result (SUCCESS/FAILURE/RUNNING), and URL.
    """
    return json.dumps(jk.build_status(job))

@mcp.tool()
def list_deployments(env: str) -> str:
    """List recent deployments to an environment.

    Args:
        env: Environment name, e.g. "staging" or "prod".
    Returns:
        JSON with the deploy job name and a list of recent deploy builds.
    """
    return json.dumps(jk.deployments(env))

# ---------------------------------------------------------------------------
# WRITE TOOL - intentionally DISABLED. Uncommenting exposes an action that
# changes Jenkins state. Never ship it without the approval gate shown.
#
# @mcp.tool()
# def trigger_deploy(env: str, confirm_token: str = "") -> str:
#     """Trigger a deploy. Requires an approval token from a human."""
#     expected = os.environ.get("DEPLOY_APPROVAL_TOKEN", "")
#     if not expected or confirsudo apt update && sudo apt install -y python3.14-venvm_token != expected:
#         return json.dumps({"status": "blocked", "reason": "approval required"})
#     # ... only here would you POST to /job/deploy-<env>/build ...
#     return json.dumps({"status": "queued", "env": env})
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    mcp.run()  # stdio transport by default
EOF
```

#### Write a pure MCP client probe (no LLM, no API key)

This proves the wiring and shows the raw tool call and response.

Note: the MCP stdio client passes only a safe subset of environment variables to
the server child. You must forward the `JENKINS_*` vars explicitly, or the server
never sees `JENKINS_MOCK` and tries to reach a real Jenkins.

```
cat > client_probe.py <<'EOF'
"""Connect to server.py over stdio, list tools, call each once."""
import asyncio, os
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client

_fwd = {k: v for k, v in os.environ.items() if k.startswith("JENKINS_")}
params = StdioServerParameters(command="python", args=["server.py"], env=_fwd or None)

async def main():
    async with stdio_client(params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            tools = await session.list_tools()
            print("== TOOLS ADVERTISED ==")
            for t in tools.tools:
                print(f"  {t.name}: {t.description.splitlines()[0]}")
            print("\n== CALL get_build_status(job='payments-api') ==")
            r1 = await session.call_tool("get_build_status", {"job": "payments-api"})
            print(r1.content[0].text)
            print("\n== CALL list_deployments(env='prod') ==")
            r2 = await session.call_tool("list_deployments", {"env": "prod"})
            print(r2.content[0].text)

if __name__ == "__main__":
    asyncio.run(main())
EOF
```

#### Run the probe (expect real tool output)

```
JENKINS_MOCK=1 python client_probe.py
```

Expected output:

```
== TOOLS ADVERTISED ==
  get_build_status: Get the status of the most recent build for a Jenkins job.
  list_deployments: List recent deployments to an environment.

== CALL get_build_status(job='payments-api') ==
{"job": "payments-api", "found": true, "number": 412, "result": "SUCCESS", ...}

== CALL list_deployments(env='prod') ==
{"env": "prod", "job": "deploy-prod", "count": 2, "deployments": [...]}
```

#### Inspect the server with MCP Inspector (optional, needs Node)

The Inspector is a browser UI that lists your tools and lets you call them by
hand. Pass the env through so mock mode reaches the server.

```
JENKINS_MOCK=1 npx @modelcontextprotocol/inspector python server.py
# Open the printed localhost URL, pick a tool, fill the arg, click Call.
```

#### Connect an LLM client that forces a tool call (Anthropic)

This is the "ask a question that forces a tool call" step. The client pulls the
tool schemas from MCP, hands them to Claude, and executes whatever tool Claude
chooses, printing the full trace.

```
cat > ask.py <<'EOF'
import asyncio, json, os, sys
from anthropic import Anthropic
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client

MODEL = os.environ.get("ANTHROPIC_MODEL", "claude-haiku-4-5-20251001")
_fwd = {k: v for k, v in os.environ.items() if k.startswith("JENKINS_")}
params = StdioServerParameters(command="python", args=["server.py"], env=_fwd or None)

async def run(question: str):
    client = Anthropic()  # reads ANTHROPIC_API_KEY
    async with stdio_client(params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            listed = await session.list_tools()
            tools = [{"name": t.name, "description": t.description,
                      "input_schema": t.inputSchema} for t in listed.tools]
            messages = [{"role": "user", "content": question}]
            while True:
                resp = client.messages.create(model=MODEL, max_tokens=1024,
                                               tools=tools, messages=messages)
                messages.append({"role": "assistant", "content": resp.content})
                tool_uses = [b for b in resp.content if b.type == "tool_use"]
                if not tool_uses:
                    text = "".join(b.text for b in resp.content if b.type == "text")
                    print("\n== FINAL ANSWER ==\n" + text)
                    return
                results = []
                for tu in tool_uses:
                    print(f"\n== TOOL CALL == {tu.name}({json.dumps(tu.input)})")
                    out = await session.call_tool(tu.name, tu.input)
                    raw = out.content[0].text
                    print(f"== TOOL RESULT ==\n{raw}")
                    results.append({"type": "tool_result", "tool_use_id": tu.id,
                                    "content": raw})
                messages.append({"role": "user", "content": results})

if __name__ == "__main__":
    q = " ".join(sys.argv[1:]) or "Did the last build of payments-api pass, and what happened in the two most recent prod deployments?"
    print(f"QUESTION: {q}")
    asyncio.run(run(q))
EOF
```

Run it. The question is phrased so a plain text answer is impossible without
calling both tools.

```
export ANTHROPIC_API_KEY=sk-ant-...              # your key
# export ANTHROPIC_MODEL=claude-haiku-4-5-20251001   # or any model on your account
JENKINS_MOCK=1 python ask.py
```

You should see two `TOOL CALL` lines, their raw JSON results, then a final answer
that stitches them together. Change the question to see the model pick different
tools:

```
JENKINS_MOCK=1 python ask.py "Is web-frontend green right now?"
```

#### Gemini free-tier alternative (no paid key)

For trainees without an Anthropic key. Get a free key at aistudio.google.com. The
google-genai SDK can take the MCP session directly as a tool.

```
cat > ask_gemini.py <<'EOF'
import asyncio, os, sys
from google import genai
from google.genai import types
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client

MODEL = os.environ.get("GEMINI_MODEL", "gemini-2.0-flash")
_fwd = {k: v for k, v in os.environ.items() if k.startswith("JENKINS_")}
params = StdioServerParameters(command="python", args=["server.py"], env=_fwd or None)

async def run(question: str):
    client = genai.Client(api_key=os.environ["GEMINI_API_KEY"])
    async with stdio_client(params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            resp = await client.aio.models.generate_content(
                model=MODEL, contents=question,
                config=types.GenerateContentConfig(tools=[session], temperature=0))
            print("\n== FINAL ANSWER ==\n" + resp.text)

if __name__ == "__main__":
    q = " ".join(sys.argv[1:]) or "Did the last build of payments-api pass, and what happened in the two most recent prod deployments?"
    print(f"QUESTION: {q}")
    asyncio.run(run(q))
EOF

export GEMINI_API_KEY=...                        # free key
JENKINS_MOCK=1 python ask_gemini.py
```

## Optional: register the server in Claude Desktop

Add this to the Claude Desktop config, then restart it. The tools show up under
the connectors icon. Use absolute paths.

```
# macOS: ~/Library/Application Support/Claude/claude_desktop_config.json
# Linux: ~/.config/Claude/claude_desktop_config.json
cat <<'EOF'
{
  "mcpServers": {
    "jenkins-readonly": {
      "command": "/ABSOLUTE/PATH/.venv/bin/python",
      "args": ["/ABSOLUTE/PATH/server.py"],
      "env": { "JENKINS_MOCK": "1" }
    }
  }
}
EOF
```

#### Point at a real Jenkins (when you are ready)

Create a read-only API token in Jenkins (user menu, Configure, API Token). Give
that user only Overall/Read and Job/Read in matrix security. Then:

```
export JENKINS_URL="https://jenkins.example.com"
export JENKINS_USER="ci-reader"
export JENKINS_TOKEN="11xxxxxxxxxxxxxxxxxxxxxxxxx"
unset JENKINS_MOCK
python client_probe.py     # now hits the real server, still GET-only
```

Notes:
1. `list_deployments` expects jobs named `deploy-staging`, `deploy-prod`, etc.
   Rename the convention in `jenkins_client.py` if yours differ.
2. If you see 403, the token user lacks Job/Read on that job. Fix permissions,
   do not add write scope.
3. Keep the token out of shell history: put the exports in a file you `source`
   with `chmod 600`, not inline.

#### Guardrails discussion (run this with the class)

Read-only vs write. Both tools here are GET-only and safe to auto-run. A write
tool (`trigger_deploy`, cancel, delete) changes state and must never auto-execute
from a model's decision alone. Show the disabled block in `server.py`: it returns
`blocked` unless a human supplies the current approval token.

```
# Demonstrate the gate logic without exposing the tool:
python - <<'EOF'
import os
def trigger_deploy(env, confirm_token=""):
    expected = os.environ.get("DEPLOY_APPROVAL_TOKEN", "")
    if not expected or confirm_token != expected:
        return {"status": "blocked", "reason": "approval required"}
    return {"status": "queued", "env": env}

print(trigger_deploy("prod"))                       # blocked, no token set
os.environ["DEPLOY_APPROVAL_TOKEN"] = "abc123"
print(trigger_deploy("prod", "wrong"))              # blocked, wrong token
print(trigger_deploy("prod", "abc123"))             # queued, human approved
EOF
```

Take away points
1. Least privilege at the source. The Jenkins token is read-only, so even a bug
   or prompt injection in the model cannot deploy. The safest write tool is the
   one that does not exist.
2. Authentication lives in the server, never in the model. The model sees tool
   names and schemas, not credentials. Rotate the token like any other secret.
3. Approval gates for any state change. A write tool returns "blocked" and hands
   back what approval is needed. A human (not the model) supplies the token out
   of band. The model can request a deploy but cannot grant it.
4. Transport and exposure. stdio keeps the server local to the client. If you move
   to HTTP (`mcp.run(transport="streamable-http")`), you now need network auth,
   TLS, and access control in front of it.
5. Audit. Log every tool call with its arguments and caller. Read calls are cheap
   to log and invaluable when something looks wrong.

#### Teardown

```
# Nothing was installed system-wide and no Jenkins state was changed.
deactivate 2>/dev/null || true
cd ~
rm -rf ~/devops-july-2026/Day5/mcp-jenkins-lab   # only if you want it gone

# If you added the Claude Desktop entry, remove the jenkins-readonly block
# from claude_desktop_config.json and restart Claude Desktop.
```
