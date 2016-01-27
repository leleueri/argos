package io.cats.agent

import java.io.IOException
import java.lang.management.ManagementFactory
import java.rmi.ConnectException

import akka.actor.Actor
import akka.actor.Actor.Receive
import scala.concurrent.duration
import scala.concurrent.duration._
import Messages._

import scala.concurrent.ExecutionContext.Implicits.global // TODO is ti really the best way to do this?

/**
 * The "sentinel" actor analyzes information provided by the JMX interface of the Cassandra Node.
 * If the received values go out configured thresholds, the sentinel will send notifications/alerts.
 *
 * The sentinel checks the cassandra information regularly.
 */

// TODO rendre la fréquence des appels JMX configurable ?

class Sentinel extends Actor {

  context.system.scheduler.schedule(1 second, 1 second, self, CHECK_CRITICAL_METRICS )// TODO la frequence doit être la meme. pas de notion de Niveau d'importance ici ?
  context.system.scheduler.schedule(1 second, 5 second, self, CHECK_WARNING_METRICS )
  context.system.scheduler.schedule(1 second, 15 second, self, CHECK_INFORMATIVE_METRICS )

  val ce = CatsEyes("127.0.0.1", 7199)

  val osMBean = ManagementFactory.getOperatingSystemMXBean()

  override def receive = {
    case CHECK_CRITICAL_METRICS => processCriticalControls
    case CHECK_WARNING_METRICS => processWarningControl
    case CHECK_INFORMATIVE_METRICS => processInformativeControl
  }

  private def processInformativeControl: Unit = {
    try {
      println(CHECK_INFORMATIVE_METRICS + " received");
      println(ce.readStorageLoad())
    } catch {
      case ex: IOException => println("ERR")
      case ex: ConnectException => println("ERR")
    }
  }

  private def processWarningControl: Unit = {
    try {
      println(CHECK_WARNING_METRICS + " received");
      println(ce.getMutationStageValues().currentBlockedTasks)
    } catch {
      case ex: ConnectException => println("ERR")
      case ex: IOException => println("ERR")
    }
  }

  private def processCriticalControls: Unit = {
    try {
     println(CHECK_CRITICAL_METRICS + " received");
     println(osMBean.getSystemLoadAverage)
    } catch {
      case ex: ConnectException => println("ERR")
      case ex: IOException => println("ERR")
    }
  }

}
