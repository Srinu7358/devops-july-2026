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

## Info - Single Sign-On with OpenLDAP and Keycloak Overview
<pre>
- OpenLDAP is where identities live
- Keycloak is what applications talk to for login
- Keycloak federates (reads) users from OpenLDAP and then speaks modern SSO protocols 
  to your apps, so your apps never touch LDAP directly and never see passwords
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

## Lab - Setup OpenLDAP and load users

Pre-flight (Docker present, port and name free)
```
docker --version
docker rm -f openldap 2>/dev/null
sudo ss -ltnp | grep -E ':(389|636)\b' && echo "PORT IN USE" || echo "ports free"
```

Run OpenLDAP and create the organization
```
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

Confirm the base tree exists
```
# admin bind DN is cn=admin,<baseDN>
docker exec openldap ldapsearch -x -H ldap://localhost \
  -D "cn=admin,dc=tektutor,dc=org" -w "Admin@123" \
  -b "dc=tektutor,dc=org" -s base
# should return the dc=tektutor,dc=org entry (objectClass organization/dcObject)
```

Write the LDIF: organizational units, groups, and users
```
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

Load the LDIF
```
# copy into the container and add it as the admin user
docker cp /tmp/tektutor.ldif openldap:/tmp/tektutor.ldif
docker exec openldap ldapadd -x -H ldap://localhost \
  -D "cn=admin,dc=tektutor,dc=org" -w "Admin@123" \
  -f /tmp/tektutor.ldif
# expect: adding new entry ... for each dn
```

Verify: all users resolve
```
docker exec openldap ldapsearch -x -H ldap://localhost \
  -D "cn=admin,dc=tektutor,dc=org" -w "Admin@123" \
  -b "ou=people,dc=tektutor,dc=org" "(objectClass=inetOrgPerson)" uid cn mail
# expect three entries: jegan, sriram, asha
```

Verify: a specific user by uid
```
docker exec openldap ldapsearch -x -H ldap://localhost \
  -D "cn=admin,dc=tektutor,dc=org" -w "Admin@123" \
  -b "ou=people,dc=tektutor,dc=org" "(uid=jegan)"
```

Verify: groups and their members resolve
```
docker exec openldap ldapsearch -x -H ldap://localhost \
  -D "cn=admin,dc=tektutor,dc=org" -w "Admin@123" \
  -b "ou=groups,dc=tektutor,dc=org" "(objectClass=groupOfNames)" cn member
# developers -> jegan, sriram ; admins -> jegan
```

Verify: which groups a user belongs to (reverse lookup)
```
# find every group whose member is jegan's DN
docker exec openldap ldapsearch -x -H ldap://localhost \
  -D "cn=admin,dc=tektutor,dc=org" -w "Admin@123" \
  -b "ou=groups,dc=tektutor,dc=org" \
  "(member=uid=jegan,ou=people,dc=tektutor,dc=org)" cn
# expect: developers and admins
```

Verify: a user can bind with their own password (real auth check)
```
# bind AS the user, not as admin. Success proves the password works.
docker exec openldap ldapsearch -x -H ldap://localhost \
  -D "uid=sriram,ou=people,dc=tektutor,dc=org" -w "Passw0rd!" \
  -b "dc=tektutor,dc=org" "(uid=sriram)" uid cn
# a returned entry = bind succeeded = credentials valid
```

## Lab - Configure Keycloak for SSO and policy

Pre-flight (OpenLDAP from the previous lab must be running)
```
docker ps --filter name=openldap        # must be Up; if not, redo the LDAP lab
docker --version
docker network ls | grep ssolab || docker network create ssolab
# put OpenLDAP on the shared network so Keycloak can reach it by name
docker network connect ssolab openldap 2>/dev/null || true
sudo ss -ltnp | grep -E ':(8080|4180|4181|9001|9002)\b' && echo "PORT IN USE" || echo "ports free"
```

Run Keycloak (dev mode is fine for a lab)
```
docker rm -f keycloak 2>/dev/null
docker run -d --name keycloak --network ssolab \
  -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD='Admin@123' \
  quay.io/keycloak/keycloak:26.0 start-dev
sleep 20
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/   # expect 200/302
```

Log in with kcadm (the admin CLI inside the container)
```
docker exec keycloak /opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:8080 --realm master \
  --user admin --password 'Admin@123'
```

