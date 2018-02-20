package io.argos.agent.workers

import java.util.concurrent.{Executors, TimeUnit}

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import io.argos.agent.Constants._
import io.argos.agent.{AgentGatewayConfig, Messages}
import io.argos.agent.bean._
import io.argos.agent.orchestrators.AgentOrchestrator
import io.argos.agent.util.OSBeanAccessor

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/**
  * This actor receives all requests from the AgentOrchestrator and executes them.
  */
class AgentGateway(conf: AgentGatewayConfig) extends Actor with ActorLogging {
  implicit val execContext = (ExecutionContext.fromExecutor(Executors.newSingleThreadScheduledExecutor()))

  val orchestratorRef = context.actorSelection(s"akka.tcp://${ACTOR_SYSTEM}@${conf.orchestratorHost}:${conf.orchestratorPort}/user/AgentOrchestrator")

  val joiningDelay = FiniteDuration.apply(5, TimeUnit.MINUTES)
  val initialJoiningDelay = FiniteDuration.apply(1, TimeUnit.SECONDS)
  val loadAvgDelay = FiniteDuration.apply(5, TimeUnit.SECONDS)

  // scheduler to generate frequent actions
  // this scheduler is initialized with the RequestJoining in order
  // to join the AgentOrchestrator.
  var scheduler = scheduleRequestJoining(initialJoiningDelay)

  var online = true

  val snapshotMng = context.actorOf(Props(classOf[SnapshotManager], conf), name = "SnapshotManager")

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    this.context.system.eventStream.subscribe(this.self, classOf[NodeStatus])
  }

  override def receive: Receive = {
    case RequestJoining() => {
      log.info("Try to join AgentOrchestrator...")
      orchestratorRef ! Joining(GatewayDescription(conf.name, self))
    }

    case Registered() => {
      val orchestrator: ActorRef = sender()
      log.info("Welcome received from AgentOrchestrator '{}'", orchestrator)
      // watch the orchestrator to reinitialize the Join process
      context.watch(orchestrator)
      // stop joining request
      scheduler.cancel()
      scheduler = scheduleLoadInfo(loadAvgDelay)
      // switch to nominal state
      log.info("AgentGateway is now in nominal state")
      context.become(nominal)
    }
    case NodeStatus(Messages.ONLINE_NODE) => {
      log.debug("Node is ONLINE")
      online = true
    }
    case NodeStatus(Messages.OFFLINE_NODE) => {
      log.debug("Node is OFFLINE")
      online = false
    }
  }

  def nominal: Receive = {
    case HeartBeat(_) => {
      log.debug("Send HeartBeat to Orchestrator...")
      orchestratorRef ! HeartBeat(Some(GatewayDescription(conf.name, self, online, OSBeanAccessor.loadAvg)))
    }
    case Terminated(ref) => {
      // orchestrator is unreachable, schedule joining request
      scheduler.cancel()
      scheduler = scheduleRequestJoining(joiningDelay)
      // switch to joining state
      context.unbecome()
    }
    case NodeStatus(Messages.ONLINE_NODE) => {
      log.debug("Node is ONLINE")
      online = true
    }
    case NodeStatus(Messages.OFFLINE_NODE) => {
      log.debug("Node is OFFLINE")
      online = false
    }
    case order : SnapshotOrder => {
      log.debug("Snapshot order received: {}", order)
      if (online) {
        snapshotMng ! order
      } else {
        orchestratorRef ! SnapshotStatus(order.id, conf.name, SnapshotProtocol.STATUS_KO, Some(s"${conf.name} is offline"))
      }
    }
    case orderStatus : SnapshotStatus => {
      log.debug("Status for Snapshot order received: {}", orderStatus)
      orchestratorRef ! orderStatus
    }
  }

  private def scheduleRequestJoining(delay : FiniteDuration) = {
    context.system.scheduler.schedule(
      FiniteDuration.apply(1, TimeUnit.MINUTES),
      delay,
      self, RequestJoining())
  }

  private def scheduleLoadInfo(delay : FiniteDuration) = {
    context.system.scheduler.schedule(
      FiniteDuration.apply(1, TimeUnit.MINUTES),
      delay,
      self, HeartBeat())
  }

}