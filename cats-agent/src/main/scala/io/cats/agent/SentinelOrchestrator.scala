package io.cats.agent

import java.io.IOException
import java.lang.management.ManagementFactory
import java.rmi.ConnectException
import java.util.concurrent.TimeUnit

import akka.actor.{Props, Actor}
import akka.actor.Actor.Receive
import com.typesafe.config.ConfigFactory
import io.cats.agent.Constants._
import io.cats.agent.sentinels._
import scala.concurrent.duration
import scala.concurrent.duration._
import io.cats.agent.bean.Notification
import Messages._

import scala.concurrent.ExecutionContext.Implicits.global // TODO is ti really the best way to do this?

/**
 * The "SentinelOrchestrator" actor schedule the sentinels that analyze information provided by the JMX interface of the Cassandra Node.
 */
class SentinelOrchestrator extends Actor {

  val globalConfig = ConfigFactory.load()

  val configSentinel = globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_ORCHESTRATOR)
  val interval = configSentinel.getDuration(CONF_ORCHESTRATOR_INTERVAL)

  context.system.scheduler.schedule(1 second, Duration.create(interval.getSeconds, TimeUnit.SECONDS), self, CHECK_METRICS )

  val jmxClient = JmxClient(configSentinel.getString(CONF_ORCHESTRATOR_JMX_HOST), configSentinel.getInt(CONF_ORCHESTRATOR_JMX_PORT))
  val osMBean = ManagementFactory.getOperatingSystemMXBean()

  val mailNotif = context.actorOf(MailNotifier.props(), name ="mail-notifier")

  val sentinels = {
    Array(
      new LoadAverageSentinel(osMBean, mailNotif, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_LOADAVG)),
      new HintsSentinel(jmxClient, mailNotif, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_HINTS)),
      new DroppedCounterSentinel(jmxClient, mailNotif, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_DROPPED_COUNTER)),
      new DroppedMutationSentinel(jmxClient, mailNotif, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_DROPPED_MUTATION)),
      new DroppedReadSentinel(jmxClient, mailNotif, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_DROPPED_READ)),
      new DroppedReadRepairSentinel(jmxClient, mailNotif, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_DROPPED_READ_REPAIR)),
      new DroppedPageRangeSentinel(jmxClient, mailNotif, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_DROPPED_PAGE)),
      new DroppedRangeSliceSentinel(jmxClient, mailNotif, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_DROPPED_RANGE)),
      new DroppedRequestResponseSentinel(jmxClient, mailNotif, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_DROPPED_REQ_RESP)),
      new StorageSentinel(jmxClient, mailNotif, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_DISK_SPACE))
      // TODO ThreadPool stats (blocked & pending ??)
      // TODO StorageException
      // TODO GC stats
    ).filter(_.isEnabled)
  }

  override def receive = {
    case CHECK_METRICS => processCriticalControls
  }

  private def processCriticalControls: Unit = {
    try {
      println(CHECK_METRICS + " received");
      sentinels.foreach(_.analyzeAndReact())
    } catch {
      case ex: ConnectException => println("ERR : " + ex.getMessage)
      case ex: IOException => println("ERR : " + ex.getMessage)
    }
  }
}
