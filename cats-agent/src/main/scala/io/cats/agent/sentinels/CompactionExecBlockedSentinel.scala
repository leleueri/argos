package io.cats.agent.sentinels

import akka.actor.ActorRef
import com.typesafe.config.Config
import io.cats.agent.Messages
import io.cats.agent.bean.{ActorProtocol, MetricsRequest}


class CompactionExecBlockedSentinel(override val metricsProvider : ActorRef, override val conf: Config) extends BlockedSentinel(metricsProvider, conf) {
  override def getThreadPoolStats: MetricsRequest = MetricsRequest(ActorProtocol.ACTION_CHECK_INTERNAL_STAGE, Messages.INTERNAL_STAGE_COMPACTION_EXEC)
}
