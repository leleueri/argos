# Argos

This project provides some tools to manage Cassandra Cluster.
Argus Panoptes (or Argos) is a 100-eyed giant in Greek mythology. With 50 opened eyes and 50 closed eyes, I guess he can keep an eye on a bunch of Cassandra nodes ;-)

## Install

First of all, package *argos* as ZIP file.
<pre>
<code>
sbt universal:package-bin
</code>
</pre>

The ZIP file will be available in the *target/universal* directory.

This archive contains:
* the startup script : argos-agent.sh 
* a configuration directory with
  * a bash script containing a set of variables
  * the application.conf file
* a directory with the argos jar

Once extracted, you have to :
* set the __ARGOS_HOME__ variable into the *argos-agent.sh* script, the value should be the absolute path of the directory containing the *argos-agent.sh* script
* set the __JAVA_HOME__ variable into the *argos-env.sh* script (target must be Java 8)
* Update the __CASSANDRA_HOME__ and the __CASSANDRA_INCLUDE__ variable into the *argos-env.sh* script if values are not correct
* adapt the *application.conf*

## Configuration

This tool is based on [Akka](http://akka.io/ "Akka"), if you want to configure the application logs, please look at the akka documentation.
The argos specific configuration entry point is identified by the __argos__ section that contains three main entries :
* metrics : this section defines the JMX connectivity 
* sentinel : this section defines the configuration of each sentinel
* notifiers : this section defines the configuration for each notification process (currently, there are only one type of notifier *mail*)

An additional parameter `scheduler-interval` is available to define the delay between two sentinel control :

Parameter | Type | Description 
--- | --- | ---
scheduler-interval | `Duration` | Duration between two Metrics validation (Default: 5 seconds)
cassandra-version | `Double` | the cassandra version x.y (ex: 2.1) (Default: 3.0)

### Metrics

The sentinels need some metrics provided by the JMX interface of the cassandra node, this section configures the JMX connection parameters.

Parameter | Type | Description 
--- | --- | ---
jmx-host | `String` | IP on which the instance of Cassandra binds the JMX server (Default: 127.0.0.1)
jmx-port | `Integer` | Listening port of the JMX Server (Default: 7199)

Because the Metric Provider can send notification when the connection to the cassandra node is lost and reestablished, there are some optional configuration parameters.

Parameter | Type | Description 
--- | --- | ---
node-down-label | `String` | Level of the notification when the Cassandra node is unreachable (Default: CRITIC)
node-down-level | `String` | Label used into the notification *title* (Default: Cassandra node is DOWN)
node-up-label | `String` |  Level of the notification when the Cassandra node comes back on line (Default: INFO)
node-up-level | `String` | Label used into the notification *title* (Default: Cassandra node is UP)

### Sentinel

A sentinel is an actor that control a specific information provided by the JMX interface of the Cassandra server. If the information is considered as " *wrong* ", the sentinel will will send a notification message to the `notifiers`.

The sentinel name is defined by the children key of the `argos.sentinel` entry.

#### LoadAvg sentinel

This sentinel examines the Load Average.

Parameter | Type | Description 
--- | --- | ---
enabled | `Boolean` | Specify if the sentinel is activated (Default: true)
threshold | `Float` | The maximum value authorized for the LoadAvg metrics
level | `String` | Level of the notification
label | `String` | Label used into the notification *title*

#### Dropped messages sentinel

These sentinels examine the number of dropped messages, if the number of dropped messages change between to controls a notification is triggered. There are one sentinel per type of Messages.

* dropped-counter
* dropped-mutation
* dropped-read 
* dropped-read-repair 
* dropped-range-slice 
* dropped-request-response 
* dropped-page-range 

Parameter | Type | Description 
--- | --- | ---
enabled | `Boolean` | Specify if the sentinel is activated (Default: true)
level | `String` | Level of the notification
label | `String` | Label used into the notification *title*

#### Consistency issue sentinel

These sentinels examine the number of ReadRepair tasks (Background & Blocking) and send a notification if the 1MinuteRate is different of 0.

There are two sentinels, one per ReadRepair type:

* consistency-repaired-blocking
* consistency-repaired-background

Parameter | Type | Description 
--- | --- | ---
enabled | `Boolean` | Specify if the sentinel is activated (Default: true)
threshold | `Integer` | The maximum value authorized (default : 0)
level | `String` | Level of the notification
label | `String` | Label used into the notification *title*

#### Connection Timeout sentinel

These sentinels examine the number of Connection Timeout and send a notification if the 1MinuteRate is different of 0.

* connection-timeouts

Parameter | Type | Description 
--- | --- | ---
enabled | `Boolean` | Specify if the sentinel is activated (Default: true)
level | `String` | Level of the notification
label | `String` | Label used into the notification *title*

#### Blocked tasks sentinel

These sentinels examines the number of blocked tasks and send a notification if the result is different of 0. There are one sentinel per type of ThreadPool.

* blocked-stage-counter
* blocked-stage-gossip
* blocked-stage-internal
* blocked-stage-memtable
* blocked-stage-mutation
* blocked-stage-read
* blocked-stage-read-repair
* blocked-stage-request-response
* blocked-stage-compaction

Parameter | Type | Description 
--- | --- | ---
enabled | `Boolean` | Specify if the sentinel is activated (Default: true)
level | `String` | Level of the notification
label | `String` | Label used into the notification *title*

#### Pending tasks sentinel

These sentinels examines the number of pending tasks and send a notification if the result exceed the threshold. There are one sentinel per type of ThreadPool.
Because having too many pending tasks may be not an issue, these sentinels cumulates the ThreadPool state in a queue and throws a notification only if all states present into the buffer exceed the limit.

* pending-stage-counter
* pending-stage-gossip
* pending-stage-internal
* pending-stage-memtable
* pending-stage-mutation
* pending-stage-read
* pending-stage-read-repair
* pending-stage-request-response
* pending-stage-compaction

Parameter | Type | Description 
--- | --- | ---
enabled | `Boolean` | Specify if the sentinel is activated (Default: true)
level | `String` | Level of the notification
label | `String` | Label used into the notification *title*
window-size | `Integer` | the size of the buffer (default: 5)
threshold | `Integer` | The maximum value authorized (default : 25)

#### Storage Exception

This sentinel examines the number of storage exceptions and send a notification if the result is different of 0

Parameter | Type | Description 
--- | --- | ---
enabled | `Boolean` | Specify if the sentinel is activated (Default: true)
level | `String` | Level of the notification
label | `String` | Label used into the notification *title*

#### Storage Hint

This sentinel examines the number of Hinted-Handoff and send a notification if this number increases between two controls.

Parameter | Type | Description 
--- | --- | ---
enabled | `Boolean` | Specify if the sentinel is activated (Default: true)
level | `String` | Level of the notification
label | `String` | Label used into the notification *title*

#### Storage Space

This sentinel examines the used space on each directory declared in the Cassandra.yml. If the available space is too low, a notification is triggered.

Parameter | Type | Description 
--- | --- | ---
enabled | `Boolean` | Specify if the sentinel is activated (Default: true)
level | `String` | Level of the notification
label | `String` | Label used into the notification *title*
threshold | `Integer` | Percentage of available space required for the data directories
commitlog-threshold | `Integer` | Percentage of available space required for the commtilog directory

#### GC Inspector

This sentinel examines the duration of GC. If a GC duration is too long (bigger than the threshold), a notification is triggered.

Parameter | Type | Description 
--- | --- | ---
enabled | `Boolean` | Specify if the sentinel is activated (Default: true)
level | `String` | Level of the notification
label | `String` | Label used into the notification *title*
threshold | `Integer` | max duration (in ms) for a GC

#### JMX notification

This sentinel send a notification if the JMX listener is informed about a ERROR or an ABORTED operation (like a repair)

Parameter | Type | Description 
--- | --- | ---
enabled | `Boolean` | Specify if the sentinel is activated (Default: true)
level | `String` | Level of the notification
label | `String` | Label used into the notification *title*

#### Consitency Level

This sentinel send a notification if the declared ConsistencyLevel can't be reach for a TokenRange 

Parameter | Type | Description 
--- | --- | ---
enabled | `Boolean` | Specify if the sentinel is activated (Default: true)
level | `String` | Level of the notification
label | `String` | Label used into the notification *title*
keyspaces | `List` | List of Object to defined the expected ConsistencyLevel for a given keyspace

The "keyspace" object has two attributes :
* name : to identify the keyspace
* cl   : to define to expected ConsistencyLevel

#### Period

Each sentinel may configure the 'period' between two notifications to avoid flooding the alert receiver.
By default, the period between two alerts is set to 5 minutes (excepted for disk capacity, the period is 4 H)

### Notifiers

A notifier is an object managing the notification message send by the Sentinels.
Each notifier configuration must have the key `providerClass` specifying the implementation of the `NotifierProvider` trait.
With this trait, the provider has to implement a props method that return the "Props" object used to create the Notifier actor.

Whatever the notifier implementation, there are two common configuration entries available to filter the notifications:

Parameter | Type | Description 
--- | --- | ---
white-list | `List[String]` | the notifier will manage notifications send by the sentinel present in this list
black-list | `List[String]` | the notifier will ignore notifications send by the sentinel present in this list

If these lists are empty, the filtering is disabled and all notification will be managed by the Notifier implementation.
If both lists are filled, only the while list will be used.

Currently, there are only one notifier named `mail`.

Parameter | Type | Description 
--- | --- | ---
providerClass | `String` | the provider class `io.argos.agent.notifiers.MailNotifierProvider`
smtp-host | `String` | hostname of the SMTP service
smtp-port | `String` | port of the SMTP service
from | `String` | The email address specified into the from header
recipients | `List[String]` | list of recipients that will receive the notification

### Example

<pre>
<code>

akka {
  loglevel = "INFO"
}

argos {
  scheduler-interval = 5 seconds
  cassandra-version = "2.1"
  metrics {
    jmx-host = "127.0.0.1"
    jmx-port = 7100
  }
  sentinel {
    load-avg {
      enabled= true
      threshold= 20.0
      level= "CRITIC"
      label= "Load Average"
    }
    consitency-level {
      enabled= true
	  period = 5 minutes 
      level= "CRITIC"
      label= "Consitency Level"
      keyspaces= [
        {
          name= "excelsior"
          cl= "quorum"
        },
        {
          name= "excelsior"
          cl= "local_one"
        },
        {
          name= "excelsior"
          cl= "all"
        }
      ]
    }
    consistency-repaired-blocking {
      enabled= true
      level= "WARNING"
      label= "Blocking Read repairs"
    }
    consistency-repaired-background {
      enabled= true
      level= "WARNING"
      label= "Background Read repairs"
    }
    connection-timeouts {
      enabled= true
      level= "WARNING"
      label= "Connection Timeouts"
    }
    dropped-counter {
      enabled= true
      level= "WARNING"
      label= "Dropped Counter Mutation"
    }
    dropped-mutation {
      enabled= true
      level= "WARNING"
      label= "Dropped Mutation"
    }
    dropped-read {
      enabled= true
      level= "WARNING"
      label= "Dropped Read"
    }
    dropped-read-repair {
      enabled= true
      level= "WARNING"
      label= "Dropped ReadRepair"
    }
    dropped-range-slice {
      enabled= true
      level= "WARNING"
      label= "Dropped Range Slice"
    }
    dropped-request-response {
      enabled= true
      level= "WARNING"
      label= "Dropped Request Response"
    }
    dropped-page-range {
      enabled= true
      level= "WARNING"
      label= "Dropped Request Response"
    }
    storage-space {
      enabled= true
      level= "CRITIC"
      label= "Few Disk Space"
      threshold= 50
      commitlog-threshold= 5
    }
    storage-exception {
      enabled= true
      level= "CRITIC"
      label= "Cassandra Storage Exception"
    }
    storage-hints {
      enabled= true
      level= "CRITIC"
      label= "Network partition"
    }
    gc-inspector{
      enabled= true
      level= "WARNING"
      label= "GC too long"
      threshold= 200
    }
    blocked-stage-counter {
      enabled= true
      level= "WARNING"
      label= "Stage counter mutation"
    }
    blocked-stage-gossip {
      enabled= true
      level= "WARNING"
      label= "Stage gossip"
    }
    blocked-stage-internal {
      enabled= true
      level= "WARNING"
      label= "Stage Internal Response"
    }
    blocked-stage-memtable {
      enabled= true
      level= "WARNING"
      label= "Stage Memtable Write Flusher"
    }
    blocked-stage-mutation {
      enabled= true
      level= "WARNING"
      label= "Stage Mutation"
    }
    blocked-stage-read {
      enabled= true
      level= "WARNING"
      label= "Stage Read"
    }
    blocked-stage-read-repair {
      enabled= true
      level= "WARNING"
      label= "Stage ReadRepair"
    }
    blocked-stage-request-response {
      enabled= true
      level= "WARNING"
      label= "Stage Request Response"
    }
    blocked-stage-compaction {
      enabled= true
      level= "WARNING"
      label= "Stage Compaction Executor"
    }
    notification-jmx {
      enabled= true
      level= "INFO"
      label= "Progress Event"
    }
    pending-stage-counter {
      enabled= true
      level= "WARNING"
      label= "Stage counter mutation - pending"
      threshold= 25
      window-size= 10
    }
    pending-stage-gossip {
      enabled= true
      level= "WARNING"
      label= "Stage gossip - pending"
      threshold= 25
      window-size= 10
    }
    pending-stage-internal {
      enabled= true
      level= "WARNING"
      label= "Stage Internal Response - pending"
      threshold= 25
      window-size= 10
    }
    pending-stage-memtable {
      enabled= true
      level= "WARNING"
      label= "Stage Memtable Write Flusher - pending"
      threshold= 25
      window-size= 10
    }
    pending-stage-mutation {
      enabled= true
      level= "WARNING"
      label= "Stage Mutation - pending"
      threshold= 25
      window-size= 10
    }
    pending-stage-read {
      enabled= true
      level= "WARNING"
      label= "Stage Read - pending"
      threshold= 25
      window-size= 10
    }
    pending-stage-read-repair {
      enabled= true
      level= "WARNING"
      label= "Stage ReadRepair - pending"
      threshold= 25
      window-size= 10
    }
    pending-stage-request-response {
      enabled= true
      level= "WARNING"
      label= "Stage Request Response - pending"
      threshold= 25
      window-size= 10
    }
    pending-stage-compaction {
      enabled= true
      level= "WARNING"
      label= "Stage Compaction Executor - pending"
      threshold= 25
      window-size= 10
    }
  }
  notifiers {
    mail {
      providerClass = "io.argos.agent.notifiers.MailNotifierProvider"
      smtp-host= "127.0.0.1"
      smtp-port= "25"
      from= "cassandra-agent@no-reply"
      recipients = ["eric.leleu@somewhere.net", "eric.leleu@somewhereelse.net"]
      //white-list= ["consitency-level", "notification-jmx"]
      //black-list= ["consitency-level", "notification-jmx"]
    }
  }
}
</code>
</pre>
