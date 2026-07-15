# App-Tier Push Plugin: Restrict Access at the Web Tier

A two-tier Tomcat setup where the app tier decides which of its hosted paths are
public, generates an allow-list, and pushes it to a web-tier Tomcat. The web
tier forwards only approved paths to the app tier and blocks everything else,
including paths the app tier still hosts.

Runs on one Linux machine. The two Tomcats are separated by port.

## Objectives

- Web-tier Tomcat reads a pushed allow-list and forwards only approved paths to
  the app-tier Tomcat.
- The app server generates the allow-list and pushes it to the web tier.
- A path not in the allow-list is blocked at the web tier even if the app server
  still hosts it.

## Architecture

```
client → web tier (8080)                        app tier (9080)
         GatewayFilter  --forward approved-->    /api/products    [public]
         AllowListStore                          /api/orders      [public]
         /admin/allowlist <--push list--         /api/internal/metrics [hosted, not pushed]
                                                 AllowListPublisher (push plugin)
```

The app tier is the source of truth. The web tier enforces only what it was
pushed. Before the first push the allow-list is empty, so the gate fails closed
and blocks everything.

## Port map (all on 127.0.0.1)

| Instance   | HTTP | Shutdown | Role                         |
|------------|------|----------|------------------------------|
| Tomcat web | 8080 | 8005     | gateway, enforces allow-list |
| Tomcat app | 9080 | 9005     | hosts endpoints, pushes list |

## Repository layout

```
.
├── README.md              this file
├── pushgate-lab.md        full lab guide
├── pushgate-lab.pdf       lab guide as PDF
├── web-tier/              gateway webapp (deploy as ROOT on 8080)
│   ├── build.sh
│   ├── src/gw/AllowListStore.java
│   ├── src/gw/AllowListServlet.java   POST /admin/allowlist receives the push
│   ├── src/gw/GatewayFilter.java      /* gate + reverse proxy
│   └── web/WEB-INF/web.xml            pushToken, backendBase
└── app-tier/              endpoints + push plugin (deploy as ROOT on 9080)
    ├── build.sh
    ├── src/app/ProductsServlet.java   /api/products        [public]
    ├── src/app/OrdersServlet.java     /api/orders          [public]
    ├── src/app/MetricsServlet.java    /api/internal/metrics [hosted, not pushed]
    ├── src/app/AllowListPublisher.java the push plugin (@WebListener)
    ├── src/app/PublishServlet.java    /admin/publish re-push on demand
    └── web/WEB-INF/web.xml            publicPaths, webTierPushUrl, pushToken
```

## Prerequisites

- One Linux machine.
- Tomcat 10.1 at `/opt/tomcat` (`CATALINA_HOME`). Tomcat 9 needs `web.xml` on
  the `javaee` namespace and `javax.*` imports instead of `jakarta.*`.
- JDK 17 or later (uses `java.net.http.HttpClient`).
- `curl`.

## Quick start

```sh
export CATALINA_HOME=/opt/tomcat

# 1. two instances from one install
for n in webtier apptier; do
  mkdir -p /opt/tomcat/$n/{conf,logs,temp,webapps,work,bin}
  cp -r $CATALINA_HOME/conf/* /opt/tomcat/$n/conf/
done
# then set app tier to 9080/9005 in /opt/tomcat/apptier/conf/server.xml

# 2. build and deploy
cd web-tier && ./build.sh && cd ..
cd app-tier && ./build.sh && cd ..
cp web-tier/ROOT.war /opt/tomcat/webtier/webapps/
cp app-tier/ROOT.war /opt/tomcat/apptier/webapps/

# 3. start web tier first, then app tier (so the push target exists)
CATALINA_BASE=/opt/tomcat/webtier $CATALINA_HOME/bin/catalina.sh start
CATALINA_BASE=/opt/tomcat/apptier $CATALINA_HOME/bin/catalina.sh start
```

## What to expect

```sh
# the pushed list the web tier now holds
curl http://127.0.0.1:8080/admin/allowlist
#   /api/products
#   /api/orders

# approved path: forwarded to the app tier
curl http://127.0.0.1:8080/api/products
#   app-tier: products list [public]

# hosted but not approved: app tier serves it directly...
curl -i http://127.0.0.1:9080/api/internal/metrics     # 200
# ...but the web tier blocks it
curl -i http://127.0.0.1:8080/api/internal/metrics     # 403 blocked at web tier
```

Change `publicPaths` on the app tier and re-push (`curl
http://127.0.0.1:9080/admin/publish`) to add or remove access at runtime. The
web tier follows whatever the app tier last pushed.

## Security model

The push endpoint is protected by a shared token (`X-Push-Token`) that must
match in both `web.xml` files. For production, move to mutual TLS or an IP
allow-list on the push endpoint, push over HTTPS, sign the payload, and persist
the last list on the web tier so a restart does not briefly allow nothing.
The gate fails closed by design: an empty list blocks every path.

## License

Training material. Use and adapt freely.
