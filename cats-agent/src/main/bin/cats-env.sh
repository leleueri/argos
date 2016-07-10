#!/bin/bash
#
# Define env variable for cats
#
# Be sure that CASSANDRA_HOME & CASSANDRA_INCLUDE are defined with the same path as the Cassandra instance
export CASSANDRA_HOME=/usr/share/cassandra
export CASSANDRA_INCLUDE=$CASSANDRA_HOME/cassandra.in.sh
# you can change the path for these two files
export cats_log_file=/var/log/cassandra/cats-agent.log
export cats_pid_file=/var/run/cassandra/cats-agent.pid
# update the JAVA_HOME with yours (it must be Java 8)
JAVA_HOME="/usr/java/jdk-1.8/"
JAVA="$JAVA_HOME/bin/java"
export JAVA_HOME JAVA

CATS_CLASSPATH="$CATS_HOME/conf"
for jar in `ls $CATS_HOME/lib/*.jar`; do
    CATS_CLASSPATH=$CATS_CLASSPATH:$jar
done

export $CATS_CLASSPATH

