package io.cats.agent

import java.io.IOException
import java.lang.management.ManagementFactory
import java.rmi.ConnectException
import java.util.concurrent.{Executors, Executor, TimeUnit}

import akka.actor.{ActorLogging, Props, Actor}
import akka.actor.Actor.Receive
import com.typesafe.config.{ConfigObject, ConfigFactory}
import io.cats.agent.Constants._
import io.cats.agent.notifiers.{NotifierProvider, ConsoleNotifierProvider, ConsoleNotifier, MailNotifier}
import io.cats.agent.sentinels._
import io.cats.agent.util.{HostnameProvider, JmxClient}
import org.apache.cassandra.tools.NodeProbe
import scala.concurrent.{ExecutionContext, duration}
import scala.concurrent.duration._
import io.cats.agent.bean.Notification
import Messages._

import scala.util.matching.Regex

// to convert the entrySet of globalConfig.getConfig(CONF_OBJECT_ENTRY_NOTIFIERS)
import collection.JavaConversions._

/**
 * The "SentinelOrchestrator" actor schedule the sentinels that analyze information provided by the JMX interface of Cassandra.
 */
class SentinelOrchestrator extends Actor with ActorLogging {

  implicit val evtStream = context.system.eventStream

  val globalConfig = ConfigFactory.load()

  val configSentinel = globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_ORCHESTRATOR)

  val jmxClient = JmxClient(configSentinel.getString(CONF_ORCHESTRATOR_JMX_HOST), configSentinel.getInt(CONF_ORCHESTRATOR_JMX_PORT))
  val osMBean = ManagementFactory.getOperatingSystemMXBean()
  val nodeProbe = new NodeProbe(configSentinel.getString(CONF_ORCHESTRATOR_JMX_HOST), configSentinel.getInt(CONF_ORCHESTRATOR_JMX_PORT))

  // Start all Notifiers
  globalConfig.getConfig(CONF_OBJECT_ENTRY_NOTIFIERS).entrySet().toList.filter( _.getKey.matches("[^\\.]+\\."+CONF_PROVIDER_CLASS_KEY)).foreach(
    confValue => {
      try {
        log.info("Initialize '{}' notifier", confValue.getKey)
        val providerClass: Any = Class.forName(confValue.getValue.unwrapped().asInstanceOf[String]).newInstance()
        context.actorOf(providerClass.asInstanceOf[NotifierProvider].props, confValue.getKey)
      } catch {
        case e : Exception =>
          log.error(e, "Actor system will terminate, unable to initialize the '{}' notifier : '{}'.", confValue.getKey, e.getMessage)
          context.system.terminate()
      }
    }
  )

  // initialize the frequency of metrics control
  val interval = configSentinel.getDuration(CONF_ORCHESTRATOR_INTERVAL)
  context.system.scheduler.schedule(1 second,
    Duration.create(interval.getSeconds, TimeUnit.SECONDS),
    self,
    CHECK_METRICS)(ExecutionContext.fromExecutor(Executors.newSingleThreadScheduledExecutor()))

  log.info("SentinelOrchestrator is running...")

  val sentinels = {
    Array(
      new LoadAverageSentinel(osMBean, evtStream, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_LOADAVG)),

      new AvailabilitySentinel(nodeProbe, evtStream, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_AVAILABLE)),

      new DroppedCounterSentinel(jmxClient, evtStream, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_DROPPED_COUNTER)),
      new DroppedMutationSentinel(jmxClient, evtStream, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_DROPPED_MUTATION)),
      new DroppedReadSentinel(jmxClient, evtStream, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_DROPPED_READ)),
      new DroppedReadRepairSentinel(jmxClient, evtStream, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_DROPPED_READ_REPAIR)),
      new DroppedPageRangeSentinel(jmxClient, evtStream, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_DROPPED_PAGE)),
      new DroppedRangeSliceSentinel(jmxClient, evtStream, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_DROPPED_RANGE)),
      new DroppedRequestResponseSentinel(jmxClient, evtStream, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_DROPPED_REQ_RESP)),

      new StorageSpaceSentinel(jmxClient, evtStream, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_STORAGE_SPACE)),
      new StorageHintsSentinel(jmxClient, evtStream, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_STORAGE_HINTS)),
      new StorageExceptionSentinel(jmxClient, evtStream, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_STORAGE_EXCEPTION)),

      new CounterMutationBlockedSentinel(jmxClient, evtStream, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_STAGE_COUNTER_MUTATION)),
      new GossipBlockedSentinel(jmxClient, evtStream, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_STAGE_GOSSIP)),
      new InternalResponseBlockedSentinel(jmxClient, evtStream, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_STAGE_INTERNAL)),
      new MemtableFlusherBlockedSentinel(jmxClient, evtStream, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_STAGE_MEMTABLE)),
      new MutationBlockedSentinel(jmxClient, evtStream, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_STAGE_MUTATION)),
      new ReadBlockedSentinel(jmxClient, evtStream, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_STAGE_READ)),
      new ReadRepairBlockedSentinel(jmxClient, evtStream, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_STAGE_READ_REPAIR)),
      new RequestResponseBlockedSentinel(jmxClient, evtStream, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_STAGE_REQUEST_RESPONSE)),

      new InternalNotificationsSentinel(jmxClient, evtStream, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_JMX_NOTIFICATION))

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
      sentinels.foreach { sentinel =>
        log.debug("CALL {}.analyseAndReact", sentinel.getClass.getName)
        sentinel.analyzeAndReact()
      }
    } catch {
      case ex: ConnectException =>
        log.warning("Connection error : {}", ex.getMessage, ex);
        evtStream.publish(Notification(s"[CRITIC] Cassandra node ${HostnameProvider.hostname} is DOWN", s"The node ${HostnameProvider.hostname} may be down!!!", "CRITIC","Cassandra node is DOWN", HostnameProvider.hostname))
        context.become(offline) // become offline. this mode try to check the metrics but call logger with debug level
      case ex: IOException =>
        log.warning("Unexpected IO Exception : {}", ex.getMessage, ex) // do we have to become offline in this case??
    }
  }

  private def tryToProcessControls: Unit = {
    try {
      log.debug("{} received, try to reconnect", CHECK_METRICS);
      jmxClient.reconnect
      log.info("Reconnected to the cassandra node");
      evtStream.publish(Notification(s"[INFO] Cassandra node ${HostnameProvider.hostname} is UP", s"The node ${HostnameProvider.hostname} joins the cluster", "INFO","Cassandra node is UP", HostnameProvider.hostname))
      context.unbecome // if checks succeeded, the connection is established with the Cassandra node, we can retrieve our nominal state
    } catch {
      case ex: ConnectException => log.debug("Connection error : {}", ex.getMessage);
      case ex: IOException => log.debug("Unexpected IO Exception : {}", ex.getMessage);
    }
  }
}
