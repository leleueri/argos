package io.cats.agent.sentinels

import java.lang.management.{ManagementFactory, OperatingSystemMXBean}
import java.util.concurrent.TimeUnit

import com.typesafe.config.Config

import io.cats.agent.Constants._
import io.cats.agent.bean.CheckMetrics
import io.cats.agent.util.HostnameProvider

import scala.concurrent.duration.FiniteDuration
import scala.util.Try

class LoadAverageSentinel(override val conf: Config) extends Sentinel {
  private val osMBean = ManagementFactory.getOperatingSystemMXBean()

  private val loadAvgThreshold = conf.getDouble(CONF_THRESHOLD)
  private var nextReact = System.currentTimeMillis
  private val FREQUENCY = Try(conf.getDuration(CONF_FREQUENCY, TimeUnit.MILLISECONDS)).getOrElse(FiniteDuration(5, TimeUnit.MINUTES).toMillis)

  override def processProtocolElement: Receive = {
    case CheckMetrics() => analyze
  }

  def analyze() : Unit = {
    if (System.currentTimeMillis >= nextReact) {
      val loadAvg = osMBean.getSystemLoadAverage
      if (log.isDebugEnabled) {
        log.debug("LoadAvg=<{}>, threshold=<{}>", loadAvg.toString, loadAvgThreshold.toString)
      }
      if (loadAvg > loadAvgThreshold) {
        react(loadAvg, loadAvgThreshold)
      }
    }
  }

  def react(loadAvg: Double, threshold: Double): Unit = {
    val message =
      s"""Cassandra Node ${HostnameProvider.hostname} is overloaded.
                                                       |
                                                       |Current loadAvg : ${loadAvg}
          |Threshold : ${loadAvgThreshold}
          |
          |Something wrong may append on this node...
      """.stripMargin

    context.system.eventStream.publish(buildNotification(message))

    nextReact = System.currentTimeMillis + FREQUENCY

    { }
  }
}
