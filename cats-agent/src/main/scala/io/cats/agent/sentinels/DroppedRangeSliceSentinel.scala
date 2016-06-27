package io.cats.agent.sentinels

import akka.actor.ActorRef
import akka.event.EventStream
import com.typesafe.config.Config
import io.cats.agent.Messages
import io.cats.agent.bean.{ActorProtocol, MetricsRequest, DroppedMessageStats}
import io.cats.agent.util.JmxClient

class DroppedRangeSliceSentinel(override val metricsProvider: ActorRef, override val conf: Config) extends DroppedSentinel(metricsProvider, conf) {
  override def getDroppedMessageStats: MetricsRequest = MetricsRequest(ActorProtocol.ACTION_CHECK_DROPPED_MESSAGES, Messages.DROPPED_MESSAGE_RANGE_SLICE)
}
