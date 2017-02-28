package io.argos.agent.workers

import java.util.concurrent.{Executors, TimeUnit}

import akka.actor.{Actor, ActorLogging, ActorRef, Terminated}
import io.argos.agent.Constants._
import io.argos.agent.AgentGatewayConfig
import io.argos.agent.bean.{GatewayDescription, Joining, Registered, RequestJoining}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/**
  * This actor receives all requests from the AgentOrchestrator and executes them.
  */
class AgentGateway(conf: AgentGatewayConfig) extends Actor with ActorLogging {
  implicit val execContext = (ExecutionContext.fromExecutor(Executors.newSingleThreadScheduledExecutor()))
  val agentOrchetratorRef = context.actorSelection(s"akka.tcp://${ACTOR_SYSTEM}@${conf.orchestratorHost}:${conf.orchestratorPort}/user/AgentOrchestrator")

  var cancelRequestJoining = context.system.scheduler.schedule(
    FiniteDuration.apply(0, TimeUnit.MILLISECONDS),
    FiniteDuration.apply(1, TimeUnit.MINUTES),
    self, RequestJoining())

  override def receive: Receive = {
    case RequestJoining() => {
      log.info("Try to join AgentOrchestrator...")
      agentOrchetratorRef ! Joining(GatewayDescription(conf.name, self))
    }

    case Registered() => {
      val orchestrator: ActorRef = sender()
      log.info("Welcome received from AgentOrchestrator '{}'", orchestrator)
      // watch the orchestrator to reinitialize the Join process
      context.watch(orchestrator)
      // stop joining request
      cancelRequestJoining.cancel()
      // switch to nominal state
      context.become(nominal)
    }
  }

  def nominal: Receive = {
    case Terminated(ref) => {
      // orchestrator is unreachable, schedule joining request
      if (cancelRequestJoining.isCancelled) {
        cancelRequestJoining = context.system.scheduler.schedule(
          FiniteDuration.apply(5, TimeUnit.MINUTES),
          FiniteDuration.apply(1, TimeUnit.MINUTES),
          self, RequestJoining())
      }
      // switch to joining state
      context.unbecome()
    }
  }
}