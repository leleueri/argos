package io.argos.agent

import akka.actor.{ActorSystem, Props}
import Constants._
import com.typesafe.config.{Config, ConfigFactory}
import io.argos.agent.notifiers.NotifierProvider
import io.argos.agent.orchestrators.{AgentOrchestrator, SentinelOrchestrator}
import io.argos.agent.util.CommonLoggerFactory
import io.argos.agent.workers.AgentGateway

// to convert the entrySet of globalConfig.getConfig(CONF_OBJECT_ENTRY_NOTIFIERS)
import collection.JavaConversions._

/**
 * Created by eric on 26/01/16.
 */
object Launcher extends App {

  val system = ActorSystem(ACTOR_SYSTEM)

  // Start all Notifiers
  private val cfg: Config = ConfigFactory.load()
  cfg.getConfig(CONF_OBJECT_ENTRY_NOTIFIERS).entrySet().toList.filter(_.getKey.matches("[^\\.]+\\." + CONF_PROVIDER_CLASS_KEY)).foreach(
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
  // TODO make SentinelOrchestrator optional ???
  CommonLoggerFactory.commonLogger.info(this, "Start Sentinel Orchestrator")
  system.actorOf(Props[SentinelOrchestrator], name = "SentinelOrchestrator")

  // start the agent orchestrator only if useful
  val orchestratorCfg = ConfigHelper.getAgentOrchestratorConfig(cfg)
  if (orchestratorCfg.enable) {
    CommonLoggerFactory.commonLogger.info(this, "Agent Orchestrator enabled")
    system.actorOf(Props(classOf[AgentOrchestrator], orchestratorCfg), name = "AgentOrchestrator")
  } else {
    CommonLoggerFactory.commonLogger.info(this, "Agent Orchestrator disabled")
  }

  // start the agent gateway
  val agentCfg = ConfigHelper.getAgentGatewayConfig(cfg)
  if (agentCfg.enable) {
    CommonLoggerFactory.commonLogger.info(this, "Agent Gateway enabled")
    system.actorOf(Props(classOf[AgentGateway], agentCfg), name = "AgentGateway")
  } else {
    CommonLoggerFactory.commonLogger.info(this, "Agent Gateway disabled")
  }

}
