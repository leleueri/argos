package io.argos.agent.orchestrators

import akka.actor.{Actor, ActorLogging}

class AgentOrchestrator extends Actor with ActorLogging {

  override def receive: Receive = {
    case unknown => log.error("Unknown message received by Agent orchestrator : {}", unknown)
  }

}
