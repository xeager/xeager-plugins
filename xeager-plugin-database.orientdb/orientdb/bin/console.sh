#!/bin/sh
#
# Copyright (c) Orient Technologies LTD (http://www.orientechnologies.com)
#

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

# Get standard environment variables
PRGDIR=`dirname "$PRG"`

# Only set ORIENTDB_HOME if not already set
[ -f "$ORIENTDB_HOME"/lib/orientdb-tools-2.1.13.jar ] || ORIENTDB_HOME=`cd "$PRGDIR/.." ; pwd`
export ORIENTDB_HOME
cd "$ORIENTDB_HOME/bin"

# Set JavaHome if it exists
if [ -f "${JAVA_HOME}/bin/java" ]; then 
   JAVA=${JAVA_HOME}/bin/java
else
   JAVA=java
fi
export JAVA

ORIENTDB_SETTINGS="-Djava.util.logging.config.file=\"$ORIENTDB_HOME/config/orientdb-client-log.properties\" -Djava.awt.headless=true"
#JAVA_OPTS=-Xmx1024m
KEYSTORE="$ORIENTDB_HOME/config/cert/orientdb-console.ks"
KEYSTORE_PASS=password
TRUSTSTORE="$ORIENTDB_HOME/config/cert/orientdb-console.ts"
TRUSTSTORE_PASS=password
SSL_OPTS="-Xmx512m -Dclient.ssl.enabled=false "

exec "$JAVA" -client $JAVA_OPTS $ORIENTDB_SETTINGS $SSL_OPTS \
    -Dfile.encoding=utf-8 -Dorientdb.build.number="2.1.x@r9bc1a54a4a62c4de555fc5360357f446f8d2bc84; 2016-03-14 17:00:05+0000" \
    -cp "$ORIENTDB_HOME/lib/orientdb-tools-2.1.13.jar:$ORIENTDB_HOME/lib/*:$ORIENTDB_HOME/plugins/*" \
    "-Djavax.net.ssl.keyStore=$KEYSTORE" "-Djavax.net.ssl.keyStorePassword=$KEYSTORE_PASS" "-Djavax.net.ssl.trustStore=$TRUSTSTORE" "-Djavax.net.ssl.trustStorePassword=$TRUSTSTORE_PASS" \
    com.orientechnologies.orient.graph.console.OGremlinConsole $*
