package io.cats.agent.sentinels

import java.lang.management.OperatingSystemMXBean

import akka.actor.ActorRef
import com.typesafe.config.Config
import io.cats.agent.Constants._
import io.cats.agent.bean.{DroppedMessageStats, Notification}
import io.cats.agent.util.JmxClient

class DroppedCounterSentinel(jmxAccess: JmxClient, handler: ActorRef, override val conf: Config) extends DroppedSentinel(jmxAccess, handler, conf) {
  override def getDroppedMessageStats: DroppedMessageStats = jmxAccess.getCounterMutationDroppedMessage()
}
