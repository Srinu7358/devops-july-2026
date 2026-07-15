#!/bin/bash
export JAVA_HOME=$(readlink -f $(which java) | sed "s:/bin/java::")
export CATALINA_OPTS="-Xms256m -Xmx512m -Djava.awt.headless=true"
