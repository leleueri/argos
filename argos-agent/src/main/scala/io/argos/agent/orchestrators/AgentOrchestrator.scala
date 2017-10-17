package io.argos.agent.orchestrators

import java.time.Duration

import akka.actor.{Actor, ActorLogging, ActorRef, Terminated}
import io.argos.agent.AgentOrchestratorConfig
import io.argos.agent.bean.{GatewayDescription, HeartBeat, Joining, Registered}

import scala.collection.mutable.Map



/**
  * Manage the interactions with each agent.
  */
class AgentOrchestrator(agentOrchestratorCfg: AgentOrchestratorConfig, requestTimeout: Duration) extends Actor with ActorLogging {

  val agents : Map[String, GatewayDescription] = Map()

  implicit val actorSystem = this.context.system
  new OrchestratorHttpHandler(self, agentOrchestratorCfg, requestTimeout)

  override def receive: Receive = {
    case Joining(description) => {
      log.info("JOINING received from '{}' named '{}'", description.endpoint, description.name)
      // keep reference of the agent gateway
      agents += (description.name -> description)
      // watch this agent to avoid useless message sending
      context.watch(description.endpoint)
      // ack the message
      sender() ! Registered()
      log.info("REGISTERED send to '{}'", description.endpoint)
    }
    case Terminated(ref) => {
      log.info("AgentGateway '{}' is dead, state of the Cassandra node is unknown", ref)
      switchGatewayState(ref)
    }
    case GetClusterStatus() => {
      log.info("HTTP Endpoint request cluster status" )
      sender() ! agents.values.map(_.toStatus()).toList
    }
    case HeartBeat(Some(description)) => {
      log.info("HeartBeat received with description : {}", description ) // TODO set to debug
      agents += (description.name -> description)
    }
  }

  private def switchGatewayState(ref: ActorRef) : Unit = {
    agents.values.find( _.endpoint.equals(ref)) match {
      case Some(desc) =>  agents += (desc.name -> desc.copy(gatewayUp = false))
    }
  }

}

case class GetClusterStatus()
