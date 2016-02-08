package io.cats.agent.sentinels

import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import com.typesafe.config.Config
import io.cats.agent.Constants._
import io.cats.agent.bean.{DroppedMessageStats, Notification}
import io.cats.agent.{HostnameProvider, JmxClient}

import scala.concurrent.duration.FiniteDuration
import scala.util.Try

abstract class DroppedSentinel(jmxAccess: JmxClient, handler: ActorRef, override val conf: Config) extends Sentinel[DroppedMessageStats] {

  private var nextReact = System.currentTimeMillis
  private val FREQUENCY = Try(conf.getDuration(CONF_FREQUENCY, TimeUnit.MILLISECONDS)).getOrElse(FiniteDuration(5, TimeUnit.MINUTES).toMillis)

  def getDroppedMessageStats : DroppedMessageStats

  override def analyze(): Option[DroppedMessageStats] = {
    val droppedMsg = getDroppedMessageStats
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

    handler ! Notification(title, message)

    nextReact = System.currentTimeMillis + FREQUENCY

    { }
  }
}
