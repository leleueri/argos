package io.cats.agent.sentinels

import akka.actor.ActorRef
import com.typesafe.config.Config
import io.cats.agent.JmxClient
import io.cats.agent.bean.ThreadPoolStats


class CounterMutationBlockedSentinel(jmxAccess: JmxClient, handler: ActorRef, override val conf: Config) extends BlockedSentinel(jmxAccess, handler, conf) {
  override def getThreadPoolStats: ThreadPoolStats = jmxAccess.getCounterMutationStageValues()
}
