#!/bin/sh
#
# Copyright (c) Orient Technologies LTD (http://www.orientechnologies.com)
#

echo "           .                                          "
echo "          .\`        \`                                 "
echo "          ,      \`:.                                  "
echo "         \`,\`    ,:\`                                   "
echo "         .,.   :,,                                    "
echo "         .,,  ,,,                                     "
echo "    .    .,.:::::  \`\`\`\`                                 :::::::::     :::::::::   "
echo "    ,\`   .::,,,,::.,,,,,,\`;;                      .:    ::::::::::    :::    :::  "
echo "    \`,.  ::,,,,,,,:.,,.\`  \`                       .:    :::      :::  :::     ::: "
echo "     ,,:,:,,,,,,,,::.   \`        \`         \`\`     .:    :::      :::  :::     ::: "
echo "      ,,:.,,,,,,,,,: \`::, ,,   ::,::\`   : :,::\`  ::::   :::      :::  :::    :::  "
echo "       ,:,,,,,,,,,,::,:   ,,  :.    :   ::    :   .:    :::      :::  :::::::     "
echo "        :,,,,,,,,,,:,::   ,,  :      :  :     :   .:    :::      :::  :::::::::   "
echo "  \`     :,,,,,,,,,,:,::,  ,, .::::::::  :     :   .:    :::      :::  :::     ::: "
echo "  \`,...,,:,,,,,,,,,: .:,. ,, ,,         :     :   .:    :::      :::  :::     ::: "
echo "    .,,,,::,,,,,,,:  \`: , ,,  :     \`   :     :   .:    :::      :::  :::     ::: "
echo "      ...,::,,,,::.. \`:  .,,  :,    :   :     :   .:    :::::::::::   :::     ::: "
echo "           ,::::,,,. \`:   ,,   :::::    :     :   .:    :::::::::     ::::::::::  "
echo "           ,,:\` \`,,.                                  "
echo "          ,,,    .,\`                                  "
echo "         ,,.     \`,                                          GRAPH DATABASE  "
echo "       \`\`        \`.                                                          "
echo "                 \`\`                                          orientdb.com"
echo "                 \`                                    "

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
[ -f "$ORIENTDB_HOME"/bin/server.sh ] || ORIENTDB_HOME=`cd "$PRGDIR/.." ; pwd`
export ORIENTDB_HOME
cd "$ORIENTDB_HOME/bin"

if [ ! -f "${CONFIG_FILE}" ]
then
  CONFIG_FILE=$ORIENTDB_HOME/config/orientdb-server-config.xml
fi

# Raspberry Pi check (Java VM does not run with -server argument on ARMv6)
if [ `uname -m` != "armv6l" ]; then
  JAVA_OPTS="$JAVA_OPTS -server "
fi
export JAVA_OPTS

# Set JavaHome if it exists
if [ -f "${JAVA_HOME}/bin/java" ]; then 
   JAVA=${JAVA_HOME}/bin/java
else
   JAVA=java
fi
export JAVA

LOG_FILE=$ORIENTDB_HOME/config/orientdb-server-log.properties
WWW_PATH=$ORIENTDB_HOME/www
ORIENTDB_SETTINGS="-Dprofiler.enabled=true"
JAVA_OPTS_SCRIPT="-Djna.nosys=true -XX:+HeapDumpOnOutOfMemoryError -Djava.awt.headless=true -Dfile.encoding=UTF8 -Drhino.opt.level=9"

# ORIENTDB memory options, default to 512 of heap.

if [ -z "$ORIENTDB_OPTS_MEMORY" ] ; then
    ORIENTDB_OPTS_MEMORY="-Xms512m -Xmx512m"
fi

#ORIENTDB MAXIMUM DISKCACHE IN MB, EXAMPLE, ENTER -Dstorage.diskCache.bufferSize=8192 FOR 8GB
MAXDISKCACHE=""

exec "$JAVA" $JAVA_OPTS $ORIENTDB_OPTS_MEMORY $JAVA_OPTS_SCRIPT $ORIENTDB_SETTINGS $MAXDISKCACHE \
    -Djava.util.logging.config.file="$LOG_FILE" \
    -Dorientdb.config.file="$CONFIG_FILE" \
    -Dorientdb.www.path="$WWW_PATH" \
    -Dorientdb.build.number="2.1.x@r9bc1a54a4a62c4de555fc5360357f446f8d2bc84; 2016-03-14 17:00:05+0000" \
    -cp "$ORIENTDB_HOME/lib/orientdb-server-2.1.13.jar:$ORIENTDB_HOME/lib/*" \
    $* com.orientechnologies.orient.server.OServerMain
