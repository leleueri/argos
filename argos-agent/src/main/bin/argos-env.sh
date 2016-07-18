#!/bin/bash
#
# Define env variable for cats
#
# Be sure that CASSANDRA_HOME & CASSANDRA_INCLUDE are defined with the same path as the Cassandra instance
export CASSANDRA_HOME=/usr/share/cassandra
export CASSANDRA_INCLUDE=$CASSANDRA_HOME/cassandra.in.sh

# you can change the path for these two files
export argos_log_file=/var/log/cassandra/argos-agent.log
export argos_pid_file=/var/run/cassandra/argos-agent.pid

# update the JAVA_HOME with yours (it must be Java 8)
JAVA_HOME="/usr/java/jdk-1.8/"
JAVA="$JAVA_HOME/bin/java"
export JAVA_HOME JAVA

ACLASSPATH="$ARGOS_HOME/conf"
for jar in `ls $ARGOS_HOME/lib/*.jar`; do
    ACLASSPATH=$ACLASSPATH:$jar
done

export ARGOS_CLASSPATH=$ACLASSPATH

