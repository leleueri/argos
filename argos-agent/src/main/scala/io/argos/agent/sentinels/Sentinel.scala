package io.argos.agent.sentinels

import akka.actor.{Actor, ActorLogging}
import io.argos.agent.SentinelConfiguration
import io.argos.agent.bean.Notification
import io.argos.agent.util.HostnameProvider
import io.argos.agent.bean.CheckMetrics

/**
 * The sentinel analyzes information provided by the JMX interface of the Cassandra Node.
 * If the received values go out configured thresholds, the sentinel will send notifications/alerts.
 */
abstract class Sentinel extends Actor with ActorLogging {

  var nextReact = System.currentTimeMillis

  def conf : SentinelConfiguration

  def isEnabled = conf.enabled

  override def receive: Receive = {
    case msg => if (isEnabled) {
      processProtocolElement(msg)
    } else {
      log.debug("Not enabled : {}", msg)
    }
  }

  def processProtocolElement: Receive

  protected def level() = conf.level

  protected def label() = conf.label

  protected def title() = s"[${level}][${label}][CASSANDRA] Sentinel '${self.path.name}' found something on '${HostnameProvider.hostname}'"

  protected def buildNotification(msg: String) : Notification = Notification(self.path.name, title, msg, level, label, HostnameProvider.hostname)

  protected def updateNextReact() : Unit = {
    nextReact =  System.currentTimeMillis + conf.frequency
  }

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    this.context.system.eventStream.subscribe(this.self, classOf[CheckMetrics])
  }
}
