package io.argos.agent.sentinels

import akka.actor.ActorRef
import io.argos.agent.bean._
import io.argos.agent.SentinelConfiguration
import io.argos.agent.util.HostnameProvider

/**
  * Created by eric on 08/10/16.
  */
class GCSentinel (val metricsProvider: ActorRef, val conf: SentinelConfiguration) extends Sentinel {

  override def processProtocolElement: Receive =  {

    case CheckMetrics() => if (System.currentTimeMillis >= nextReact) metricsProvider ! MetricsRequest(ActorProtocol.ACTION_CHECK_GC, "")
    case metrics: MetricsResponse[GCState] if metrics.value.isDefined => {

      val gcState = metrics.value.get

      if (log.isDebugEnabled) {
        log.debug("GCSentinel : since last check gc-count=<{}>, gc-total-time-elapsed=<{}>, gc-total-bytes-reclaimed=<{}>", gcState.count,  gcState.totalRealTimeElapsed, gcState.totalBytesReclaimed)
      }

      if ((gcState.gcMeanElapsedTime() >= conf.threshold || gcState.maxRealTimeElapsed >= conf.threshold) && System.currentTimeMillis >= nextReact) {
        react(gcState)
      }
    }
  }

  def react(info: GCState): Unit = {

    val message =
      s"""Cassandra Node ${HostnameProvider.hostname} may have GC issue.
         |
         |Since the last check GC ran ${info.count} times for a total time of ${info.totalRealTimeElapsed} ms and ${info.totalBytesReclaimed} reclaimed bytes.
         |ElapsedTime (ms) : mean = ${info.gcMeanElapsedTime()} / max = ${info.maxRealTimeElapsed}
         |
         |If this message is rise regularly, the Cassandra may need a GC tuning or the node is overloaded (hot spot ?)
         |
       """.stripMargin

    context.system.eventStream.publish(buildNotification(conf.messageHeader.map(h => h + " \n\n--####--\n\n" + message).getOrElse(message)))

    updateNextReact()

    { }
  }
}
