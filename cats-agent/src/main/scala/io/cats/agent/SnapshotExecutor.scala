package io.cats.agent

import akka.actor.Actor.Receive
import akka.actor.{ActorLogging, Actor}
import io.cats.agent.Constants._
import io.cats.agent.bean.SnapshotCmd

/**
 * Created by eric on 20/06/16.
 */
class SnapshotExecutor extends Actor with ActorLogging {

  override def receive: Receive = {
    case SnapshotCmd(keyspace, table) => log.info("PRINT KEYSPACE SNAPSHOT: " + keyspace)
  }
}
