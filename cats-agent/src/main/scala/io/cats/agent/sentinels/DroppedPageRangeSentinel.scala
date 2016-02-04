package io.cats.agent.sentinels

import akka.actor.ActorRef
import com.typesafe.config.Config
import io.cats.agent.JmxClient
import io.cats.agent.bean.DroppedMessageStats

class DroppedPageRangeSentinel(jmxAccess: JmxClient, handler: ActorRef, override val conf: Config) extends DroppedSentinel(jmxAccess, handler, conf) {
  override def getDroppedMessageStats: DroppedMessageStats = jmxAccess.getPagedRangeDroppedMessage()
}
