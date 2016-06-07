# cats
Cassandra Administration Tools 
This project provides some tools to manage Cassandra Cluster.

## Install

First of all, package *cats* as ZIP file.
<pre>
<code>
sbt universal:package-bin
</code>
</pre>

The ZIP file will be available in the *target/universal* directory.

This archive contains:
* the startup script : cats-agent.sh 
* a configuration directory with
  * a bash script containing a set of variables
  * the application.conf file
* a directory with the cats jar

Once extracted, you have to :
* set the __CATS_HOME__ variable into the *cats-agent.sh* script, the value should be the absolute path of the directory containing the *cats-agent.sh* script
* set the __JAVA_HOME__ variable into the *cats-env.sh* script (target must be Java 8)
* Update the __CASSANDRA_HOME__ and the __CASSANDRA_INCLUDE__ variable into the *cats-env.sh* script if values are not correct
* adapt the *application.conf*

## Configuration

This tool is based on [Akka](http://akka.io/ "Akka"), if you want to configure the application logs, please look at the akka documentation.
The cats specific configuration entry point is identified by the __cats__ section that contains two main entries :
* sentinel : this section defines the configuration of each sentinel and the JMX connectivity
* notifiers : this section defines the configuration for each notification process (currently, there are only one type of notifier *mail*)

### Sentinel

A sentinel is an object that control a specific information provided by the JMX interface of the Cassandra server. If the information is considered as " *wrong* ", the sentinel will will send a notification message to the `notifiers`

#### Manager

The sentinels are managed by an Orchestrator that needs some configuration parameters. These parameters are defined into the `manager` section.

Parameter | Type | Description 
--- | --- | ---
jmx-host | `String` | IP on which the instance of Cassandra binds the JMX server
jmx-port | `Integer` | Listening port of the JMX Server
scheduler-interval | `Duration` | Duration between two Metrics validation 

#### LoadAvg sentinel

This sentinel examines the Load Average.

Parameter | Type | Description 
--- | --- | ---
enabled | `Boolean` | Specify if the sentinel is activated
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
enabled | `Boolean` | Specify if the sentinel is activated
level | `String` | Level of the notification
label | `String` | Label used into the notification *title*

#### Blocked tasks sentinel

These sentinels examine the number of blocked tasks and send a notification if the result is different of 0. There are one sentinel per type of ThreadPool.

**NOTE:** Blocked tasks sentinel are available since version 2.2 of cassandra

* stage-counter
* stage-gossip
* stage-internal
* stage-memtable
* stage-mutation
* stage-read
* stage-read-repair
* stage-request-response

Parameter | Type | Description 
--- | --- | ---
enabled | `Boolean` | Specify if the sentinel is activated
level | `String` | Level of the notification
label | `String` | Label used into the notification *title*

#### Storage Exception

This sentinel examines the number of storage exception and send a notification if the result is different of 0

Parameter | Type | Description 
--- | --- | ---
enabled | `Boolean` | Specify if the sentinel is activated
level | `String` | Level of the notification
label | `String` | Label used into the notification *title*

#### Storage Hint

This sentinel examines the number of Hinted-Handoff and send a notification if this number increases between two controls.

Parameter | Type | Description 
--- | --- | ---
enabled | `Boolean` | Specify if the sentinel is activated
level | `String` | Level of the notification
label | `String` | Label used into the notification *title*

#### Storage Space

This sentinel examines the used space on each directory declared in the Cassandra.yml. If the available space is too low, a notification is triggered.

Parameter | Type | Description 
--- | --- | ---
enabled | `Boolean` | Specify if the sentinel is activated
level | `String` | Level of the notification
label | `String` | Label used into the notification *title*
threshold | `Integer` | Percentage of available space required for the data directories
commitlog-threshold | `Integer` | Percentage of available space required for the commtilog directory

#### JMX notification

This sentinel send a notification if the JMX listener is informed about a ERROR or an ABORTED operation (like a repair)

Parameter | Type | Description 
--- | --- | ---
enabled | `Boolean` | Specify if the sentinel is activated
level | `String` | Level of the notification
label | `String` | Label used into the notification *title*
cversion | `Float` | The version of the cassandra node (needed to interpret the JMX notification message)


#### Consitency Level

This sentinel send a notification if the declared ConsistencyLevel can't be reach for a TokenRange 

Parameter | Type | Description 
--- | --- | ---
enabled | `Boolean` | Specify if the sentinel is activated
level | `String` | Level of the notification
label | `String` | Label used into the notification *title*
keyspaces | `List` | List of Object to defined the expected ConsistencyLevel for a given keyspace

The "keyspace" object has two attributes :
* name : to identify the keyspace
* cl   : to define to expected ConsistencyLevel

### Notifiers

A notifier is an object managing the notification message send by the Sentinels.
Each notifier configuration must have the key 'providerClass' specifying the implementation of the 'NotifierProvider' trait.
With this trait, the provider has to implement a props method that return the "Props" object used to create the Notifier actor.
Currently, there are only one notifier named `mail`.

Parameter | Type | Description 
--- | --- | ---
providerClass | `String` | the provider class `io.cats.agent.notifiers.MailNotifierProvider`
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
	cats {
	  sentinel {
		manager {
		  jmx-host = "127.0.0.1"
		  jmx-port = 7100
		  scheduler-interval = 5 seconds
		}
		load-avg {
		  enabled= true
		  threshold= 20.0
		  level= "CRITIC"
		  label= "Load Average"
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
		stage-counter {
		  enabled= true
		  level= "WARNING"
		  label= "Stage counter mutation"
		}
		stage-gossip {
		  enabled= true
		  level= "WARNING"
		  label= "Stage gossip"
		}
		stage-internal {
		  enabled= true
		  level= "WARNING"
		  label= "Stage Internal Response"
		}
		stage-memtable {
		  enabled= true
		  level= "WARNING"
		  label= "Stage Memtable Write Flusher"
		}
		stage-mutation {
		  enabled= true
		  level= "WARNING"
		  label= "Stage Mutation"
		}
		stage-read {
		  enabled= true
		  level= "WARNING"
		  label= "Stage Read"
		}
		stage-read-repair {
		  enabled= true
		  level= "WARNING"
		  label= "Stage ReadRepair"
		}
		stage-request-response {
		  enabled= true
		  level= "WARNING"
		  label= "Stage Request Response"
		}
		notification-jmx {
		  enabled= true
		  level= "INFO"
		  label= "Progress Event"
		  cversion=2.2
		}    
	        consitency-level {
	          enabled= true
	          level= "CRITIC"
	          label= "Consitency Level"
	          keyspaces= [
		    {
		      name= "keyspace1"
		      cl= "quorum"
		    },
		    {
		      name= "keyspace2"
		      cl= "local_one"
		    }
		  ]
		}
	  }
	  notifiers {
		mail {
		  providerClass = "io.cats.agent.notifiers.MailNotifierProvider"
		  smtp-host= "127.0.0.1"
		  smtp-port= "25"
		  from= "cassandra-agent@no-reply"
		  recipients = ["eric.leleu@somewhere.net", "eric.leleu@somewhereelse.net"]
		}
	  }
	}
</code>
</pre>
