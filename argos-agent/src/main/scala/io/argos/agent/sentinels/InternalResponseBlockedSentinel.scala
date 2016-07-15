package io.argos.agent.sentinels

import akka.actor.ActorRef
import akka.event.EventStream
import com.typesafe.config.Config
import io.argos.agent.Messages
import io.argos.agent.bean.{MetricsRequest, ActorProtocol}
import io.argos.agent.util.JmxClient
import io.argos.agent.bean.MetricsRequest


class InternalResponseBlockedSentinel(override val metricsProvider : ActorRef, override val conf: Config) extends BlockedSentinel(metricsProvider, conf) {
  override def getThreadPoolStats: MetricsRequest = MetricsRequest(ActorProtocol.ACTION_CHECK_INTERNAL_STAGE, Messages.INTERNAL_STAGE_INTERNAL_RESPONSE)
}
