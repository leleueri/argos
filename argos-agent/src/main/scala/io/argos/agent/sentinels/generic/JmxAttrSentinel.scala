package io.argos.agent.sentinels.generic

import akka.actor.ActorRef
import com.typesafe.config.Config
import io.argos.agent.Constants
import io.argos.agent.Constants._
import io.argos.agent.bean._
import io.argos.agent.sentinels.Sentinel
import io.argos.agent.util.{HostnameProvider, WindowBuffer}

import scala.util.Try

/**
  * Created by eric on 16/11/16.
  */
class JmxAttrSentinel(val metricsProvider: ActorRef, override val conf: Config) extends Sentinel {
  val jmxName = conf.getString(Constants.CONF_JMX_NAME)
  val jmxAttr = conf.getString(Constants.CONF_JMX_ATTR)

  private val threshold = if (conf.hasPath(CONF_THRESHOLD)) conf.getDouble(CONF_THRESHOLD) else 0.0
  private val epsilon = if (conf.hasPath(CONF_EPSILON)) conf.getLong(CONF_EPSILON) else 0.01
  private val customMsg = if (conf.hasPath(CONF_CUSTOM_MSG)) Some(conf.getString(CONF_CUSTOM_MSG)) else None

  val BSIZE = Try(conf.getInt(CONF_WINDOW_SIZE)).getOrElse(1)
  val wBuffer = new WindowBuffer[JmxAttrValue](BSIZE, epsilon)
  val checkMean = Try(conf.getBoolean(CONF_WINDOW_MEAN)).getOrElse(false)

  // TODO create a Config Object

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
      if (checkMean && !wBuffer.meanUnderThreshold(threshold, extractValue)) {
        react(container)
      } else if (!wBuffer.underThreshold(threshold, extractValue)) {
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
         |Last value : '${container.value}'
         |
         |Window Size : '${BSIZE}'
         |Configured Threshold : '${threshold}'
         |Threshold On Mean : '${checkMean}'
         |
         |""".stripMargin

    context.system.eventStream.publish(buildNotification(customMsg.map( cm => cm + "\n\n" + messageValue)getOrElse(messageHeader + messageValue)))
    nextReact = System.currentTimeMillis + FREQUENCY
    wBuffer.clear()
  }

}