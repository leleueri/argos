package io.argos.agent

import akka.actor.{ActorSystem, Props}
import Constants._
import com.typesafe.config.ConfigFactory
import io.argos.agent.notifiers.NotifierProvider
import io.argos.agent.orchestrators.SentinelOrchestrator
import io.argos.agent.util.CommonLoggerFactory

// to convert the entrySet of globalConfig.getConfig(CONF_OBJECT_ENTRY_NOTIFIERS)
import collection.JavaConversions._

/**
 * Created by eric on 26/01/16.
 */
object Launcher extends App {

  val system = ActorSystem(ACTOR_SYSTEM)

  // TODO manage process parameter to start Sentinel or/and Agent orchestrator ... ???

  // Start all Notifiers
  ConfigFactory.load().getConfig(CONF_OBJECT_ENTRY_NOTIFIERS).entrySet().toList.filter(_.getKey.matches("[^\\.]+\\." + CONF_PROVIDER_CLASS_KEY)).foreach(
    confValue => {
      try {
        val clazz: String = confValue.getValue.unwrapped().asInstanceOf[String]
        CommonLoggerFactory.commonLogger.info(this, "Initialize '{}' notifier", clazz)
        val providerClass: Any = Class.forName(clazz).newInstance()
        system.actorOf(providerClass.asInstanceOf[NotifierProvider].props, confValue.getKey.substring(0, confValue.getKey.indexOf('.')).capitalize+"Notifier")
      } catch {
        case e: Exception =>
          CommonLoggerFactory.commonLogger.error(this, e, "Actor system will terminate, unable to initialize the '{}' notifier : '{}'.", confValue.getKey, e.getMessage)
          system.terminate()
      }
    }
  )

  // start the Sentinel Orchestrator (this actor launches MetricsProviders, sentinels... and schedule the sentinel processing)
  system.actorOf(Props[SentinelOrchestrator], name = "SentinelOrchestrator")

  // start the agent orchestrator only if the
  system.actorOf(Props[SentinelOrchestrator], name = "AgentOrchestrator")

}
