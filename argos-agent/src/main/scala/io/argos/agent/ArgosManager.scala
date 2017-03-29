package io.argos.agent

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging, Props}
import com.typesafe.config.Config
import io.argos.agent.orchestrators.{AgentOrchestrator, SentinelOrchestrator}
import io.argos.agent.util.CommonLoggerFactory
import io.argos.agent.workers.AgentGateway
import Constants._


class ArgosManager(cfg: Config) extends Actor with ActorLogging {

  startSentinelOrchestrator
  startAgentOrchestrator
  startAgentGateway

  def startSentinelOrchestrator = {
    if (cfg.hasPath(CONF_OBJECT_ENTRY_SENTINEL) && cfg.hasPath(CONF_OBJECT_ENTRY_SENTINEL_ENABLE) && cfg.getBoolean(CONF_OBJECT_ENTRY_SENTINEL_ENABLE)) {
      context.actorOf(Props[SentinelOrchestrator], name = "SentinelOrchestrator")
      CommonLoggerFactory.commonLogger.info(this, "Start Sentinel Orchestrator")
    }

    {}
  }

  def startAgentOrchestrator = {
    val orchestratorCfg = ConfigHelper.getAgentOrchestratorConfig(cfg)
    if (orchestratorCfg.enable) {
      CommonLoggerFactory.commonLogger.info(this, "Agent Orchestrator enabled")
      context.actorOf(Props(classOf[AgentOrchestrator], orchestratorCfg), name = "AgentOrchestrator")
    } else {
      CommonLoggerFactory.commonLogger.info(this, "Agent Orchestrator disabled")
    }

    {}
  }

  def startAgentGateway = {
    val agentCfg = ConfigHelper.getAgentGatewayConfig(cfg)
    if (agentCfg.enable) {
      CommonLoggerFactory.commonLogger.info(this, "Agent Gateway enabled")
      context.actorOf(Props(classOf[AgentGateway], agentCfg), name = "AgentGateway")
    } else {
      CommonLoggerFactory.commonLogger.info(this, "Agent Gateway disabled")
    }

    {}
  }


  override def receive = ???
}
