package io.cats.agent.sentinels

import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import akka.event.EventStream
import com.typesafe.config.Config
import io.cats.agent.Constants._
import io.cats.agent.bean._
import io.cats.agent.util.HostnameProvider
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

abstract class BlockedSentinel(val metricsProvider: ActorRef, val conf: Config) extends Sentinel {

  private var nextReact = System.currentTimeMillis
  private val FREQUENCY = Try(conf.getDuration(CONF_FREQUENCY, TimeUnit.MILLISECONDS)).getOrElse(FiniteDuration(5, TimeUnit.MINUTES).toMillis)

  def getThreadPoolStats : MetricsRequest

  override def processProtocolElement: Receive = {

    case CheckMetrics() => if (System.currentTimeMillis >= nextReact) metricsProvider ! getThreadPoolStats
    case metrics: MetricsResponse[ThreadPoolStats] if metrics.value.isDefined => {

      val treadPool = metrics.value.get

      if (log.isDebugEnabled) {
        log.debug("BlockedSentinel : ThreadPool=<{}>, currentlyBlockedTasks=<{}>", treadPool.`type`, treadPool.currentBlockedTasks.toString)
      }

      if (treadPool.currentBlockedTasks > 0 && System.currentTimeMillis >= nextReact) {
        Some(treadPool)
      } else {
        None
      }
    }
  }

  def react(info:  ThreadPoolStats): Unit = {

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

    context.system.eventStream.publish(buildNotification(message))

    nextReact = System.currentTimeMillis + FREQUENCY

    { }
  }
}
