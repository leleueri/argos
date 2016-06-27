package io.cats.agent.sentinels

import akka.actor.Actor.Receive
import akka.actor.{ActorLogging, Actor}
import com.typesafe.config.Config
import io.cats.agent.Constants
import io.cats.agent.bean.{CheckMetrics, Notification}
import io.cats.agent.util.HostnameProvider

/**
 * The sentinel analyzes information provided by the JMX interface of the Cassandra Node.
 * If the received values go out configured thresholds, the sentinel will send notifications/alerts.
 */
abstract class Sentinel extends Actor with ActorLogging {

  def conf : Config

  def isEnabled = conf.getBoolean(Constants.CONF_ENABLED)

  override def receive: Receive = {
    case msg => if (isEnabled) {
      processProtocolElement(msg)
    } else {
      log.debug("Not enabled : {}", msg)
    }
  }

  def processProtocolElement: Receive

  protected def level() = conf.getString(Constants.CONF_LEVEL)

  protected def label() = conf.getString(Constants.CONF_LABEL)

  protected def title() = s"[${level}] [${label}] Cassandra Sentinel found something"

  protected def buildNotification(msg: String) : Notification = Notification(title, msg, level, label, HostnameProvider.hostname)

}
