package io.argos.agent.sentinels

import akka.actor.ActorRef
import com.typesafe.config.Config
import io.argos.agent.bean.{MetricsRequest, MetricsResponse, ActorProtocol}
import io.argos.agent.util.HostnameProvider
import io.argos.agent.bean._


class StorageHintsSentinel(val metricsProvider: ActorRef, override val conf: Config) extends Sentinel {

  private var previousValue : Array[Long] = Array(-1,-1)

  override def receive: Receive = {
    case msg => if (isEnabled) {
      processProtocolElement(msg)
    } else {
      log.debug("Not enabled : {}", msg)
    }
  }
  override def processProtocolElement: Receive = {

    case CheckMetrics() => if (System.currentTimeMillis >= nextReact) metricsProvider ! MetricsRequest(ActorProtocol.ACTION_CHECK_STORAGE_HINTS, "")

    case metrics: MetricsResponse[Tuple2[Long, Long]] if metrics.value.isDefined => {

      val totalHints = metrics.value.get._1
      val hintsInProgress = metrics.value.get._2

      val notificationData = Array(totalHints - math.max(0, previousValue(0)),  hintsInProgress, totalHints)

      if ((notificationData(0) > 0) || (notificationData(1) > 0)) {
        if (previousValue(0) == 0 || previousValue(0) == -1) {
          // total hints is cumulative (it is not the current number of hints, but the number of hints since the node startup)
          previousValue = Array(totalHints, hintsInProgress)
        } else {
          previousValue = Array(totalHints, hintsInProgress)
          react(notificationData)
        }
      }
      {}
    }
  }


  def react(info: Array[Long]): Unit = {
    val message = s"""Cassandra Node ${HostnameProvider.hostname} has some storage hints.
         |
         | At least '${info(0)}' hints since last check
         | Currently this node's replying '${info(1)}' hints (Total Hints since startup: ${info(2)}).
         |
         | Some nodes may be stopped (or there are network issues).
       """.stripMargin

    context.system.eventStream.publish(buildNotification(message))
    nextReact = System.currentTimeMillis + FREQUENCY
  }
}
