package io.argos.agent.sentinels


import akka.actor.ActorRef
import io.argos.agent.{Constants, SentinelConfiguration}
import io.argos.agent.bean.{Availability, AvailabilityRequirements}
import io.argos.agent.util.HostnameProvider
import io.argos.agent.bean._

class AvailabilitySentinel(val metricsProvider: ActorRef, override val conf: SentinelConfiguration) extends Sentinel {

  override def processProtocolElement: Receive = {
    case CheckMetrics() => if (System.currentTimeMillis >= nextReact) {
      conf.keyspaces.foreach{
        req => metricsProvider ! AvailabilityRequirements(req.keyspace, req.consistency)
      }
    }
    case AvailabilityIssue(issues) => react(issues)
  }

  def react(info:  List[Availability]): Unit = {

    info.foreach {
      availabilityInfo =>
        val message = s"""Application may have consistency issue (detected by cassandra node '${HostnameProvider.hostname}')
                  |
                  |${availabilityInfo.unreachableEndpoints.length} replica(s) unreachable
                  |
                  |Keyspace : ${availabilityInfo.keyspace}
                  |Tested Consistency Level : ${availabilityInfo.consistencyLevel}
                  |Unreachable replicas : ${availabilityInfo.unreachableEndpoints.mkString("['", "', '", "']")}
                """.stripMargin

        context.system.eventStream.publish(buildNotification(conf.messageHeader.map(h => h + " \n\n--####--\n\n" + message).getOrElse(message)))
    }

    if (!info.isEmpty){
      updateNextReact()
    }

    { }
  }
}
