package io.argos.agent.sentinels

import akka.actor.ActorRef
import io.argos.agent.{Messages, SentinelConfiguration}
import io.argos.agent.bean._
import io.argos.agent.util.{HostnameProvider, WindowBuffer}

/**
  * Created by eric on 27/09/16.
  */
abstract class PendingSentinel (val metricsProvider: ActorRef, val conf: SentinelConfiguration) extends Sentinel {

  def getThreadPoolStats : MetricsRequest

  val wBuffer = new WindowBuffer[ThreadPoolStats](conf.windowSize)
  val checkMean = conf.checkMean
  val threshold = conf.threshold.toInt

  private def extractPendingTasks(entry: ThreadPoolStats) : Double = entry.pendingTasks.toDouble

  override def processProtocolElement: Receive = {

    case CheckMetrics() => if (System.currentTimeMillis >= nextReact) metricsProvider ! getThreadPoolStats
    case metrics: MetricsResponse[ThreadPoolStats] if metrics.value.isDefined => {

      val threadPool = metrics.value.get

      wBuffer.push(threadPool)

      if (log.isDebugEnabled) {
        log.debug("PendingSentinel : ThreadPool=<{}>, pendingTasks=<{}>", threadPool.`type`, threadPool.pendingTasks.toString)
      }

      if (System.currentTimeMillis >= nextReact) {
        if ((checkMean && !wBuffer.meanUnderThreshold(threshold, extractPendingTasks))
          || (!checkMean && !wBuffer.underThreshold(threshold, extractPendingTasks))) {
          react(threadPool)
        }
      }
    }
  }

  def react(info:  ThreadPoolStats): Unit = {

    val message =
      s"""Cassandra Node ${HostnameProvider.hostname} may be overloaded.
          |
          |During last '${conf.windowSize}' checks, too many actions are pending for the Type '${info.`type`}'
          |
          |Last ThreadPool value:
          |
          |- Currently blocked tasks : ${info.currentBlockedTasks}
          |- Pending tasks           : ${info.pendingTasks}
          |- Active Tasks            : ${info.activeTasks}
          |- Available executors     : ${info.maxPoolSize}
          |
          |- Total blocked tasks since node startup : ${info.totalBlockedTasks}
          |
      """.stripMargin

    context.system.eventStream.publish(buildNotification(conf.messageHeader.map(h => h + " \n\n--####--\n\n" + message).getOrElse(message)))

    updateNextReact()

    wBuffer.clear()

    { }
  }
}

// --------- PendingSentinel implementations

class CompactionExecPendingSentinel(override val metricsProvider : ActorRef, override val conf: SentinelConfiguration) extends PendingSentinel(metricsProvider, conf) {
  override def getThreadPoolStats: MetricsRequest = MetricsRequest(ActorProtocol.ACTION_CHECK_INTERNAL_STAGE, Messages.INTERNAL_STAGE_COMPACTION_EXEC)
}

class CounterMutationPendingSentinel(override val metricsProvider : ActorRef, override val conf: SentinelConfiguration) extends PendingSentinel(metricsProvider, conf) {
  override def getThreadPoolStats: MetricsRequest = MetricsRequest(ActorProtocol.ACTION_CHECK_STAGE, Messages.STAGE_COUNTER_MUTATION)
}

class GossipPendingSentinel(override val metricsProvider : ActorRef, override val conf: SentinelConfiguration) extends PendingSentinel(metricsProvider, conf) {
  override def getThreadPoolStats: MetricsRequest = MetricsRequest(ActorProtocol.ACTION_CHECK_INTERNAL_STAGE, Messages.INTERNAL_STAGE_GOSSIP)
}

class InternalResponsePendingSentinel(override val metricsProvider : ActorRef, override val conf: SentinelConfiguration) extends PendingSentinel(metricsProvider, conf) {
  override def getThreadPoolStats: MetricsRequest = MetricsRequest(ActorProtocol.ACTION_CHECK_INTERNAL_STAGE, Messages.INTERNAL_STAGE_INTERNAL_RESPONSE)
}

class MemtableFlusherPendingSentinel(override val metricsProvider : ActorRef, override val conf: SentinelConfiguration) extends PendingSentinel(metricsProvider, conf) {
  override def getThreadPoolStats: MetricsRequest = MetricsRequest(ActorProtocol.ACTION_CHECK_INTERNAL_STAGE, Messages.INTERNAL_STAGE_MEMTABLE_FLUSHER)
}

class MutationPendingSentinel( override val metricsProvider : ActorRef, override val conf: SentinelConfiguration) extends PendingSentinel(metricsProvider, conf) {
  override def getThreadPoolStats: MetricsRequest = MetricsRequest(ActorProtocol.ACTION_CHECK_STAGE, Messages.STAGE_MUTATION)
}

class ReadPendingSentinel(override val metricsProvider : ActorRef, override val conf: SentinelConfiguration) extends PendingSentinel(metricsProvider, conf) {
  override def getThreadPoolStats: MetricsRequest = MetricsRequest(ActorProtocol.ACTION_CHECK_STAGE, Messages.STAGE_READ)
}

class ReadRepairPendingSentinel(override val metricsProvider : ActorRef, override val conf: SentinelConfiguration) extends PendingSentinel(metricsProvider, conf) {
  override def getThreadPoolStats: MetricsRequest = MetricsRequest(ActorProtocol.ACTION_CHECK_STAGE, Messages.STAGE_READ_REPAIR)
}

class RequestResponsePendingSentinel(override val metricsProvider : ActorRef, override val conf: SentinelConfiguration) extends PendingSentinel(metricsProvider, conf) {
  override def getThreadPoolStats: MetricsRequest = MetricsRequest(ActorProtocol.ACTION_CHECK_STAGE, Messages.STAGE_REQUEST_RESPONSE)
}
