package io.cats.agent.sentinels

import akka.actor.ActorRef
import akka.event.EventStream
import com.typesafe.config.Config
import io.cats.agent.Messages
import io.cats.agent.bean.{ActorProtocol, MetricsRequest, ThreadPoolStats}
import io.cats.agent.util.JmxClient


class ReadRepairBlockedSentinel(override val metricsProvider : ActorRef, override val conf: Config) extends BlockedSentinel(metricsProvider, conf) {
  override def getThreadPoolStats: MetricsRequest = MetricsRequest(ActorProtocol.ACTION_CHECK_STAGE, Messages.STAGE_READ_REPAIR)
}
