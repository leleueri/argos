package io.argos.agent.sentinels

import akka.actor.ActorRef
import io.argos.agent.SentinelConfiguration
import io.argos.agent.bean.{ActorProtocol, MetricsRequest, MetricsResponse}
import io.argos.agent.util.HostnameProvider
import io.argos.agent.bean._

class StorageExceptionSentinel(val metricsProvider: ActorRef, override val conf: SentinelConfiguration) extends Sentinel {

  private var previousValue : Long = 0

  override def processProtocolElement: Receive = {

    case CheckMetrics() => if (System.currentTimeMillis >= nextReact) metricsProvider ! MetricsRequest(ActorProtocol.ACTION_CHECK_STORAGE_EXCEPTION, "")

    case metrics: MetricsResponse[Long] if metrics.value.isDefined => {
      val storageExc = metrics.value.get
      val exception = storageExc - previousValue
      if (exception > 0 && (System.currentTimeMillis >= nextReact)) {
        previousValue = storageExc
        react(exception)
      } else None
    }
  }

  def react(info: Long): Unit = {
    val message = s"""Cassandra Node ${HostnameProvider.hostname} has some storage exceptions.
         |
         | exceptions : ${info}
         |
         | You should check cassandra logs (see /var/log/cassandra/system.log or custom location).
       """.stripMargin

    context.system.eventStream.publish(buildNotification(conf.messageHeader.map(h => h + " \n\n--####--\n\n" + message).getOrElse(message)))

    updateNextReact()
  }
}
