package io.argos.agent.orchestrators

import java.util.concurrent.{Executors, TimeUnit}

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.util.Timeout
import io.argos.agent.bean.{GatewayStatus, SnapshotOrder, SnapshotStatus}

import scala.concurrent.duration.FiniteDuration
import java.time.Duration
import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import spray.json._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import io.argos.agent.AgentOrchestratorConfig
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Location
import io.argos.agent.util.{SnapshotReport, SnapshotReportDto}
import org.jboss.netty.handler.codec.http.HttpResponseStatus

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val gatewayDescriptionJSON = jsonFormat3(GatewayStatus)
  implicit val innerSnapshotStatusJSON = jsonFormat4(SnapshotStatus)

  implicit val requestSnapshotJSON = jsonFormat3(RequestSnapshot)
  implicit val snapshotIdJSON = jsonFormat3(SnapshotId)
  implicit val requestSnapshotStatusJSON = jsonFormat1(RequestSnapshotStatus)
  implicit val responseSnapshotStatusJSON = jsonFormat5(ResponseSnapshotStatus)
  implicit val responseSnapshotReportJSON = jsonFormat10(SnapshotReportDto)
}

/**
  * Created by eric on 06/06/17.
  */
class OrchestratorHttpHandler(orchestrator: ActorRef, orchestratorCfg: AgentOrchestratorConfig, requestTimeout: Duration)(implicit system: ActorSystem) extends JsonSupport {

  // start HTTP Endpoint
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = ExecutionContext.fromExecutor(Executors.newScheduledThreadPool(8))
  val bindingFuture = Http().bindAndHandle(buildRoute(requestTimeout), orchestratorCfg.hostname, orchestratorCfg.port)
  println(s"HTTP Server online at http://${orchestratorCfg.hostname}:${orchestratorCfg.port}/ ...")


  def buildRoute(requestTimeout: Duration) : Route = {
    implicit val timeout = Timeout(requestTimeout.toMillis, TimeUnit.MILLISECONDS)
    path("cluster" / "state") {
      get {
        // query the actor for the current auction state
        val status = (orchestrator ? GetClusterStatus()).mapTo[List[GatewayStatus]]
        complete(status)
      }
    } ~ pathPrefix("keyspaces" / Segment) { keyspace =>
      pathPrefix("snapshots") {
          pathEnd {
            post {
              parameter('table.as[String] ?, 'flush.as[Boolean] ?) { (table, flush) =>
                onComplete((orchestrator ? RequestSnapshot(keyspace, table, flush.getOrElse(true))).mapTo[String]) {
                  case Success(value) => complete(HttpResponse(StatusCodes.Accepted, List(Location(Uri(s"/keyspace/${keyspace}/snapshot/{value}"))), HttpEntity(value)))
                  case Failure(ex) => failWith(ex)
                }
              }
            } ~
            get {
              rejectEmptyResponse {
                onComplete((orchestrator ? RequestKeyspaceSnapshot(keyspace)).mapTo[List[SnapshotReportDto]]) {
                  case Success(value) => complete(value)
                  case Failure(ex) => failWith(ex)
                }
              }
            }
          }~ {
          path(Segment) { snapshotId =>
            rejectEmptyResponse {
              get {
                onComplete((orchestrator ? RequestSnapshotStatus(snapshotId)).mapTo[Option[SnapshotReportDto]]) {
                  case Success(Some(value)) => if (value.keyspace.equals(keyspace)) complete(value) else complete(None)
                  case Success(None) => complete(None)
                  case Failure(ex) => failWith(ex)
                }
              }
            }
          }
        }
      }
    }
  }
}

