package io.argos.agent.orchestrators

import java.util.concurrent.{Executors, TimeUnit}

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.util.Timeout
import io.argos.agent.bean.GatewayStatus

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

import scala.concurrent.ExecutionContext

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val gatewayDescription = jsonFormat3(GatewayStatus)
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

  def buildRoute(requestTimeout: Duration) : Route =
    path("cluster"/"state") {
      get {
        FiniteDuration
        implicit val timeout = Timeout(requestTimeout.toMillis, TimeUnit.MILLISECONDS)
        // query the actor for the current auction state
        val status = (orchestrator ? GetClusterStatus()).mapTo[List[GatewayStatus]]
        complete(status)
      }
    }
}

