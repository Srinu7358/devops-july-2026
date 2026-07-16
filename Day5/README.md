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
