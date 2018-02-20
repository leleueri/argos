package io.argos.agent.workers

import javafx.scene.SnapshotResult

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging}
import com.typesafe.config.ConfigFactory
import io.argos.agent.AgentGatewayConfig
import io.argos.agent.Constants._
import io.argos.agent.bean.{SnapshotOrder, SnapshotProtocol, SnapshotStatus}
import io.argos.agent.util.JmxClient

/**
  * This actor manages the Snapshot process on a Keyspace/table.
  */
class SnapshotManager(conf: AgentGatewayConfig) extends Actor with ActorLogging {

  val jmxConfig = ConfigFactory.load().getConfig(CONF_OBJECT_ENTRY_METRICS)
  val hostname = jmxConfig.getString(CONF_ORCHESTRATOR_JMX_HOST)
  val port = jmxConfig.getInt(CONF_ORCHESTRATOR_JMX_PORT)
  val user = if (jmxConfig.hasPath(CONF_ORCHESTRATOR_JMX_USER)) Some(jmxConfig.getString(CONF_ORCHESTRATOR_JMX_USER)) else None
  val pwd = if (jmxConfig.hasPath(CONF_ORCHESTRATOR_JMX_PWD)) Some(jmxConfig.getString(CONF_ORCHESTRATOR_JMX_PWD)) else None


  JmxClient.initInstance(hostname, port, user, pwd)(context.system)
  val jmxClient = JmxClient.getInstance()

  override def receive: Receive = {
    case order : SnapshotOrder => {
      log.info("Snapshot order {} received from {}", order, sender)
      try {
        jmxClient.takeSnapshot(order.id, order.keyspace, order.table, order.flush)
        log.info("Snapshot order '{}' done", order.id)
        sender ! SnapshotStatus(order.id, conf.name, SnapshotProtocol.STATUS_OK)
      } catch {
        case e =>
          log.warning("Snapshot order '{}' failed : {}", order.id, e.getMessage, e)
          sender ! SnapshotStatus(order.id, conf.name, SnapshotProtocol.STATUS_KO, Option(e.getMessage).orElse(Some("Unknown Error")))
      }
    }
  }
}
