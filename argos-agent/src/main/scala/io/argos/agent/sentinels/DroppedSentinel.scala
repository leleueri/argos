package io.argos.agent.sentinels

import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import com.typesafe.config.Config
import io.argos.agent.{Constants, Messages}
import io.argos.agent.bean.{DroppedMessageStats, MetricsRequest, MetricsResponse}
import io.argos.agent.util.HostnameProvider
import Constants._
import io.argos.agent.bean._

import scala.concurrent.duration.FiniteDuration
import scala.util.Try

abstract class DroppedSentinel(val metricsProvider: ActorRef, val conf: Config) extends Sentinel {

  private var nextReact = System.currentTimeMillis
  private val FREQUENCY = Try(conf.getDuration(CONF_FREQUENCY, TimeUnit.MILLISECONDS)).getOrElse(FiniteDuration(5, TimeUnit.MINUTES).toMillis)

  def getDroppedMessageStats : MetricsRequest

  override def processProtocolElement: Receive = {

    case CheckMetrics() => if (System.currentTimeMillis >= nextReact) metricsProvider ! getDroppedMessageStats

    case metrics: MetricsResponse[DroppedMessageStats] if metrics.value.isDefined => {

      val droppedMsg = metrics.value.get

      if (log.isDebugEnabled) {
        log.debug("DroppedSentinel : MessageType=<{}>, onMinRate=<{}>, totalDropped=<{}>", droppedMsg.`type`, droppedMsg.oneMinRate.toString, droppedMsg.count.toString)
      }

      if (droppedMsg.oneMinRate > 0.0) {
        react(droppedMsg)
      }
    }
  }

  def react(info:  DroppedMessageStats): Unit = {

    val message =
      s"""Cassandra Node ${HostnameProvider.hostname} may be overloaded.
        |
        |There are some dropped messages for the Type '${info.`type`}'
        |
        |Last Minute Dropped messages : ${info.oneMinRate}
        |Last Five minutes Dropped messages : ${info.fiveMinRate}
        |Last Fifteen minutes Dropped messages : ${info.fifteenMinRate}
        |
        |Total since startup : ${info.count}
        |
        |Something wrong may append on this node...
      """.stripMargin

    context.system.eventStream.publish(buildNotification(message))

    nextReact = System.currentTimeMillis + FREQUENCY

    { }
  }
}

// ------ DroppedSentinel Implementations

class DroppedRequestResponseSentinel(override val metricsProvider: ActorRef, override val conf: Config) extends DroppedSentinel(metricsProvider, conf) {
  override def getDroppedMessageStats: MetricsRequest = MetricsRequest(ActorProtocol.ACTION_CHECK_DROPPED_MESSAGES, Messages.DROPPED_MESSAGE_REQUEST_RESPONSE)
}

class DroppedReadSentinel(override val metricsProvider: ActorRef, override val conf: Config) extends DroppedSentinel(metricsProvider, conf) {
  override def getDroppedMessageStats: MetricsRequest = MetricsRequest(ActorProtocol.ACTION_CHECK_DROPPED_MESSAGES, Messages.DROPPED_MESSAGE_READ)
}

class DroppedReadRepairSentinel(override val metricsProvider: ActorRef, override val conf: Config) extends DroppedSentinel(metricsProvider, conf) {
  override def getDroppedMessageStats: MetricsRequest = MetricsRequest(ActorProtocol.ACTION_CHECK_DROPPED_MESSAGES, Messages.DROPPED_MESSAGE_READ_REPAIR)
}

class DroppedRangeSliceSentinel(override val metricsProvider: ActorRef, override val conf: Config) extends DroppedSentinel(metricsProvider, conf) {
  override def getDroppedMessageStats: MetricsRequest = MetricsRequest(ActorProtocol.ACTION_CHECK_DROPPED_MESSAGES, Messages.DROPPED_MESSAGE_RANGE_SLICE)
}

class DroppedPageRangeSentinel(override val metricsProvider: ActorRef, override val conf: Config) extends DroppedSentinel(metricsProvider, conf) {
  override def getDroppedMessageStats: MetricsRequest = MetricsRequest(ActorProtocol.ACTION_CHECK_DROPPED_MESSAGES, Messages.DROPPED_MESSAGE_PAGED_RANGE)
}

class DroppedMutationSentinel(override val metricsProvider: ActorRef, override val conf: Config) extends DroppedSentinel(metricsProvider, conf) {
  override def getDroppedMessageStats: MetricsRequest = MetricsRequest(ActorProtocol.ACTION_CHECK_DROPPED_MESSAGES, Messages.DROPPED_MESSAGE_MUTATION)
}

class DroppedCounterSentinel(override val metricsProvider: ActorRef, override val conf: Config) extends DroppedSentinel(metricsProvider, conf) {
  override def getDroppedMessageStats: MetricsRequest = MetricsRequest(ActorProtocol.ACTION_CHECK_DROPPED_MESSAGES, Messages.DROPPED_MESSAGE_COUNTER_MUTATION)
}