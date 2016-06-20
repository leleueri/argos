package io.cats.agent.sentinels

import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import akka.event.EventStream
import com.typesafe.config.Config
import io.cats.agent.Constants._
import io.cats.agent.bean.Notification
import io.cats.agent.util.{JmxClient, HostnameProvider}

import scala.concurrent.duration.FiniteDuration
import scala.util.Try

class StorageHintsSentinel(jmxAccess: JmxClient, stream: EventStream, override val conf: Config) extends Sentinel[Array[Long]] {

  private var nextReact = System.currentTimeMillis
  private var previousValue : Array[Long] = Array(0,0)
  private val FREQUENCY = Try(conf.getDuration(CONF_FREQUENCY, TimeUnit.MILLISECONDS)).getOrElse(FiniteDuration(5, TimeUnit.MINUTES).toMillis)

  override def analyze(): Option[Array[Long]] = {
    val totalHints = jmxAccess.getStorageMetricTotalHints()
    val hintsInProgress = jmxAccess.getStorageMetricTotalHintsInProgess()

    val notificationData = Array(totalHints - previousValue(0), hintsInProgress, totalHints)

    if ( ((notificationData(0) > 0) || (notificationData(1) > 0)) && (System.currentTimeMillis >= nextReact)) {
      if(previousValue(0) == 0) {
        // total hints is cumulative (it is not the current number of hints, but the number of hints since the node startup)
        previousValue = Array(totalHints, hintsInProgress)
        None
      } else {
        previousValue = Array(totalHints, hintsInProgress)
        Some(notificationData)
      }
    } else {
      None
    }
  }

  override def react(info: Array[Long]): Unit = {
    val message = s"""Cassandra Node ${HostnameProvider.hostname} has some storage hints.
         |
         | At least '${info(0)}' hints since last check
         | Currently this node's replying '${info(1)}' hints (Total Hints since startup: ${info(2)}).
         |
         | Some nodes may be stopped (or there are network issues).
       """.stripMargin

    stream.publish(buildNotification(message))
    nextReact = System.currentTimeMillis + FREQUENCY
  }
}
