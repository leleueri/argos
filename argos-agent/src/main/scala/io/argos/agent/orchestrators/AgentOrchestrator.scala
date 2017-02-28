package io.argos.agent.orchestrators

import akka.actor.{Actor, ActorLogging, ActorRef, Terminated}
import io.argos.agent.AgentOrchestratorConfig
import io.argos.agent.bean.{GatewayDescription, Joining, Registered}

import scala.collection.mutable.Map


/**
  * Manage the interactions with each agent.
  */
class AgentOrchestrator(agentOrchestratorCfg: AgentOrchestratorConfig) extends Actor with ActorLogging {

  val agents : Map[ActorRef, GatewayDescription] = Map()

  override def receive: Receive = {
    case Joining(description) => {
      log.info("JOINING received from '{}' named '{}'", description.endpoint, description.name)
      // keep reference of the agent gateway
      agents += (description.endpoint -> description)
      // watch this agent to avoid useless message sending
      context.watch(description.endpoint)
      // ack the message
      sender() ! Registered()
      log.info("REGISTERED send to '{}'", description.endpoint)
    }
    case Terminated(ref) => {
      log.info("AgentGateway '{}' is dead", ref)
      agents.remove(ref)
    }
  }

}