Create the realm
```
docker exec keycloak /opt/keycloak/bin/kcadm.sh create realms \
  -s realm=tektutor -s enabled=true
```

Federate the realm to OpenLDAP as the user store
```
# connection URL uses the container name on the shared network
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

Add a group mapper so LDAP groups become Keycloak groups
```
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

Sync users and groups from OpenLDAP
```
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

Register application 1 as an OIDC client (app1, will run behind proxy on 4180)
```
docker exec keycloak /opt/keycloak/bin/kcadm.sh create clients -r tektutor \
  -s clientId=app1 \
  -s enabled=true \
  -s protocol=openid-connect \
  -s publicClient=false \
  -s standardFlowEnabled=true \
  -s 'redirectUris=["http://localhost:4180/oauth2/callback"]' \
  -s 'webOrigins=["http://localhost:4180"]'
# capture app1 client secret
A1=$(docker exec keycloak /opt/keycloak/bin/kcadm.sh get clients -r tektutor \
  --query clientId=app1 --fields id --format csv --noquotes | tail -1)
docker exec keycloak /opt/keycloak/bin/kcadm.sh get "clients/$A1/client-secret" -r tektutor
# note the "value" -> this is APP1_CLIENT_SECRET
```

Register application 2 as an OIDC client (app2, proxy on 4181)
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
docker exec keycloak /opt/keycloak/bin/kcadm.sh get "clients/$A2/client-secret" -r tektutor
# note APP2_CLIENT_SECRET
```

Add a groups claim to the token so the proxy can enforce policy
```
# add a group-membership mapper on each client so the ID token carries "groups"
for CID in $A1 $A2; do
docker exec keycloak /opt/keycloak/bin/kcadm.sh create \
  "clients/$CID/protocol-mappers/models" -r tektutor \
  -s name=groups \
  -s protocol=openid-connect \
  -s protocolMapper=oidc-group-membership-mapper \
  -s 'config."claim.name"=["groups"]' \
  -s 'config."full.path"=["false"]' \
  -s 'config."id.token.claim"=["true"]' \
  -s 'config."access.token.claim"=["true"]' \
  -s 'config."userinfo.token.claim"=["true"]'
done
```

Define the policy: allow only the "developers" group
```
# There are two valid ways to enforce "only developers":
#   (a) at the proxy  -> oauth2-proxy checks the groups claim (used below)
#   (b) in Keycloak   -> client authorization policy on a group
# We use (a) because it protects the app edge and needs no per-app resource
# modeling. The token already carries "groups" from the mapper above.
echo "policy = oauth2-proxy allowed_groups=developers (enforced at the proxy)"
```

Run two tiny backend apps to protect (plain hello servers)
```
docker run -d --name app1 --network ssolab -p 9001:80 \
  hashicorp/http-echo -text="APP1 backend: you are in"
docker run -d --name app2 --network ssolab -p 9002:80 \
  hashicorp/http-echo -text="APP2 backend: you are in"
```

Protect app1 with oauth2-proxy, restricted to the developers group
```
docker run -d --name proxy-app1 --network ssolab -p 4180:4180 \
  quay.io/oauth2-proxy/oauth2-proxy:latest \
  --provider=oidc \
  --oidc-issuer-url=http://keycloak:8080/realms/tektutor \
  --client-id=app1 \
  --client-secret='APP1_CLIENT_SECRET' \
  --redirect-url=http://localhost:4180/oauth2/callback \
  --upstream=http://app1:80 \
  --http-address=0.0.0.0:4180 \
  --cookie-secret="$(openssl rand -base64 32 | tr -d '\n' | cut -c1-32)" \
  --cookie-secure=false \
  --email-domain='*' \
  --scope="openid profile email groups" \
  --allowed-group=developers \
  --oidc-groups-claim=groups
```

Protect app2 the same way on 4181
```
docker run -d --name proxy-app2 --network ssolab -p 4181:4181 \
  quay.io/oauth2-proxy/oauth2-proxy:latest \
  --provider=oidc \
  --oidc-issuer-url=http://keycloak:8080/realms/tektutor \
  --client-id=app2 \
  --client-secret='APP2_CLIENT_SECRET' \
  --redirect-url=http://localhost:4181/oauth2/callback \
  --upstream=http://app2:80 \
  --http-address=0.0.0.0:4181 \
  --cookie-secret="$(openssl rand -base64 32 | tr -d '\n' | cut -c1-32)" \
  --cookie-secure=false \
  --email-domain='*' \
  --scope="openid profile email groups" \
  --allowed-group=developers \
  --oidc-groups-claim=groups
```

