package io.cats.agent.sentinels

import java.lang.management.OperatingSystemMXBean
import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import com.typesafe.config.Config
import io.cats.agent.Constants._
import io.cats.agent.bean.Notification
import io.cats.agent.util.{JmxClient, HostnameProvider}

import scala.concurrent.duration.FiniteDuration
import scala.util.Try

class HintsSentinel(jmxAccess: JmxClient, handler: ActorRef, override val conf: Config) extends Sentinel[Long] {

  private var nextReact = System.currentTimeMillis
  private val FREQUENCY = Try(conf.getDuration(CONF_FREQUENCY, TimeUnit.MILLISECONDS)).getOrElse(FiniteDuration(5, TimeUnit.MINUTES).toMillis)

  override def analyze(): Option[Long] = {
    Some(jmxAccess.getStorageMetricTotalHints()).filter(_>0) // TODO improve the algorithm, we should also check the hints in progress and see how these two value evolve
  }

  override def react(info: Long): Unit = {

    val title = s"[${level}] [${label}] Cassandra Sentinel found something"
    val message =
      s"""Cassandra Node ${HostnameProvider.hostname} has some Hints and may fail to contact another node.
        |
        |StorageHints : ${info}
        |
        |A node may be DOWN or OverLoaded, check cluster status (using the following command : nodetool status)
      """.stripMargin

    handler ! Notification(title, message)

    nextReact = System.currentTimeMillis + FREQUENCY

    { }
  }
}
