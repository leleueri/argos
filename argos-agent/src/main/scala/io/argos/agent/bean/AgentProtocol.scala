package io.argos.agent.bean

import akka.actor.ActorRef



case class GatewayDescription(name: String, endpoint: ActorRef, up: Boolean = true, loadAvg: Double = 0.0) {
  def toStatus() = GatewayStatus(name, up, loadAvg)
}

case class GatewayStatus(name: String, up: Boolean = true, loadAvg: Double = 0.0)

/**
  * Send by the AgentGateway to itself in order to schedule
  * the transmission of the GatewayDescription to the orchestrator
  * in order to inform it about the LoadAvg of the host
  */
case class HeartBeat (description: Option[GatewayDescription] = None)
/**
  * Send by the AgentGateway to itself in order to schedule
  * a Joining message to the Orchestrator in case of disconnection
  */
case class RequestJoining()

/**
  * Send by the AgentGateway to join the AgentOrchestrator
  */
case class Joining(description: GatewayDescription)

/**
  * Send by the AgentOrchestrator to the AgentGateway
  * to confirm the registration
  */
case class Registered()

case class LoadAvegrageInfo(value: Double)