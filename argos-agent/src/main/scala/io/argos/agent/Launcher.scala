package io.argos.agent

import akka.actor.{ActorRef, ActorSystem, Props}
import Constants._
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
import akka.util.Timeout._
import com.typesafe.config.{Config, ConfigFactory}
import io.argos.agent.bean.{GatewayDescription, GatewayStatus}
import io.argos.agent.notifiers.NotifierProvider
import io.argos.agent.orchestrators.{AgentOrchestrator, GetClusterStatus, SentinelOrchestrator}
import io.argos.agent.util.CommonLoggerFactory
import io.argos.agent.workers.AgentGateway
import akka.pattern.ask

import scala.concurrent.{Future, duration}
import scala.concurrent.duration._
import scala.concurrent.duration.FiniteDuration

// to convert the entrySet of globalConfig.getConfig(CONF_OBJECT_ENTRY_NOTIFIERS)
import collection.JavaConversions._

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val gatewayDescription = jsonFormat3(GatewayStatus)
}


object Launcher extends App with JsonSupport{

  implicit val system = ActorSystem(ACTOR_SYSTEM)

  val cfg: Config = ConfigFactory.load()

  startSentinelOrchestrator
  startAgentOrchestrator
  startAgentGateway
  startAllNotifiers



  // ---------------------------------------------------------------------------------
  // Start Sentinel orchestrator
  // ---------------------------------------------------------------------------------
  def startSentinelOrchestrator = {
    if (cfg.hasPath(CONF_OBJECT_ENTRY_SENTINEL) && cfg.hasPath(CONF_OBJECT_ENTRY_SENTINEL_ENABLE) && cfg.getBoolean(CONF_OBJECT_ENTRY_SENTINEL_ENABLE)) {
      CommonLoggerFactory.commonLogger.info(this, "Start Sentinel Orchestrator")
      system.actorOf(Props[SentinelOrchestrator], name = "SentinelOrchestrator")
    }
    {}
  }

  // ---------------------------------------------------------------------------------
  // Start agent orchestrator with the http endpoint
  // ---------------------------------------------------------------------------------
  def startAgentOrchestrator = {
    val orchestratorCfg = ConfigHelper.getAgentOrchestratorConfig(cfg)
    if (orchestratorCfg.enable) {
      CommonLoggerFactory.commonLogger.info(this, "Agent Orchestrator enabled")
      val orchestrator = system.actorOf(Props(classOf[AgentOrchestrator], orchestratorCfg), name = "AgentOrchestrator")

      // start HTTP Endpoint
      implicit val materializer = ActorMaterializer()
      // FIXME use this ExecutionContxet for HTTP or create a dedicated one???
      implicit val executionContext = system.dispatcher
      val bindingFuture = Http().bindAndHandle(buildRoute(orchestrator), orchestratorCfg.hostname, orchestratorCfg.port)
      println(s"HTTP Server online at http://${orchestratorCfg.hostname}:${orchestratorCfg.port}/ ...")

    } else {
      CommonLoggerFactory.commonLogger.info(this, "Agent Orchestrator disabled")
    }

    {}
  }

  // ---------------------------------------------------------------------------------
  // Start agent gateway
  // ---------------------------------------------------------------------------------
  def startAgentGateway = {
    val agentCfg = ConfigHelper.getAgentGatewayConfig(cfg)
    if (agentCfg.enable) {
      CommonLoggerFactory.commonLogger.info(this, "Agent Gateway enabled")
      system.actorOf(Props(classOf[AgentGateway], agentCfg), name = "AgentGateway")
    } else {
      CommonLoggerFactory.commonLogger.info(this, "Agent Gateway disabled")
    }

    {}
  }

  // ---------------------------------------------------------------------------------
  // Start alert notifiers according to the configuration
  // ---------------------------------------------------------------------------------
  def startAllNotifiers = {
    cfg.getConfig(CONF_OBJECT_ENTRY_NOTIFIERS).entrySet().toList.filter(_.getKey.matches("[^\\.]+\\." + CONF_PROVIDER_CLASS_KEY)).foreach(
        confValue => {
          try {
            val clazz: String = confValue.getValue.unwrapped().asInstanceOf[String]
            CommonLoggerFactory.commonLogger.info(this, "Initialize '{}' notifier", clazz)
            val providerClass: Any = Class.forName(clazz).newInstance()
            system.actorOf(providerClass.asInstanceOf[NotifierProvider].props, confValue.getKey.substring(0, confValue.getKey.indexOf('.')).capitalize + "Notifier")
          } catch {
            case e: Exception =>
              CommonLoggerFactory.commonLogger.error(this, e, "Actor system will terminate, unable to initialize the '{}' notifier : '{}'.", confValue.getKey, e.getMessage)
              system.terminate()
          }
        }
    )
  }

  def buildRoute(orchestrator: ActorRef) : Route =
    path("cluster"/"state") {
        get {
          // TODO use the HTTP configuration
          implicit val timeout: Timeout = 20.seconds
          // query the actor for the current auction state
          val status = (orchestrator ? GetClusterStatus()).mapTo[List[GatewayStatus]]
          complete(status)
        }
    }

}
