#!/bin/bash -x
#
# Startup script for Cassandra Administration Tools
#
export CATS_HOME=/root/test-cats
NAME="cats-agent"

. $CATS_HOME/conf/cats-env.sh

case "$1" in
    start)
        # Cassandra startup
        echo -n "Starting Cassandra Agent: "
        . $CASSANDRA_INCLUDE
 	if [ -s "$cats_pid_file" ]; then
            DIRECTORY="/proc/`cat $cats_pid_file`"
            if [ -d "$DIRECTORY" ]; then
                echo "$NAME is running ..."
                exit 1
            fi
        fi
        nohup $JAVA -cp "$CATS_CLASSPATH:$CLASSPATH" io.cats.agent.Launcher > $cats_log_file 2>&1 &
        retval=$?
        pid=$!
        [ $retval -eq 0 ] && echo "$pid" > $cats_pid_file
        echo "OK"
        ;;
    stop)
        # Cassandra shutdown
        echo -n "Shutdown Cassandra Agent: "
        kill `cat $cats_pid_file`
        retval=$?
        [ $retval -eq 0 ] && rm -f $cats_pid_file
        sleep 5
        STATUS=`$0 status`
        if [[ $STATUS == "$NAME stopped !" ]]; then
            echo "OK"
        else
            echo "ERROR: could not stop $NAME:  $STATUS"
            exit 1
        fi
        ;;
    status)
        if [ -s "$cats_pid_file" ]; then
            DIRECTORY="/proc/`cat $cats_pid_file`"
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

