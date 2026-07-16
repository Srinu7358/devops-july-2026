# Day 5

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