Test: a developer gets in, SSO carries to the second app
```
# In a browser (these flows need cookies/redirects, curl won't complete login):
#  1. open http://localhost:4180  -> redirected to Keycloak login
#  2. log in as  sriram / Passw0rd!   (member of developers)
#  3. you land on "APP1 backend: you are in"
#  4. open http://localhost:4181  -> NO second login (SSO session), APP2 shows
```

Test: a non-developer is denied by the policy
```
# asha is NOT in the developers group.
#  1. open a fresh/incognito browser -> http://localhost:4180
#  2. log in as  asha / Passw0rd!
#  3. Keycloak authenticates her, but oauth2-proxy sees no "developers" in the
#     groups claim and returns 403 -> access denied at the proxy
```

Verify the token actually carries the group (troubleshooting aid)
```
# confirm the issuer/discovery is reachable from the proxy's network
docker exec proxy-app1 wget -qO- \
  http://keycloak:8080/realms/tektutor/.well-known/openid-configuration | head -c 300
# proxy logs show the allow/deny decision
docker logs --tail 20 proxy-app1
```

## Lab - Prove single sign-on and policy

Pre-flight (everything from the Keycloak lab must be up)
```
docker ps --filter name=openldap --filter name=keycloak \
  --filter name=proxy-app1 --filter name=proxy-app2 \
  --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
# all five (openldap, keycloak, proxy-app1, proxy-app2, + app1/app2) must be Up
```

Confirm Keycloak discovery is reachable from a proxy (the usual failure point)
```
docker exec proxy-app1 wget -qO- \
  http://keycloak:8080/realms/tektutor/.well-known/openid-configuration \
  | tr ',' '\n' | grep -E '"issuer"|authorization_endpoint'
# the issuer value here must match what the browser will hit; note it
```

Use a browser on your workstation, not the headless server
```
# The OIDC flow needs redirects + cookies. If tektutor is headless, from your
# laptop either use an SSH tunnel or point at the server's IP:
#   ssh -L 4180:localhost:4180 -L 4181:localhost:4181 -L 8080:localhost:8080 jegan@<server>
# then browse http://localhost:4180 on your laptop.
```

Step 1 - Open the first app, get redirected, sign in, reach the app
```
# 1. Open a FRESH browser (or incognito) -> http://localhost:4180
# 2. oauth2-proxy sees no session -> redirects you to Keycloak login
# 3. Sign in as a developers-group user:   sriram / Passw0rd!
# 4. Keycloak authenticates against OpenLDAP (LDAP bind), issues a token
# 5. You land on: "APP1 backend: you are in"
```

SiteMinder equivalent (Step 1)
```
# Keycloak realm login page      = SiteMinder credential collector / login FCC
# oauth2-proxy in front of app1   = SiteMinder Web Agent on the web server
# "no session -> redirect to IdP" = Web Agent finds no SMSESSION cookie,
#                                    redirects to the Policy Server login
# Keycloak authenticating via LDAP= SiteMinder Policy Server authenticating
#                                    against its LDAP user directory
# token/oauth2-proxy cookie issued= SiteMinder issues the SMSESSION cookie
```

Step 2 - Open the second app, confirm NO second login
```
# In the SAME browser (same session):
#   Open http://localhost:4181
#   You reach "APP2 backend: you are in" WITHOUT logging in again.
# That is single sign-on: Keycloak already has an SSO session for this browser,
# so it issues app2 a token silently.
```

SiteMinder equivalent (Step 2)
```
# Keycloak SSO session (KEYCLOAK_SESSION cookie) = the SMSESSION cookie shared
#   across agents in the same cookie domain / policy domain
# app2's Web Agent sees a valid SMSESSION -> no re-auth, just an authz check
# This is exactly SiteMinder "single sign-on across agents in one domain"
```

Confirm the SSO session exists (evidence for Step 2)
```
# In the browser dev tools -> Application -> Cookies for localhost:8080,
# you'll see Keycloak's session cookies (KEYCLOAK_SESSION / KEYCLOAK_IDENTITY).
# Those are what let app2 skip the login. (SiteMinder analog: SMSESSION.)
```

