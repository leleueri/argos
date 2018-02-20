package io.argos.agent.orchestrators

import java.time.{Duration, Instant}
import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Terminated}
import io.argos.agent.AgentOrchestratorConfig
import io.argos.agent.bean.SnapshotProtocol._
import io.argos.agent.bean._
import io.argos.agent.util.{SnapshotReport, SnapshotReportDto}

import scala.collection.mutable.Map



/**
  * Manage the interactions with each agent.
  */
class AgentOrchestrator(agentOrchestratorCfg: AgentOrchestratorConfig, requestTimeout: Duration) extends Actor with ActorLogging {

  val agents : Map[String, GatewayDescription] = Map()

  val orderReports : Map[String, SnapshotReport] = Map()

  implicit val actorSystem = this.context.system;

  new OrchestratorHttpHandler(self, agentOrchestratorCfg, requestTimeout)

  override def receive: Receive = {
    case Joining(description) => {
      log.info("JOINING received from '{}' named '{}'", description.endpoint, description.name)
      // keep reference of the agent gateway
      agents += (description.name -> description)
      // watch this agent to avoid useless message sending
      context.watch(description.endpoint)
      // ack the message
      sender() ! Registered()
      log.info("REGISTERED send to '{}'", description.endpoint)
    }
    case Terminated(ref) => {
      log.info("AgentGateway '{}' is dead, state of the Cassandra node is unknown", ref)
      switchGatewayState(ref)
    }
    case GetClusterStatus() => {
      log.info("HTTP Endpoint request cluster status" )
      sender() ! agents.values.map(_.toStatus()).toList
    }
    case HeartBeat(Some(description)) => {
      log.debug("HeartBeat received with description : {}", description )
      agents += (description.name -> description)
    }
    case RequestSnapshot(ks, table, flush) => {
      log.info("Receive Snapshot order : ks={}, table={}, flush={}", ks, table, flush)
      val order = SnapshotOrder(UUID.randomUUID().toString, ks, table, flush)
      val report = agents.values.foldLeft(SnapshotReport(order)) {
        (report, gateway) =>
          gateway.endpoint ! order
          report.copy(status = report.status + (gateway.endpoint -> SnapshotStatus(order.id, gateway.name, STATUS_ONGOING)))
      }
      orderReports += (order.id -> report)
      sender() ! order.id
    }
    case snapshotStatus : SnapshotStatus => {
      orderReports.get(snapshotStatus.id) match {
        case Some(report) => {
          val tmpRep = report.copy(status = report.status + (sender() -> snapshotStatus))
          orderReports += (snapshotStatus.id -> tmpRep.copy(updateDate = Instant.now().toEpochMilli, done = !tmpRep.status.exists(_._2.state.equals(STATUS_ONGOING))))
        }
        case None => log.error("Receive a SnapshotStatus '{}' for unknown Snapshot Order!", snapshotStatus)
      }

    }
    case RequestKeyspaceSnapshot(keyspace) => {
      val groupByKs = orderReports.values.groupBy(report => report.order.keyspace)
      val reports = groupByKs.get(keyspace).map(_.toList).getOrElse(List())
      sender() ! reports.map(snapshotReportToDTO)
    }
    case RequestSnapshotStatus(orderId) => {
      sender() ! orderReports.get(orderId).map(snapshotReportToDTO)
    }
  }

  private def snapshotReportToDTO(report: SnapshotReport) = {
    val status = if (!report.done) STATUS_ONGOING else if (report.status.exists(_._2.state.equals(STATUS_KO))) STATUS_KO else STATUS_OK
    val nodeByState = report.status.values.groupBy(_.state)
    SnapshotReportDto(report.order.id,
      report.order.keyspace,
      report.order.table,
      report.order.flush,
      status,
      report.initDate,
      report.updateDate,
      nodeByState.get(STATUS_OK).getOrElse(Iterable.empty).map(_.gatewayName).toList,
      nodeByState.get(STATUS_KO).getOrElse(Iterable.empty).map(_.gatewayName).toList,
      nodeByState.get(STATUS_ONGOING).getOrElse(Iterable.empty).map(_.gatewayName).toList
    )
  }

  private def switchGatewayState(ref: ActorRef) : Unit = {
    agents.values.find( _.endpoint.equals(ref)) match {
      case Some(desc) =>  agents += (desc.name -> desc.copy(gatewayUp = false))
    }
  }

}

case class GetClusterStatus()

case class RequestKeyspaceSnapshot(ks: String)
case class RequestSnapshot(ks: String, table: Option[String], flush: Boolean = true)
case class SnapshotId(id: String, ks: String, table: Option[String])
case class RequestSnapshotStatus(order: String)
case class ResponseSnapshotStatus(order: String, status: String, start: Long, end: Long, details : Iterable[SnapshotStatus])