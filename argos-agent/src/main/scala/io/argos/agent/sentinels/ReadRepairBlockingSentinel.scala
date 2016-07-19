package io.argos.agent.sentinels

import akka.actor.ActorRef
import com.typesafe.config.Config
import io.argos.agent.Messages
import io.argos.agent.bean.{ActorProtocol, MetricsRequest}

class ReadRepairBlockingSentinel(override val metricsProvider: ActorRef, override val conf: Config) extends ConsistencySentinel (metricsProvider, conf) {
  override def getReadRepairStats: MetricsRequest = MetricsRequest(ActorProtocol.ACTION_CHECK_READ_REPAIR, Messages.READ_REPAIR_BLOCKING)
}
