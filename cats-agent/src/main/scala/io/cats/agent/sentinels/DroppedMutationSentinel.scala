package io.cats.agent.sentinels

import akka.actor.ActorRef
import com.typesafe.config.Config
import io.cats.agent.bean.DroppedMessageStats
import io.cats.agent.util.JmxClient

class DroppedMutationSentinel(jmxAccess: JmxClient, handler: ActorRef, override val conf: Config) extends DroppedSentinel(jmxAccess, handler, conf) {
  override def getDroppedMessageStats: DroppedMessageStats = jmxAccess.getMutationDroppedMessage()
}
