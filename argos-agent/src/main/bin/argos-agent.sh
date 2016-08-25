#!/bin/bash
#
# Startup script for Cassandra Administration Tools
#

# change the HOME directory according to you installation
export ARGOS_HOME=/usr/share/argos
NAME="argos-agent"

. $ARGOS_HOME/conf/argos-env.sh

ARGOS_JAVA_OPTS=" -Xmx128m "

case "$1" in
    start)
        # Cassandra startup
        echo -n "Starting Cassandra Agent: "
        . $CASSANDRA_INCLUDE
 	    if [ -s "$argos_pid_file" ]; then
            DIRECTORY="/proc/`cat $argos_pid_file`"
            if [ -d "$DIRECTORY" ]; then
                echo "$NAME is running ..."
                exit 1
            fi
        fi
        nohup $JAVA -cp "$ARGOS_CLASSPATH:$CLASSPATH" $ARGOS_JAVA_OPTS io.argos.agent.Launcher > $argos_log_file 2>&1 &
        retval=$?
        pid=$!
        [ $retval -eq 0 ] && echo "$pid" > $argos_pid_file
        echo "OK"
        ;;
    stop)
        # Cassandra shutdown
        echo -n "Shutdown Cassandra Agent: "
        AGENT_PID=`cat $argos_pid_file`
        DIRECTORY="/proc/$AGENT_PID"
        if [ -d "$DIRECTORY" ]; then
            kill $AGENT_PID
        fi
        retval=$?
        [ $retval -eq 0 ] && rm -f $argos_pid_file
        sleep 5
        STATUS=`$0 status`
        if [[ $STATUS == "$NAME stopped !" ]]; then
            echo "OK"
        else
            echo "ERROR: could not stop $NAME: $STATUS"
            exit 1
        fi
        ;;
    status)
        if [ -s "$argos_pid_file" ]; then
            DIRECTORY="/proc/`cat $argos_pid_file`"
            if [ -d "$DIRECTORY" ]; then
                echo "$NAME is running ..."
            else
                echo "$NAME stopped !"
            fi
        else
            echo "$NAME stopped !"
        fi

        exit $?
        ;;
    *)
        echo "Usage: `basename $0` start|stop|status"
        exit 1
esac

exit 0
