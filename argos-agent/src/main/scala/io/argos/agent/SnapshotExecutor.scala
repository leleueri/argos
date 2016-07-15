package io.argos.agent

import akka.actor.Actor.Receive
import akka.actor.{ActorLogging, Actor}
import io.argos.agent.bean.SnapshotCmd
import Constants._

/**
 * Created by eric on 20/06/16.
 */
class SnapshotExecutor extends Actor with ActorLogging {

  override def receive: Receive = {
    case SnapshotCmd(keyspace, table) => log.info("PRINT KEYSPACE SNAPSHOT: " + keyspace)
  }
}
