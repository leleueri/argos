package io.argos.agent.sentinels

import akka.actor.ActorRef
import com.typesafe.config.Config
import io.argos.agent.Constants._
import io.argos.agent.Messages
import io.argos.agent.bean._
import io.argos.agent.util.HostnameProvider

import scala.collection.mutable
import scala.util.Try

/**
  * Created by eric on 27/09/16.
  */
abstract class PendingSentinel (val metricsProvider: ActorRef, val conf: Config) extends Sentinel {

  def getThreadPoolStats : MetricsRequest

  val BSIZE = Try(conf.getInt(CONF_WINDOW_SIZE)).getOrElse(5)
  val wBuffer = new WindowBuffer(BSIZE)
  val checkMean = Try(conf.getBoolean(CONF_WINDOW_MEAN)).getOrElse(false)

  val threshold = conf.getInt(CONF_THRESHOLD)

  override def processProtocolElement: Receive = {

    case CheckMetrics() => if (System.currentTimeMillis >= nextReact) metricsProvider ! getThreadPoolStats
    case metrics: MetricsResponse[ThreadPoolStats] if metrics.value.isDefined => {

      val threadPool = metrics.value.get

      wBuffer.push(threadPool)

      if (log.isDebugEnabled) {
        log.debug("PendingSentinel : ThreadPool=<{}>, pendingTasks=<{}>", threadPool.`type`, threadPool.pendingTasks.toString)
      }

      if (System.currentTimeMillis >= nextReact) {
        if (checkMean && !wBuffer.meanUnderThreshold(threshold)) {
          react(threadPool)
        } else if (!wBuffer.underThreshold(threshold)) {
          react(threadPool)
        }
      }
    }
  }

  def react(info:  ThreadPoolStats): Unit = {

    val message =
      s"""Cassandra Node ${HostnameProvider.hostname} may be overloaded.
          |
          |During last '${BSIZE}' checks, too many actions are pending for the Type '${info.`type`}'
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

    context.system.eventStream.publish(buildNotification(message))

    nextReact = System.currentTimeMillis + FREQUENCY

    wBuffer.clear()

    { }
  }
}

// --------- PendingSentinel implementations

class CompactionExecPendingSentinel(override val metricsProvider : ActorRef, override val conf: Config) extends PendingSentinel(metricsProvider, conf) {
  override def getThreadPoolStats: MetricsRequest = MetricsRequest(ActorProtocol.ACTION_CHECK_INTERNAL_STAGE, Messages.INTERNAL_STAGE_COMPACTION_EXEC)
}

class CounterMutationPendingSentinel(override val metricsProvider : ActorRef, override val conf: Config) extends PendingSentinel(metricsProvider, conf) {
  override def getThreadPoolStats: MetricsRequest = MetricsRequest(ActorProtocol.ACTION_CHECK_STAGE, Messages.STAGE_COUNTER_MUTATION)
}

class GossipPendingSentinel(override val metricsProvider : ActorRef, override val conf: Config) extends PendingSentinel(metricsProvider, conf) {
  override def getThreadPoolStats: MetricsRequest = MetricsRequest(ActorProtocol.ACTION_CHECK_INTERNAL_STAGE, Messages.INTERNAL_STAGE_GOSSIP)
}

class InternalResponsePendingSentinel(override val metricsProvider : ActorRef, override val conf: Config) extends PendingSentinel(metricsProvider, conf) {
  override def getThreadPoolStats: MetricsRequest = MetricsRequest(ActorProtocol.ACTION_CHECK_INTERNAL_STAGE, Messages.INTERNAL_STAGE_INTERNAL_RESPONSE)
}

class MemtableFlusherPendingSentinel(override val metricsProvider : ActorRef, override val conf: Config) extends PendingSentinel(metricsProvider, conf) {
  override def getThreadPoolStats: MetricsRequest = MetricsRequest(ActorProtocol.ACTION_CHECK_INTERNAL_STAGE, Messages.INTERNAL_STAGE_MEMTABLE_FLUSHER)
}

class MutationPendingSentinel( override val metricsProvider : ActorRef, override val conf: Config) extends PendingSentinel(metricsProvider, conf) {
  override def getThreadPoolStats: MetricsRequest = MetricsRequest(ActorProtocol.ACTION_CHECK_STAGE, Messages.STAGE_MUTATION)
}

class ReadPendingSentinel(override val metricsProvider : ActorRef, override val conf: Config) extends PendingSentinel(metricsProvider, conf) {
  override def getThreadPoolStats: MetricsRequest = MetricsRequest(ActorProtocol.ACTION_CHECK_STAGE, Messages.STAGE_READ)
}

class ReadRepairPendingSentinel(override val metricsProvider : ActorRef, override val conf: Config) extends PendingSentinel(metricsProvider, conf) {
  override def getThreadPoolStats: MetricsRequest = MetricsRequest(ActorProtocol.ACTION_CHECK_STAGE, Messages.STAGE_READ_REPAIR)
}

class RequestResponsePendingSentinel(override val metricsProvider : ActorRef, override val conf: Config) extends PendingSentinel(metricsProvider, conf) {
  override def getThreadPoolStats: MetricsRequest = MetricsRequest(ActorProtocol.ACTION_CHECK_STAGE, Messages.STAGE_REQUEST_RESPONSE)
}

// --------- Window buffer utility class

class WindowBuffer(limit : Int) {
  val buffer = mutable.Queue[ThreadPoolStats]()

  def push(elt: ThreadPoolStats): Unit = {
    if (buffer.size == limit) buffer.dequeue()
    buffer.enqueue(elt)
  }

  /**
    * @param threshold
    * @return true if the mean pending tasks of the buffer are under the threshold
    */
  def meanUnderThreshold(threshold: Int) : Boolean = {
    if (buffer.size == limit) (buffer.foldLeft(0)((cumul, poolStats) => cumul + poolStats.pendingTasks)/limit) < threshold
    else true
  }

  /**
    * @param threshold
    * @return true if all pending tasks of the buffer are under the threshold
    */
  def underThreshold(threshold: Int) : Boolean = {
    if (buffer.size == limit) buffer.exists( _.pendingTasks < threshold)
    else true
  }

  def clear() = buffer.clear()
}