package io.argos.agent.sentinels.generic

import akka.actor.ActorRef
import io.argos.agent.SentinelConfiguration
import io.argos.agent.bean._
import io.argos.agent.sentinels.Sentinel
import io.argos.agent.util.{HostnameProvider, WindowBuffer}


/**
  * Created by eric on 16/11/16.
  */
class JmxAttrSentinel(val metricsProvider: ActorRef, override val conf: SentinelConfiguration) extends Sentinel {
  val jmxName = conf.objectName.get
  val jmxAttr = conf.objectAttr.get

  private val threshold = conf.threshold
  private val epsilon = conf.precision
  private val customMsg = conf.messageHeader

  val BSIZE = conf.windowSize
  val wBuffer = new WindowBuffer[JmxAttrValue](BSIZE, epsilon)
  val checkMean = conf.checkMean

  private def extractValue(entry: JmxAttrValue) : Double = entry.value

  override def processProtocolElement: Receive = {
    case CheckMetrics() => if (System.currentTimeMillis >= nextReact) {
      metricsProvider ! MetricsAttributeRequest(ActorProtocol.ACTION_CHECK_JMX_ATTR, jmxName, jmxAttr)
    }
    case metrics : MetricsResponse[JmxAttrValue] if (metrics.value.isDefined) => {
      val container = metrics.value.get

      wBuffer.push(container)

      if (log.isDebugEnabled) {
        log.debug("JmxAttrSentinel : Object=<{}>, Attribute=<{}> ==> <{}>", jmxName, jmxAttr, container.value)
      }
      if ((checkMean && !wBuffer.meanUnderThreshold(threshold, extractValue))
        || (!checkMean && !wBuffer.underThreshold(threshold, extractValue))) {
        react(container)
      }
    }
  }

  def react(container: JmxAttrValue): Unit = {

    val messageHeader =
      s"""Cassandra Node ${HostnameProvider.hostname} rises an alert about attribute '${jmxAttr}' on '${jmxName}'.
         |
       """.stripMargin

    val messageValue =
      s"""
         |--####--
         |
         |Last value : '${container.value}'
         |
         |Window Size : '${BSIZE}'
         |Configured Threshold : '${threshold}'
         |Threshold On Mean : '${checkMean}'
         |
         |--####--
         |""".stripMargin

    context.system.eventStream.publish(buildNotification(customMsg.map( cm => cm + "\n\n" + messageValue).getOrElse(messageHeader + messageValue)))
    updateNextReact()
    wBuffer.clear()
  }

}