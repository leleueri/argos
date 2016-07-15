package io.argos.agent.sentinels

import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import akka.event.EventStream
import com.typesafe.config.Config
import io.argos.agent.Constants
import io.argos.agent.bean.{AvailabilityRequirements, Availability}
import io.argos.agent.util.{HostnameProvider, CommonLoggerFactory}
import Constants._
import io.argos.agent.bean._
import CommonLoggerFactory._
import org.apache.cassandra.tools.NodeProbe

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

import scala.collection.JavaConverters._

class AvailabilitySentinel(val metricsProvider: ActorRef, override val conf: Config) extends Sentinel {

  private var nextReact = System.currentTimeMillis
  private val FREQUENCY = Try(conf.getDuration(CONF_FREQUENCY, TimeUnit.MILLISECONDS)).getOrElse(FiniteDuration(5, TimeUnit.MINUTES).toMillis)


  override def processProtocolElement: Receive = {
    case CheckMetrics() => if (System.currentTimeMillis >= nextReact) {
      val tt = for {
        pair <- conf.getConfigList(CONF_KEYSPACES).asScala.toList
        ks = pair.getString(CONF_KEYSPACE_NAME)
        cl = pair.getString(CONF_CONSISTENCY_LEVEL)
      } yield AvailabilityRequirements(ks, cl)
      tt.foreach{
        cc => metricsProvider ! cc
      }
    }
    case AvailabilityIssue(issues) => react(issues)
  }

  def react(info:  List[Availability]): Unit = {

    info.foreach {
      availabilityInfo =>
        val message = s"""Application may have consistency issue (detected by cassandra node '${HostnameProvider.hostname}')
                  |
                  |Unreachable ${availabilityInfo.unreachableEndpoints.length} replica(s) for range[${availabilityInfo.tokenRange.start}, ${availabilityInfo.tokenRange.end}]
                  |
                  |Unreachable replicas : ${availabilityInfo.unreachableEndpoints.mkString("['", "', '", "']")}
                  |Keyspace : ${availabilityInfo.keyspace}
                  |Tested Consistency Level : ${availabilityInfo.consistencyLevel}
                  |Endpoints details : ${availabilityInfo.tokenRange.endpointDetails}
                """.stripMargin

        context.system.eventStream.publish(buildNotification(message))
    }

    nextReact = System.currentTimeMillis + FREQUENCY

    { }
  }
}
