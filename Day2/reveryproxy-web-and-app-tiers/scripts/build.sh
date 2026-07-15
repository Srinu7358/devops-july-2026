#!/bin/bash
# Builds ROOT.war (web tier) and api.war (app tier).
# Tries Maven first; falls back to javac + jar against Tomcat's own servlet-api.jar.
# Run from the project root:  ./scripts/build.sh
set -e

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
mkdir -p dist

if command -v mvn >/dev/null 2>&1; then
  echo "=== Building with Maven ==="
  (cd web-tier && mvn -q clean package)
  (cd app-tier && mvn -q clean package)
  cp web-tier/target/ROOT.war dist/
  cp app-tier/target/api.war  dist/
else
  echo "=== Maven not found. Building with javac + jar ==="
  SERVLET_API="${SERVLET_API:-/opt/tomcat11/lib/servlet-api.jar}"
  if [ ! -f "$SERVLET_API" ]; then
    echo "ERROR: cannot find servlet-api.jar."
    echo "Set SERVLET_API=/path/to/servlet-api.jar and re-run,"
    echo "or install Maven. Tomcat 11 ships it at /opt/tomcat11/lib/servlet-api.jar"
    exit 1
  fi

  build_war () {
    local name="$1" src="$2" warname="$3"
    echo "--- $name ---"
    rm -rf "build/$name"
    mkdir -p "build/$name/WEB-INF/classes"
    javac -cp "$SERVLET_API" --release 17 \
          -d "build/$name/WEB-INF/classes" \
          $(find "$src" -name '*.java')
    (cd "build/$name" && jar -cf "$ROOT/dist/$warname" .)
  }

  build_war webtier web-tier/src/main/java ROOT.war
  build_war apitier app-tier/src/main/java api.war
fi

echo
echo "=== Built ==="
ls -l dist/
echo
echo "Verify each WAR contains its servlet class:"
unzip -l dist/ROOT.war | grep -E "WebTierServlet.class" || echo "  MISSING in ROOT.war"
unzip -l dist/api.war  | grep -E "ApiServlet.class"     || echo "  MISSING in api.war"
