package io.cats.agent

import akka.actor.{Props, ActorSystem}
import Constants._
import akka.pattern.BackoffSupervisor
import io.cats.agent.util.HostnameProvider
import scala.concurrent.duration.FiniteDuration

/**
 * Created by eric on 26/01/16.
 */
object Launcher extends App {

  val system = ActorSystem(ACTOR_SYSTEM)

  // start the sentinel orchestrator
  //val sentinel = system.actorOf(Props[SentinelOrchestrator], name = s"sentinel-${HostnameProvider.hostname}")

  //  instead of a simple Orchestrator startup, we use the BackOffSupervisor in order to restart the
  // orchestrator in case of error (ex: cassandra node not available on agent startup)
  val supervisor = BackoffSupervisor.props(
      Props[SentinelOrchestrator],
      childName = s"sentinel-${HostnameProvider.hostname}",
      minBackoff = FiniteDuration(30, "seconds"),
      maxBackoff = FiniteDuration(60, "seconds"),
      randomFactor = 0.2 // adds 20% "noise" to vary the intervals slightly
    )
  system.actorOf(supervisor, name = "sentinelSupervisor")
}
