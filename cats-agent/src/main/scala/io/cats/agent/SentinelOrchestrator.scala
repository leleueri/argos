package io.cats.agent

import java.util.concurrent.{Executors, TimeUnit}

import akka.actor.{ActorLogging, Props, Actor}
import com.typesafe.config.ConfigFactory
import io.cats.agent.Constants._
import io.cats.agent.sentinels._
import io.cats.agent.workers.MetricsProvider
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import io.cats.agent.bean._
import Messages._


// to convert the entrySet of globalConfig.getConfig(CONF_OBJECT_ENTRY_NOTIFIERS)
import collection.JavaConversions._

/**
 * The "SentinelOrchestrator" actor schedule the sentinels that analyze information provided by the JMX interface of Cassandra.
 */
class SentinelOrchestrator extends Actor with ActorLogging {

  implicit val evtStream = context.system.eventStream

  val globalConfig = ConfigFactory.load()

  val configSentinel = globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_ORCHESTRATOR)

  // TODO manage User/PWD
  val metricsProvider = context.actorOf(Props(classOf[MetricsProvider], configSentinel.getString(CONF_ORCHESTRATOR_JMX_HOST), configSentinel.getInt(CONF_ORCHESTRATOR_JMX_PORT), None, None))


  // initialize the frequency of metrics control
  context.system.scheduler.schedule(1 second,
    Duration.create(configSentinel.getDuration(CONF_ORCHESTRATOR_INTERVAL).getSeconds, TimeUnit.SECONDS),
    self,
    CHECK_METRICS)(ExecutionContext.fromExecutor(Executors.newSingleThreadScheduledExecutor()))

  log.info("SentinelOrchestrator is running...")

  Array(context.actorOf(Props(classOf[LoadAverageSentinel], globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_LOADAVG)), name = "LoadAverageSentinel"),

  context.actorOf(Props(classOf[DroppedCounterSentinel], metricsProvider, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_DROPPED_COUNTER)), name = "DroppedCounterSentinel"),
  context.actorOf(Props(classOf[DroppedMutationSentinel], metricsProvider, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_DROPPED_MUTATION)), name = "DroppedMutationSentinel"),
  context.actorOf(Props(classOf[DroppedReadSentinel], metricsProvider, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_DROPPED_READ)), name = "DroppedReadSentinel"),
  context.actorOf(Props(classOf[DroppedReadRepairSentinel], metricsProvider, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_DROPPED_READ_REPAIR)), name = "DroppedReadRepairSentinel"),
  context.actorOf(Props(classOf[DroppedPageRangeSentinel], metricsProvider, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_DROPPED_PAGE)), name = "DroppedPageRangeSentinel"),
  context.actorOf(Props(classOf[DroppedRangeSliceSentinel], metricsProvider, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_DROPPED_RANGE)), name = "DroppedRangeSliceSentinel"),
  context.actorOf(Props(classOf[DroppedRequestResponseSentinel], metricsProvider, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_DROPPED_REQ_RESP)), name = "DroppedRequestResponseSentinel"),

  context.actorOf(Props(classOf[CounterMutationBlockedSentinel], metricsProvider, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_STAGE_COUNTER_MUTATION)), name = "CounterMutationBlockedSentinel"),
  context.actorOf(Props(classOf[GossipBlockedSentinel], metricsProvider, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_STAGE_GOSSIP)), name = "GossipBlockedSentinel"),
  context.actorOf(Props(classOf[InternalResponseBlockedSentinel], metricsProvider, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_STAGE_INTERNAL)), name = "InternalResponseBlockedSentinel"),
  context.actorOf(Props(classOf[MemtableFlusherBlockedSentinel], metricsProvider, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_STAGE_MEMTABLE)), name = "MemtableFlusherBlockedSentinel"),
  context.actorOf(Props(classOf[MutationBlockedSentinel], metricsProvider, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_STAGE_MUTATION)), name = "MutationBlockedSentinel"),
  context.actorOf(Props(classOf[ReadBlockedSentinel], metricsProvider, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_STAGE_READ)), name = "ReadBlockedSentinel"),
  context.actorOf(Props(classOf[ReadRepairBlockedSentinel], metricsProvider, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_STAGE_READ_REPAIR)), name = "ReadRepairBlockedSentinel"),
  context.actorOf(Props(classOf[RequestResponseBlockedSentinel], metricsProvider, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_STAGE_REQUEST_RESPONSE)), name = "RequestResponseBlockedSentinel"),

  context.actorOf(Props(classOf[StorageSpaceSentinel], metricsProvider, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_STORAGE_SPACE)), name = "StorageSpaceSentinel"),
  context.actorOf(Props(classOf[StorageHintsSentinel], metricsProvider, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_STORAGE_HINTS)), name = "StorageHintsSentinel"),
  context.actorOf(Props(classOf[StorageExceptionSentinel], metricsProvider, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_STORAGE_EXCEPTION)), name = "StorageExceptionSentinel"),

  context.actorOf(Props(classOf[InternalNotificationsSentinel], globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_JMX_NOTIFICATION)), name = "InternalNotificationsSentinel"),

  context.actorOf(Props(classOf[AvailabilitySentinel], metricsProvider, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_AVAILABLE)), name = "AvailabilitySentinel")

  ).foreach( evtStream.subscribe(_, classOf[CheckMetrics]))

  override def receive = {
    case CHECK_METRICS => processControls
    case NodeStatus(status) if (status == Messages.OFFLINE_NODE) => context.become(offline)
  }

  def offline : Receive = {
    case CHECK_METRICS => metricsProvider ! CheckNodeStatus
    case NodeStatus(status) if (status == Messages.ONLINE_NODE) => context.unbecome()
  }

  private def processControls: Unit = {
      log.debug("{} received", CHECK_METRICS);
      evtStream.publish(CheckMetrics)
  }
}
