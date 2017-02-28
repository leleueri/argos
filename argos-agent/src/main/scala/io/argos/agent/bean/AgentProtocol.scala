package io.argos.agent.bean

import akka.actor.ActorRef



case class GatewayDescription(name: String, endpoint: ActorRef, up: Boolean = true, loadAvg: Double = 0.0)


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