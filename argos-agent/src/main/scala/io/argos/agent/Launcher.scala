package io.argos.agent


import akka.actor.{ActorRef, ActorSystem, Props}
import Constants._
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http
import com.typesafe.config.{Config, ConfigFactory}
import io.argos.agent.notifiers.NotifierProvider
import io.argos.agent.orchestrators.{AgentOrchestrator, OrchestratorHttpHandler, SentinelOrchestrator}
import io.argos.agent.util.CommonLoggerFactory
import io.argos.agent.workers.AgentGateway

// to convert the entrySet of globalConfig.getConfig(CONF_OBJECT_ENTRY_NOTIFIERS)
import collection.JavaConversions._



object Launcher extends App {

  implicit val system = ActorSystem(ACTOR_SYSTEM)

  val cfg: Config = ConfigFactory.load()

  startSentinelOrchestrator
  startAgentOrchestrator
  startAgentGateway
  startAllNotifiers



  // ---------------------------------------------------------------------------------
  // Start Sentinel orchestrator
  // ---------------------------------------------------------------------------------
  def startSentinelOrchestrator = {
    if (cfg.hasPath(CONF_OBJECT_ENTRY_SENTINEL) && cfg.hasPath(CONF_OBJECT_ENTRY_SENTINEL_ENABLE) && cfg.getBoolean(CONF_OBJECT_ENTRY_SENTINEL_ENABLE)) {
      CommonLoggerFactory.commonLogger.info(this, "Start Sentinel Orchestrator")
      system.actorOf(Props[SentinelOrchestrator], name = "SentinelOrchestrator")
    }
    {}
  }

  // ---------------------------------------------------------------------------------
  // Start agent orchestrator with the http endpoint
  // ---------------------------------------------------------------------------------
  def startAgentOrchestrator = {
    val orchestratorCfg = ConfigHelper.getAgentOrchestratorConfig(cfg)
    if (orchestratorCfg.enable) {
      CommonLoggerFactory.commonLogger.info(this, "Agent Orchestrator enabled")
      system.actorOf(Props(classOf[AgentOrchestrator], orchestratorCfg, cfg.getDuration("akka.http.server.request-timeout")), name = "AgentOrchestrator")
    } else {
      CommonLoggerFactory.commonLogger.info(this, "Agent Orchestrator disabled")
    }

    {}
  }

  // ---------------------------------------------------------------------------------
  // Start agent gateway
  // ---------------------------------------------------------------------------------
  def startAgentGateway = {
    val agentCfg = ConfigHelper.getAgentGatewayConfig(cfg)
    if (agentCfg.enable) {
      CommonLoggerFactory.commonLogger.info(this, "Agent Gateway enabled")
      system.actorOf(Props(classOf[AgentGateway], agentCfg), name = "AgentGateway")
    } else {
      CommonLoggerFactory.commonLogger.info(this, "Agent Gateway disabled")
    }

    {}
  }

  // ---------------------------------------------------------------------------------
  // Start alert notifiers according to the configuration
  // ---------------------------------------------------------------------------------
  def startAllNotifiers = {
    cfg.getConfig(CONF_OBJECT_ENTRY_NOTIFIERS).entrySet().toList.filter(_.getKey.matches("[^\\.]+\\." + CONF_PROVIDER_CLASS_KEY)).foreach(
        confValue => {
          val notifierId: String = confValue.getKey.substring(0, confValue.getKey.indexOf('.'))
          if (!cfg.hasPath(CONF_OBJECT_ENTRY_NOTIFIERS+"."+notifierId+"."+CONF_ENABLE) || cfg.getBoolean(CONF_OBJECT_ENTRY_NOTIFIERS+"."+notifierId+"."+CONF_ENABLE)) {
            try {
              val clazz: String = confValue.getValue.unwrapped().asInstanceOf[String]
              CommonLoggerFactory.commonLogger.info(this, "Initialize '{}' notifier with : '{}'", notifierId, clazz)
              val providerClass: Any = Class.forName(clazz).newInstance()
              system.actorOf(providerClass.asInstanceOf[NotifierProvider].props, notifierId.capitalize + "Notifier")
            } catch {
              case e: Exception =>
                CommonLoggerFactory.commonLogger.error(this, e, "Actor system will terminate, unable to initialize the '{}' notifier : '{}'.", confValue.getKey, e.getMessage)
                system.terminate()
            }
          } else {
            CommonLoggerFactory.commonLogger.info(this, s"Notifier ${notifierId} disabled")
          }
        }
    )
  }

}
