package io.argos.agent.sentinels

import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import com.typesafe.config.Config
import io.argos.agent.Constants
import io.argos.agent.Constants._
import io.argos.agent.bean.{DroppedMessageStats, MetricsRequest, MetricsResponse, _}
import io.argos.agent.util.HostnameProvider

import scala.concurrent.duration.FiniteDuration
import scala.util.Try

abstract class ConsistencySentinel(val metricsProvider: ActorRef, val conf: Config) extends Sentinel {

  private var nextReact = System.currentTimeMillis
  private val FREQUENCY = Try(conf.getDuration(CONF_FREQUENCY, TimeUnit.MILLISECONDS)).getOrElse(FiniteDuration(5, TimeUnit.MINUTES).toMillis)

  def getReadRepairStats : MetricsRequest

  override def processProtocolElement: Receive = {

    case CheckMetrics() => if (System.currentTimeMillis >= nextReact) metricsProvider ! getReadRepairStats

    case metrics: MetricsResponse[ReadRepairStats] if metrics.value.isDefined => {

      val readRepairMsg = metrics.value.get

      if (log.isDebugEnabled) {
        log.debug("ConsistencySentinel : ReadRepair Type=<{}>, onMinRate=<{}>, total=<{}>", readRepairMsg.`type`, readRepairMsg.oneMinRate.toString, readRepairMsg.count.toString)
      }

      if (readRepairMsg.oneMinRate > 0.0) {
        react(readRepairMsg)
      }
    }
  }

  def react(info: ReadRepairStats): Unit = {

    val message =
      s"""Cassandra Node ${HostnameProvider.hostname} may be inconsistent.
        |
        |There are some ReadRepairs of type '${info.`type`}'.
        |
        |Last Minute rate : ${info.oneMinRate}
        |Last Five minutes rate : ${info.fiveMinRate}
        |Last Fifteen minutes rate : ${info.fifteenMinRate}
        |
        |Total since startup : ${info.count}
        |
        |According to the frequency of this notification, a repair may be useful...
      """.stripMargin

    context.system.eventStream.publish(buildNotification(message))

    nextReact = System.currentTimeMillis + FREQUENCY

    { }
  }
}
