package io.cats.agent

import java.io.IOException
import java.lang.management.ManagementFactory
import java.rmi.ConnectException
import java.util.concurrent.{Executors, Executor, TimeUnit}

import akka.actor.{ActorLogging, Props, Actor}
import akka.actor.Actor.Receive
import com.typesafe.config.ConfigFactory
import io.cats.agent.Constants._
import io.cats.agent.sentinels._
import io.cats.agent.util.{HostnameProvider, JmxClient}
import scala.concurrent.{ExecutionContext, duration}
import scala.concurrent.duration._
import io.cats.agent.bean.Notification
import Messages._


/**
 * The "SentinelOrchestrator" actor schedule the sentinels that analyze information provided by the JMX interface of the Cassandra Node.
 */
class SentinelOrchestrator extends Actor with ActorLogging {

  val globalConfig = ConfigFactory.load()

  val configSentinel = globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_ORCHESTRATOR)
  val interval = configSentinel.getDuration(CONF_ORCHESTRATOR_INTERVAL)

  context.system.scheduler.schedule(1 second, Duration.create(interval.getSeconds, TimeUnit.SECONDS), self, CHECK_METRICS )(ExecutionContext.fromExecutor(Executors.newSingleThreadScheduledExecutor()))

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

      new StorageSentinel(jmxClient, mailNotif, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_STORAGE_SPACE)),
      new StorageHintsSentinel(jmxClient, mailNotif, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_STORAGE_HINTS)),
      new StorageExceptionSentinel(jmxClient, mailNotif, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_STORAGE_EXCEPTION)),

      new CounterMutationBlockedSentinel(jmxClient, mailNotif, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_STAGE_COUNTER_MUTATION)),
      new GossipBlockedSentinel(jmxClient, mailNotif, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_STAGE_GOSSIP)),
      new InternalResponseBlockedSentinel(jmxClient, mailNotif, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_STAGE_INTERNAL)),
      new MemtableFlusherBlockedSentinel(jmxClient, mailNotif, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_STAGE_MEMTABLE)),
      new MutationBlockedSentinel(jmxClient, mailNotif, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_STAGE_MUTATION)),
      new ReadBlockedSentinel(jmxClient, mailNotif, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_STAGE_READ)),
      new ReadRepairBlockedSentinel(jmxClient, mailNotif, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_STAGE_READ_REPAIR)),
      new RequestResponseBlockedSentinel(jmxClient, mailNotif, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_STAGE_REQUEST_RESPONSE))

    ).filter(_.isEnabled)
  }

  override def receive = {
    case CHECK_METRICS => processControls
  }

  def offline : Receive = {
    case CHECK_METRICS => tryToProcessControls
  }

  private def processControls: Unit = {
    try {
      log.debug("{} received", CHECK_METRICS);
      sentinels.foreach(_.analyzeAndReact())
    } catch {
      case ex: ConnectException =>
        log.warning("Connection error : {}", ex.getMessage, ex);
        mailNotif ! Notification(s"[CRITIC] Cassandra node ${HostnameProvider.hostname} is DOWN", s"The node ${HostnameProvider.hostname} may be down!!!")
        context.become(offline) // become offline. this mode try to check the metrics but call logger with debug level
      case ex: IOException =>
        log.warning("Unexpected IO Exception : {}", ex.getMessage, ex) // do we have to become offline in this case??
    }
  }

  private def tryToProcessControls: Unit = {
    try {
      log.debug("{} received, try to reconnect", CHECK_METRICS);
      jmxClient.reconnect
      //sentinels.foreach(_.analyzeAndReact())
      log.info("Reconnected to the cassandra node");
      mailNotif ! Notification(s"[INFO] Cassandra node ${HostnameProvider.hostname} is UP", s"The node ${HostnameProvider.hostname} joins the cluster")
      context.unbecome // if checks succeeded, the connection is established with the Cassandra node, we can retrieve our nominal state
    } catch {
      case ex: ConnectException => log.debug("Connection error : {}", ex.getMessage);
      case ex: IOException => log.debug("Unexpected IO Exception : {}", ex.getMessage);
    }
  }
}
