package io.cats.agent.sentinels

import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import akka.event.EventStream
import com.typesafe.config.Config
import io.cats.agent.Constants._
import io.cats.agent.bean.{DroppedMessageStats, Notification}
import io.cats.agent.util.CommonLoggerFactory._
import io.cats.agent.util.{JmxClient, HostnameProvider}

import scala.concurrent.duration.FiniteDuration
import scala.util.Try

abstract class DroppedSentinel(jmxAccess: JmxClient, stream: EventStream, override val conf: Config) extends Sentinel[DroppedMessageStats] {

  private var nextReact = System.currentTimeMillis
  private val FREQUENCY = Try(conf.getDuration(CONF_FREQUENCY, TimeUnit.MILLISECONDS)).getOrElse(FiniteDuration(5, TimeUnit.MINUTES).toMillis)

  def getDroppedMessageStats : DroppedMessageStats

  override def analyze(): Option[DroppedMessageStats] = {
    val droppedMsg = getDroppedMessageStats
    if (sentinelLogger.isDebugEnabled) {
      sentinelLogger.debug(this, "DroppedSentinel : MessageType=<{}>, onMinRate=<{}>, totalDropped=<{}>", droppedMsg.`type`, droppedMsg.oneMinRate.toString, droppedMsg.count.toString)
    }
    if (droppedMsg.oneMinRate > 0.0 && System.currentTimeMillis >= nextReact) {
      Some(droppedMsg)
    } else {
      None
    }
  }

  override def react(info:  DroppedMessageStats): Unit = {

    val message =
      s"""Cassandra Node ${HostnameProvider.hostname} may be overloaded.
        |
        |There are some dropped messages for the Type '${info.`type`}'
        |
        |Last Minute Dropped messages : ${info.oneMinRate}
        |Last Five minutes Dropped messages : ${info.fiveMinRate}
        |Last Fifteen minutes Dropped messages : ${info.fifteenMinRate}
        |
        |Total since startup : ${info.count}
        |
        |Something wrong may append on this node...
      """.stripMargin

    stream.publish(buildNotification(message))

    nextReact = System.currentTimeMillis + FREQUENCY

    { }
  }
}
