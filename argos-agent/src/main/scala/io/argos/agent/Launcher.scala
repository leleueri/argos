package io.argos.agent

import akka.actor.{ActorPath, Props, ActorSystem}
import Constants._
import akka.pattern.BackoffSupervisor
import com.typesafe.config.ConfigFactory
import io.argos.agent.notifiers.NotifierProvider
import io.argos.agent.util.CommonLoggerFactory
import io.argos.agent.workers.MetricsProvider
import io.argos.agent.sentinels._

// to convert the entrySet of globalConfig.getConfig(CONF_OBJECT_ENTRY_NOTIFIERS)
import collection.JavaConversions._

/**
 * Created by eric on 26/01/16.
 */
object Launcher extends App {

  val system = ActorSystem(ACTOR_SYSTEM)

  // Start all Notifiers
  ConfigFactory.load().getConfig(CONF_OBJECT_ENTRY_NOTIFIERS).entrySet().toList.filter(_.getKey.matches("[^\\.]+\\." + CONF_PROVIDER_CLASS_KEY)).foreach(
    confValue => {
      try {
        val clazz: String = confValue.getValue.unwrapped().asInstanceOf[String]
        CommonLoggerFactory.commonLogger.info(this, "Initialize '{}' notifier", clazz)
        val providerClass: Any = Class.forName(clazz).newInstance()
        system.actorOf(providerClass.asInstanceOf[NotifierProvider].props, confValue.getKey.substring(0, confValue.getKey.indexOf('.')).capitalize+"Notifier")
      } catch {
        case e: Exception =>
          CommonLoggerFactory.commonLogger.error(this, e, "Actor system will terminate, unable to initialize the '{}' notifier : '{}'.", confValue.getKey, e.getMessage)
          system.terminate()
      }
    }
  )

  // start the Sentinel Orchestrator (this actor launches MetricsProviders, sentinels... and schedule the sentinel processing)
  system.actorOf(Props[SentinelOrchestrator], name = "SentinelOrchestrator")

   /*
    //system.actorOf(Props[SnapshotExecutor], name = "snapshot-executor")
    system.actorSelection("akka.tcp://Cats@127.0.0.2:2552/user/snapshot-executor") ! SnapshotCmd("TEST ELE 1 ")
    system.actorSelection("akka.tcp://Cats@127.0.0.1:2552/user/snapshot-executor") ! SnapshotCmd("TEST ELE 2 ")*/
}
