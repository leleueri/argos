package io.argos.agent.util

import java.time.Instant

import akka.actor.ActorRef
import io.argos.agent.bean.{SnapshotOrder, SnapshotStatus}

/**
  * Created by eric on 30/01/18.
  */
case class SnapshotReport(order: SnapshotOrder,
                          done: Boolean = false,
                          initDate:  Long = Instant.now().toEpochMilli,
                          updateDate:  Long = Instant.now().toEpochMilli,
                          status: Map[ActorRef, SnapshotStatus] = Map())


case class SnapshotReportDto(id : String,
                             keyspace: String,
                             table: Option[String],
                             flush: Boolean,
                             status: String ,
                             initDate:  Long ,
                             updateDate:  Long ,
                             nodeOk: List[String] ,
                             nodeKo: List[String] ,
                             nodeOngoing: List[String])