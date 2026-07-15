#!/usr/bin/env bash
# Build ROOT.war for this tier. Requires JDK 17+ and a Tomcat install.
set -euo pipefail
: "${CATALINA_HOME:=/opt/tomcat}"

CP=$(find "$CATALINA_HOME/lib" -name '*.jar' | tr '\n' ':')
if [ -z "$CP" ]; then
  echo "No jars under $CATALINA_HOME/lib. Set CATALINA_HOME." >&2
  exit 1
fi

rm -rf build ROOT.war
mkdir -p build/WEB-INF/classes
javac -cp "$CP" -d build/WEB-INF/classes $(find src -name '*.java')
cp -r web/WEB-INF/* build/WEB-INF/
( cd build && jar -cf ../ROOT.war . )
echo "built $(pwd)/ROOT.war"