Step 3 - Sign in as a user OUTSIDE the allowed group, confirm denial
```
# 1. Open a NEW incognito window (clean session) -> http://localhost:4180
# 2. Sign in as asha / Passw0rd!   (asha is NOT in the developers group)
# 3. Keycloak AUTHENTICATES her successfully (valid LDAP user)...
# 4. ...but oauth2-proxy checks the "groups" claim, sees no "developers",
#    and returns 403 Forbidden. Access denied at the proxy.
```

SiteMinder equivalent (Step 3)
```
# Keycloak login success + proxy 403 = SiteMinder AUTHENTICATION succeeds
#   (valid directory user) but AUTHORIZATION fails
# oauth2-proxy --allowed-group=developers = a SiteMinder authorization POLICY
#   that binds a realm/resource to a specific user directory GROUP
# "authenticated but not authorized" is the classic SiteMinder distinction:
#   the AuthN step passes, the AuthZ rule on the protected resource rejects
```

Prove the difference is authorization, not authentication (evidence for Step 3)
```
# Watch the proxy decision as asha is denied:
docker logs --tail 15 proxy-app1
# you should see the login succeed upstream but the group check fail ->
# a 403, confirming denial happened at the policy layer, not at login.
```

Reset between test users (so sessions don't bleed across cases)
```
# Each identity test needs a clean session. Either use a new incognito window
# per user, or clear cookies for localhost:8080/4180/4181 between runs.
# To force Keycloak-side logout of the current SSO session:
#   open http://localhost:8080/realms/tektutor/protocol/openid-connect/logout
```

The three assertions this lab proves, stated plainly
```
# 1. Federation works:  an OpenLDAP user logs into an app via Keycloak.
# 2. SSO works:         one login reaches app1 AND app2, no second prompt.
# 3. Policy works:      a valid user outside the group is denied (AuthZ != AuthN).
```

Full SiteMinder mapping table, for your notes
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

## Lab - Build and call an MCP server

Install the MCP SDK
```
pip install mcp --break-system-packages
python3 -c "import mcp.server.fastmcp as f; print('FastMCP:', hasattr(f,'FastMCP'))"
# if pip complains about a debian-managed package (e.g. PyJWT), add:
#   pip install mcp --break-system-packages --ignore-installed PyJWT
```

Get the lab files
```
mkdir -p ~/devops-july-2026/Day5 && cd ~/devops-july-2026/Day5
# unzip the provided mcp-jenkins.zip here, or recreate server.py from the repo
cd mcp-jenkins
ls        # server.py  client_test.py  README.md
```

Run the client, which spawns the server over stdio and calls the tools
```
python3 client_test.py
# expect: tools discovered (get_build_status, list_deployments),
#         then JSON responses. source = MOCK until you point at Jenkins.
```

Point at a live Jenkins (read-only API token)
```
# Jenkins UI: click your name > Configure > API Token > Add new token
export JENKINS_URL=http://localhost:8080
export JENKINS_USER=jegan
export JENKINS_TOKEN=<paste-api-token>
python3 client_test.py         # source flips from MOCK to jenkins
```

Inspect the actual protocol calls
```
# the server logs each JSON-RPC request to stderr; run and watch:
python3 client_test.py 2>server-protocol.log
cat server-protocol.log
# you'll see: ListToolsRequest, then CallToolRequest per tool call.
# that is the client discovering the menu, then invoking a tool.
```

Ask a question that forces a tool call (in a real AI client)
```
# Wire the server into an AI MCP client (config in README.md), then ask:
#   "Did the last build of web-tier-build pass?"   -> forces get_build_status
#   "What are the last deployments to prod?"        -> forces list_deployments
# The model cannot answer from training data (it's live CI state), so it must
# call the tool. The client shows the tool name + arguments before running it.
```

Confirm read-only by design
```
# the server only issues HTTP GET to Jenkins. Prove it:
grep -n "method=" server.py           # method="GET" only
grep -ni "deploy\|build/.*/build\|POST" server.py | grep -i tool || echo "no write/deploy tool exposed"
```

Teardown
```
unset JENKINS_URL JENKINS_USER JENKINS_TOKEN
# nothing installed system-wide except the pip package; remove if you want:
# pip uninstall mcp --break-system-packages
```
