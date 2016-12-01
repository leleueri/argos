package io.argos.agent.sentinels


import akka.actor.ActorRef
import com.typesafe.config.Config
import io.argos.agent.{Messages, SentinelConfiguration}
import io.argos.agent.Constants._
import io.argos.agent.bean.{MetricsRequest, MetricsResponse, _}
import io.argos.agent.util.HostnameProvider

abstract class ConsistencySentinel(val metricsProvider: ActorRef, val conf: SentinelConfiguration) extends Sentinel {

  private var previousValue = -1L

  def getReadRepairStats : MetricsRequest

  override def processProtocolElement: Receive = {

    case CheckMetrics() => if (System.currentTimeMillis >= nextReact) metricsProvider ! getReadRepairStats

    case metrics: MetricsResponse[ReadRepairStats] if metrics.value.isDefined => {

      val readRepairMsg = metrics.value.get

      if (log.isDebugEnabled) {
        log.debug("ConsistencySentinel : ReadRepair Type=<{}>, onMinRate=<{}>, total=<{}>", readRepairMsg.`type`, readRepairMsg.oneMinRate.toString, readRepairMsg.count.toString)
      }
      if (previousValue == -1) {
        previousValue = readRepairMsg.count
      } else if (readRepairMsg.oneMinRate > conf.threshold && previousValue < readRepairMsg.count) {
        react(readRepairMsg)
        previousValue = readRepairMsg.count
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

    nextReact = System.currentTimeMillis + conf.frequency

    { }
  }
}

// ------ ConsistencySentinel implementations

class ReadRepairBackgroundSentinel(override val metricsProvider: ActorRef, override val conf: SentinelConfiguration) extends ConsistencySentinel (metricsProvider, conf) {
  override def getReadRepairStats: MetricsRequest = MetricsRequest(ActorProtocol.ACTION_CHECK_READ_REPAIR, Messages.READ_REPAIR_BACKGROUND)
}

class ReadRepairBlockingSentinel(override val metricsProvider: ActorRef, override val conf: SentinelConfiguration) extends ConsistencySentinel (metricsProvider, conf) {
  override def getReadRepairStats: MetricsRequest = MetricsRequest(ActorProtocol.ACTION_CHECK_READ_REPAIR, Messages.READ_REPAIR_BLOCKING)
}
