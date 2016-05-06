#!/bin/bash -x
#
# Define env variable for cats
#
export CASSANDRA_HOME=/usr/share/cassandra
export CASSANDRA_INCLUDE=$CASSANDRA_HOME/cassandra.in.sh
export cats_log_file=/var/log/cassandra/cats-agent.log
export cats_pid_file=/var/run/cassandra/cats-agent.pid

JAVA_HOME="/usr/java/jdk-1.8/"
JAVA="$JAVA_HOME/bin/java"
export JAVA_HOME JAVA

CATS_CLASSPATH="$CATS_HOME/conf"
for jar in `ls $CATS_HOME/lib/*.jar`; do
    CATS_CLASSPATH=$CATS_CLASSPATH:$jar
done

export $CATS_CLASSPATH

