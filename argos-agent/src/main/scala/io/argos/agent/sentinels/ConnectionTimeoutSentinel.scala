package io.argos.agent.sentinels

import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import com.typesafe.config.Config
import io.argos.agent.Constants._
import io.argos.agent.Messages
import io.argos.agent.bean.{MetricsRequest, MetricsResponse, _}
import io.argos.agent.util.HostnameProvider

import scala.concurrent.duration.FiniteDuration
import scala.util.Try

class ConnectionTimeoutSentinel(val metricsProvider: ActorRef, val conf: Config) extends Sentinel {

  private var nextReact = System.currentTimeMillis
  private val FREQUENCY = Try(conf.getDuration(CONF_FREQUENCY, TimeUnit.MILLISECONDS)).getOrElse(FiniteDuration(5, TimeUnit.MINUTES).toMillis)

  override def processProtocolElement: Receive = {

    case CheckMetrics() => if (System.currentTimeMillis >= nextReact) metricsProvider ! MetricsRequest(ActorProtocol.ACTION_CHECK_CNX_TIMEOUT, Messages.CNX_TIMEOUT)

    case metrics: MetricsResponse[ConnectionTimeoutStats] if metrics.value.isDefined => {

      val timeoutMsg = metrics.value.get

      if (log.isDebugEnabled) {
        log.debug("ConnectionTimeoutSentinel : Timeout, onMinRate=<{}>, total=<{}>", timeoutMsg.oneMinRate.toString, timeoutMsg.count.toString)
      }

      if (timeoutMsg.oneMinRate > 0.0) {
        react(timeoutMsg)
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

    context.system.eventStream.publish(buildNotification(message))

    nextReact = System.currentTimeMillis + FREQUENCY

    { }
  }
}
