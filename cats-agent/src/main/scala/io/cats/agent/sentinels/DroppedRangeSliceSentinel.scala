package io.cats.agent.sentinels

import akka.actor.ActorRef
import akka.event.EventStream
import com.typesafe.config.Config
import io.cats.agent.bean.DroppedMessageStats
import io.cats.agent.util.JmxClient

class DroppedRangeSliceSentinel(jmxAccess: JmxClient, stream: EventStream, override val conf: Config) extends DroppedSentinel(jmxAccess, stream, conf) {
  override def getDroppedMessageStats: DroppedMessageStats = jmxAccess.getRangeSliceDroppedMessage()
}
