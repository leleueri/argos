# cats
Cassandra Administration Tools 
This project provides some tools to manage Cassandra Cluster.


## Configuration

This tool is based on [Akka](http://akka.io/ "Akka"), if you want to configure the application logs, please look at the akka documentation.
The cats specific configuration entry point is identified by the __cats__ section that contains two main entries :
* sentinel : this section defines the configuration of each sentinel and the JMX connectivity
* notifiers : this section defines the configuration for each notification process (currently, there are only one type of notifier *mail*)

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
	  }
	  notifiers {
		mail {
		  smtp-host= "127.0.0.1"
		  from= "cassandra-agent@no-reply"
		  recipients = ["eric.leleu@somewhere.net", "eric.leleu@somewhereelse.net"]
		}
	  }
	}
</code>
