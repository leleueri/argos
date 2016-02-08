package io.cats.agent.sentinels

import java.lang.management.OperatingSystemMXBean
import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import com.typesafe.config.Config

import io.cats.agent.Constants._
import io.cats.agent.HostnameProvider
import io.cats.agent.bean.Notification

import scala.concurrent.duration.FiniteDuration
import scala.util.Try

class LoadAverageSentinel(jmxAccess: OperatingSystemMXBean, handler: ActorRef, override val conf: Config) extends Sentinel[Array[Double]] {

  private val loadAvgThreshold = conf.getDouble(CONF_THRESHOLD)
  private var nextReact = System.currentTimeMillis
  private val FREQUENCY = Try(conf.getDuration(CONF_FREQUENCY, TimeUnit.MILLISECONDS)).getOrElse(FiniteDuration(5, TimeUnit.MINUTES).toMillis)

  override def analyze(): Option[Array[Double]] = {
    val loadAvg = jmxAccess.getSystemLoadAverage
    if (loadAvg > loadAvgThreshold) {
      if (System.currentTimeMillis >= nextReact) { // TODO improve this to avoid flooding. for the moment one notification every 5 minutes...
        Some(Array(loadAvg,loadAvgThreshold))
      } else None
    } else {
      None
    }
  }

  override def react(info: Array[Double]): Unit = {

    val message =
      s"""Cassandra Node ${HostnameProvider.hostname} is overloaded.
        |
        |Current loadAvg : ${info(0)}
        |Threshold : ${info(1)}
        |
        |Something wrong may append on this node...
      """.stripMargin

    handler ! Notification(title, message)

    nextReact = System.currentTimeMillis + FREQUENCY

    { }
  }
}