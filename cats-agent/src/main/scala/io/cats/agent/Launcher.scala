package io.cats.agent

import akka.actor.{Props, ActorSystem}
import Constants._

/**
 * Created by eric on 26/01/16.
 */
object Launcher extends App {

  val system = ActorSystem(ACTOR_SYSTEM)
  // default Actor constructor
  val sentinel = system.actorOf(Props[Sentinel], name = "sentinel-127.0.0.1") // TODO rendre dynamique la definition de l'IP dans le nom... (prendre le hostname)
  
}
