package io.argos.agent.sentinels

import akka.actor.ActorRef
import akka.event.EventStream
import com.typesafe.config.Config
import io.argos.agent.Messages
import io.argos.agent.bean.{MetricsRequest, ActorProtocol}
import io.argos.agent.util.JmxClient
import io.argos.agent.bean.MetricsRequest

class DroppedPageRangeSentinel(override val metricsProvider: ActorRef, override val conf: Config) extends DroppedSentinel(metricsProvider, conf) {
  override def getDroppedMessageStats: MetricsRequest = MetricsRequest(ActorProtocol.ACTION_CHECK_DROPPED_MESSAGES, Messages.DROPPED_MESSAGE_PAGED_RANGE)
}
