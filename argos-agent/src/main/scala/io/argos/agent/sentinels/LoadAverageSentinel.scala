package io.argos.agent.sentinels

import java.lang.management.ManagementFactory

import io.argos.agent.SentinelConfiguration
import io.argos.agent.bean.CheckMetrics
import io.argos.agent.util.{HostnameProvider, WindowBuffer}


class LoadAverageSentinel(override val conf: SentinelConfiguration) extends Sentinel {
  private val osMBean = ManagementFactory.getOperatingSystemMXBean()

  private lazy val threshold = conf.threshold
  val wBuffer = new WindowBuffer[Double](conf.windowSize)

  override def processProtocolElement: Receive = {
    case CheckMetrics() => analyze
  }

  def analyze() : Unit = {
    if (System.currentTimeMillis >= nextReact) {
      val loadAvg = osMBean.getSystemLoadAverage
      wBuffer.push(loadAvg)

      if (log.isDebugEnabled) {
        log.debug("LoadAvg=<{}>, threshold=<{}>", loadAvg.toString, threshold.toString)
      }

      if ( (conf.checkMean && !wBuffer.meanUnderThreshold(threshold, (x) => x))
        || (!conf.checkMean && !wBuffer.underThreshold(threshold, (x) => x))) {
        react(loadAvg)
      }
    }
  }

  def react(loadAvg: Double): Unit = {

    val message =
      s"""Cassandra Node ${HostnameProvider.hostname} is overloaded.
         |
         |Current loadAvg : ${loadAvg}
         |Threshold : ${threshold}
         |
         |Something wrong may append on this node...""".stripMargin

    context.system.eventStream.publish(buildNotification(conf.messageHeader.map(h => h + " \n\n--####--\n\n" + message).getOrElse(message)))

    updateNextReact()

    { }
  }
}
