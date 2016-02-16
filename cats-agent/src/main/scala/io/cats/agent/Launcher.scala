package io.cats.agent

import akka.actor.{Props, ActorSystem}
import Constants._
import io.cats.agent.util.HostnameProvider

/**
 * Created by eric on 26/01/16.
 */
object Launcher extends App {

  val system = ActorSystem(ACTOR_SYSTEM)
  // default Actor constructor
  val sentinel = system.actorOf(Props[SentinelOrchestrator], name = s"sentinel-${HostnameProvider.hostname}")

}
