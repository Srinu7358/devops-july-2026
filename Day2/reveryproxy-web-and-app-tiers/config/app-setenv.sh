#!/bin/bash
export JAVA_HOME=$(readlink -f $(which java) | sed "s:/bin/java::")
export CATALINA_OPTS="-Xms512m -Xmx1024m -Djava.awt.headless=true"
