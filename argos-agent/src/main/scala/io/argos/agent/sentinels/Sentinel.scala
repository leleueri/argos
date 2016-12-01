package io.argos.agent.sentinels

import java.util.concurrent.TimeUnit

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging}
import com.typesafe.config.Config
import io.argos.agent.{Constants, SentinelConfiguration}
import io.argos.agent.Constants._
import io.argos.agent.bean.Notification
import io.argos.agent.util.HostnameProvider
import io.argos.agent.bean.CheckMetrics

import scala.concurrent.duration.FiniteDuration
import scala.util.Try

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

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    this.context.system.eventStream.subscribe(this.self, classOf[CheckMetrics])
  }
}
