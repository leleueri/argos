package io.cats.agent.sentinels

import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import com.typesafe.config.Config
import io.cats.agent.Constants._
import io.cats.agent.bean.{ThreadPoolStats, DroppedMessageStats, Notification}
import io.cats.agent.util.{JmxClient, HostnameProvider}

import scala.concurrent.duration.FiniteDuration
import scala.util.Try

abstract class BlockedSentinel(jmxAccess: JmxClient, handler: ActorRef, override val conf: Config) extends Sentinel[ThreadPoolStats] {

  private var nextReact = System.currentTimeMillis
  private val FREQUENCY = Try(conf.getDuration(CONF_FREQUENCY, TimeUnit.MILLISECONDS)).getOrElse(FiniteDuration(5, TimeUnit.MINUTES).toMillis)

  def getThreadPoolStats : ThreadPoolStats

  override def analyze(): Option[ThreadPoolStats] = {
    val treadPool = getThreadPoolStats
    if (treadPool.currentBlockedTasks > 0 && System.currentTimeMillis >= nextReact) {
        Some(treadPool)
    } else {
      None
    }
  }

  override def react(info:  ThreadPoolStats): Unit = {

    val message =
      s"""Cassandra Node ${HostnameProvider.hostname} may be overloaded.
        |
        |There are some blocked thread for the Type '${info.`type`}'
        |
        |Currently blocked tasks : ${info.currentBlockedTasks}
        |Pending tasks           : ${info.pendingTasks}
        |Active Tasks            : ${info.activeTasks}
        |Available executors     : ${info.maxPoolSize}
        |
        |Total blocked tasks since node startup : ${info.totalBlockedTasks}
        |
        |Something wrong may append on this node...
      """.stripMargin

    handler ! Notification(title, message)

    nextReact = System.currentTimeMillis + FREQUENCY

    { }
  }
}
