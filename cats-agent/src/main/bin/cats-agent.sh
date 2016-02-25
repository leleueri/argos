#!/bin/bash
#
# Startup script for Cassandra Administration Tools
#
export CATS_HOME=/usr/share/cats
export CASSANDRA_HOME=/usr/share/cassandra
export CASSANDRA_INCLUDE=$CASSANDRA_HOME/cassandra.in.sh
NAME="cats-agent"
log_file=/var/log/cassandra/cats-agent.log
pid_file=/var/run/cassandra/cats-agent.pid

# The first existing directory is used for JAVA_HOME if needed.
JVM_SEARCH_DIRS="/usr/lib/jvm/jre /usr/lib/jvm/jre-1.7.* /usr/lib/jvm/java-1.7.*/jre"

# If JAVA_HOME has not been set, try to determine it.
if [ -z "$JAVA_HOME" ]; then
    # If java is in PATH, use a JAVA_HOME that corresponds to that. This is
    # both consistent with how the upstream startup script works, and with
    # the use of alternatives to set a system JVM (as is done on Debian and
    # Red Hat derivatives).
    java="`/usr/bin/which java 2>/dev/null`"
    if [ -n "$java" ]; then
        java=`readlink --canonicalize "$java"`
        JAVA_HOME=`dirname "\`dirname \$java\`"`
    else
        # No JAVA_HOME set and no java found in PATH; search for a JVM.
        for jdir in $JVM_SEARCH_DIRS; do
            if [ -x "$jdir/bin/java" ]; then
                JAVA_HOME="$jdir"
                break
            fi
        done
        # if JAVA_HOME is still empty here, punt.
    fi
fi

JAVA="$JAVA_HOME/bin/java"
export JAVA_HOME JAVA

CATS_CLASSPATH="$CATS_HOME/conf"
for jar in $CAST_HOME/lib/*.jar; do
    CATS_CLASSPATH/=$CATS_CLASSPATH:$jar
done


case "$1" in
    start)
        # Cassandra startup
        echo -n "Starting Cassandra Agent: "
        . $CASSANDRA_INCLUDE
        $JAVA -cp "$CATS_CLASSPATH:$CLASSPATH" > $log_file 2>&1
        retval=$?
        pid=$!
        [ $retval -eq 0 ] && echo "$pid" > $pid_file
        echo "OK"
        ;;
    stop)
        # Cassandra shutdown
        echo -n "Shutdown Cassandra Agent: "
        kill `cat $pid_file`
        retval=$?
        [ $retval -eq 0 ] && rm -f $pid_file
        sleep 5
        STATUS=`$0 status`
        if [[ $STATUS == "$NAME is stopped" ]]; then
            echo "OK"
        else
            echo "ERROR: could not stop $NAME:  $STATUS"
            exit 1
        fi
        ;;
    status)
        if [ -s "$pid_file" ]; then
            DIRECTORY="/proc/`cat $pid_file`"
            if [ -d "$DIRECTORY" ]; then
                echo "CATS is running ..."
            else
                echo "CATS stopped !"
            fi
        else
            echo "CATS stopped !"
        fi

        exit $?
        ;;
    *)
        echo "Usage: `basename $0` start|stop|status"
        exit 1
esac

exit 0
