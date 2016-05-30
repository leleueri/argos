package io.cats.agent.sentinels

import java.lang.management.OperatingSystemMXBean

import akka.actor.ActorRef
import akka.event.EventStream
import com.typesafe.config.Config
import io.cats.agent.Constants._
import io.cats.agent.bean.{DroppedMessageStats, Notification}
import io.cats.agent.util.JmxClient

class DroppedCounterSentinel(jmxAccess: JmxClient, stream: EventStream, override val conf: Config) extends DroppedSentinel(jmxAccess, stream, conf) {
  override def getDroppedMessageStats: DroppedMessageStats = jmxAccess.getCounterMutationDroppedMessage()
}
