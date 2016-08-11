package io.argos.agent.sentinels

import java.lang.management.{ManagementFactory}

import com.typesafe.config.Config
import io.argos.agent.Constants
import io.argos.agent.bean.CheckMetrics
import io.argos.agent.util.HostnameProvider

import Constants._


class LoadAverageSentinel(override val conf: Config) extends Sentinel {
  private val osMBean = ManagementFactory.getOperatingSystemMXBean()

  private lazy val loadAvgThreshold = conf.getDouble(CONF_THRESHOLD)

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
