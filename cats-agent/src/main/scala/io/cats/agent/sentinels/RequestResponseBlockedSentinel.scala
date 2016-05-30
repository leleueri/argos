package io.cats.agent.sentinels

import akka.actor.ActorRef
import akka.event.EventStream
import com.typesafe.config.Config
import io.cats.agent.bean.ThreadPoolStats
import io.cats.agent.util.JmxClient


class RequestResponseBlockedSentinel(jmxAccess: JmxClient, stream: EventStream, override val conf: Config) extends BlockedSentinel(jmxAccess, stream, conf) {
  override def getThreadPoolStats: ThreadPoolStats = jmxAccess.getRequestResponseStageValues()
}
