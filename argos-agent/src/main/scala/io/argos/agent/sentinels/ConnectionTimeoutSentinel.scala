package io.argos.agent.sentinels


import akka.actor.ActorRef
import io.argos.agent.{Messages, SentinelConfiguration}
import io.argos.agent.bean.{MetricsRequest, MetricsResponse, _}
import io.argos.agent.util.HostnameProvider

class ConnectionTimeoutSentinel(val metricsProvider: ActorRef, val conf: SentinelConfiguration) extends Sentinel {

  private var previousValue = -1L

  override def processProtocolElement: Receive = {

    case CheckMetrics() => if (System.currentTimeMillis >= nextReact) metricsProvider ! MetricsRequest(ActorProtocol.ACTION_CHECK_CNX_TIMEOUT, Messages.CNX_TIMEOUT)

    case metrics: MetricsResponse[ConnectionTimeoutStats] if metrics.value.isDefined => {

      val timeoutMsg = metrics.value.get

      if (log.isDebugEnabled) {
        log.debug("ConnectionTimeoutSentinel : Timeout, onMinRate=<{}>, total=<{}>", timeoutMsg.oneMinRate.toString, timeoutMsg.count.toString)
      }

      if (timeoutMsg.oneMinRate > 0.0 && previousValue < timeoutMsg.count) {
        react(timeoutMsg)
        previousValue = timeoutMsg.count
      } else if (previousValue == -1) {
        previousValue = timeoutMsg.count
      }
    }
  }

  def react(info: ConnectionTimeoutStats): Unit = {

    val message =
      s"""Cassandra Node ${HostnameProvider.hostname} may be inconsistent.
        |
        |There are some connection timeouts.
        |
        |Last Minute rate : ${info.oneMinRate}
        |Last Five minutes rate : ${info.fiveMinRate}
        |Last Fifteen minutes rate : ${info.fifteenMinRate}
        |
        |Total since startup : ${info.count}
        |
        |According to the frequency of this notification, a repair may be useful...
      """.stripMargin

    context.system.eventStream.publish(buildNotification(conf.messageHeader.map(h => h + " \n\n--####--\n\n" + message).getOrElse(message)))

    updateNextReact()

    { }
  }
}
