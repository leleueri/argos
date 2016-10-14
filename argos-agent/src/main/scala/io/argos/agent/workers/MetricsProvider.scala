package io.argos.agent.workers

import java.io.IOException
import javax.management.{InstanceNotFoundException, NotificationListener, ObjectName}

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorContext, ActorLogging, ActorRef}
import io.argos.agent.{Constants, Messages}
import io.argos.agent.bean._
import io.argos.agent.util.{CommonLoggerFactory, HostnameProvider, JmxClient}
import Constants._
import Messages._
import io.argos.agent.bean._
import ActorProtocol._
import CommonLoggerFactory._
import io.argos.agent.util.JmxClient
import java.rmi.ConnectException

import com.typesafe.config.Config
import org.apache.cassandra.tools.NodeProbe

import scala.collection.JavaConverters._
/**
 * Created by eric on 27/06/16.
 */
class MetricsProvider(jmxConfig: Config) extends NotificationListener with Actor with ActorLogging {

  val hostname = jmxConfig.getString(CONF_ORCHESTRATOR_JMX_HOST)
  val port = jmxConfig.getInt(CONF_ORCHESTRATOR_JMX_PORT)
  val user = if (jmxConfig.hasPath(CONF_ORCHESTRATOR_JMX_USER)) Some(jmxConfig.getString(CONF_ORCHESTRATOR_JMX_USER)) else None
  val pwd = if (jmxConfig.hasPath(CONF_ORCHESTRATOR_JMX_PWD)) Some(jmxConfig.getString(CONF_ORCHESTRATOR_JMX_PWD)) else None

  val downLevel = jmxConfig.getString(CONF_ORCHESTRATOR_DOWN_LEVEL)
  val downLabel = jmxConfig.getString(CONF_ORCHESTRATOR_DOWN_LABEL)
  val upLevel = jmxConfig.getString(CONF_ORCHESTRATOR_UP_LEVEL)
  val upLabel = jmxConfig.getString(CONF_ORCHESTRATOR_UP_LABEL)

  log.debug("Start MetricsProvider with params : hostname=<{}>, port=<{}>, user=<{}>, password=<{}>", hostname, port, user, pwd)

  val jmxClient =  try {
    JmxClient(hostname, port, user, pwd)
  } catch {
    case e => {
      log.error(e, "Unable to initialize the JMX client, check the configuration. Actor system will terminate...")
      context.system.terminate()
      throw e;
    }
  }

  val nodeProbe = new NodeProbe(hostname, port)
  lazy val thisEndpoint = nodeProbe.getEndpoint

  override def receive: Receive = {
    case CheckNodeStatus => log.debug("Node is online, ignore the ping message")
    case AvailabilityRequirements(ks, cl) => {
      log.debug(s"MetricsProvider receives Availability(${ks}, ${cl})")
      checkAvailability(sender(), ks, cl)
    }
    case req : MetricsRequest => {
      log.debug(s"MetricsProvider receives ${req}")
      try {

        req match {
          case MetricsRequest(ACTION_CHECK_DROPPED_MESSAGES, msgType) => sender ! MetricsResponse(ACTION_CHECK_DROPPED_MESSAGES, Some(jmxClient.getDroppedMessages(msgType)))
          case MetricsRequest(ACTION_CHECK_INTERNAL_STAGE, msgType) => sender ! MetricsResponse(ACTION_CHECK_INTERNAL_STAGE, Some(jmxClient.getInternalStageValue(msgType)))
          case MetricsRequest(ACTION_CHECK_STAGE, msgType) => sender ! MetricsResponse(ACTION_CHECK_STAGE, Some(jmxClient.getStageValue(msgType)))
          case MetricsRequest(ACTION_CHECK_STORAGE_SPACE, msgType) => sender ! MetricsResponse(ACTION_CHECK_STORAGE_SPACE, Some(jmxClient.getStorageSpaceInformation()))
          case MetricsRequest(ACTION_CHECK_STORAGE_HINTS, msgType) => sender ! MetricsResponse(ACTION_CHECK_STORAGE_HINTS, Some(jmxClient.getStorageHints()))
          case MetricsRequest(ACTION_CHECK_STORAGE_EXCEPTION, msgType) => sender ! MetricsResponse(ACTION_CHECK_STORAGE_EXCEPTION, Some(jmxClient.getStorageMetricExceptions()))
          case MetricsRequest(ACTION_CHECK_READ_REPAIR, msgType) => sender ! MetricsResponse(ACTION_CHECK_READ_REPAIR, Some(jmxClient.getReadRepairs(msgType)))
          case MetricsRequest(ACTION_CHECK_CNX_TIMEOUT, msgType) => sender ! MetricsResponse(ACTION_CHECK_CNX_TIMEOUT, Some(jmxClient.getConnectionTimeouts()))
          case MetricsRequest(ACTION_CHECK_GC, msgType) => sender ! MetricsResponse(ACTION_CHECK_GC, Some(jmxClient.getGCInspector()))
        }

      } catch {
        case ex: InstanceNotFoundException =>
          log.info("JMX Instance not found : {}", ex.getMessage)
        case ex: ConnectException =>
          log.warning("Connection error : {}", ex.getMessage, ex);
          context.system.eventStream.publish(
            Notification(self.path.name, s"[${downLevel}] Cassandra node ${HostnameProvider.hostname} is DOWN",
              s"The node ${HostnameProvider.hostname} may be down!!!",
              downLevel,
              downLabel,
              HostnameProvider.hostname))

          context.become(offline) // become offline. this mode try to check the metrics but call logger with debug level
          context.parent ! NodeStatus(OFFLINE_NODE)
        case ex: IOException =>
          log.warning("Unexpected IO Exception : {}", ex.getMessage, ex) // do we have to become offline in this case??
      }
    }
  }

  def offline : Receive = {
    case CheckNodeStatus => if (tryToProcessControls) sender() ! NodeStatus(ONLINE_NODE)
    case msg => log.debug("node is offline, message <{}> will be ignored", msg)
  }

  private def tryToProcessControls: Boolean = {
    try {
      log.debug("{} received, try to reconnect", CHECK_METRICS);
      jmxClient.reconnect

      log.info("Reconnected to the cassandra node");
      context.system.eventStream.publish(
        Notification(self.path.name, s"[${upLevel}] Cassandra node ${HostnameProvider.hostname} is UP",
          s"The node ${HostnameProvider.hostname} joins the cluster",
          upLevel,
          upLabel,
          HostnameProvider.hostname))

      context.unbecome // if checks succeeded, the connection is established with the Cassandra node, we can retrieve our nominal state
      true
    } catch {
      case ex: ConnectException => log.debug("Connection error : {}", ex.getMessage); false;
      case ex: IOException => log.debug("Unexpected IO Exception : {}", ex.getMessage); false;
    }
  }

  jmxClient.addNotificationListener(new ObjectName("org.apache.cassandra.db:type=StorageService"), this)

  override def handleNotification(notification: javax.management.Notification, handback: scala.Any): Unit = {
    context.system.eventStream.publish(JmxNotification(notification))
  }

  def checkAvailability(sender: ActorRef, keyspace: String, consistencyLevel: String): Unit = {
    val listOfAvailability = nodeProbe.describeRing(keyspace).asScala.map(interpretTokenRangeString(_))
      .filter(_.endpoints.contains(thisEndpoint)) // compute only replicas set containing the current node.
      .map(detectAvailabilityIssue(_, thisEndpoint, consistencyLevel, keyspace))
      .filter(_.isDefined) // keep endpoints with CL issue
      .map(_.get).toList

    val mergedByEndpointList = listOfAvailability.groupBy(_.unreachableEndpoints).map {
      case (unreachEndpoint:List[String], details: List[Availability]) => {
        details.foldLeft(details(0).copy(tokenRanges = List()))((collector, entry) => collector.copy(tokenRanges = collector.tokenRanges ::: entry.tokenRanges))
      }
    }.toList

    sender ! AvailabilityIssue(mergedByEndpointList)
  }

  private def interpretTokenRangeString(tokenrange: String): ArgosTokenRange = {
    val cTokenRange = ("start_token:([^,]+),\\send_token:([^,]+),\\sendpoints:\\[([^\\]]+)\\],\\srpc_endpoints:\\[([^\\]]+)").r.findAllIn(tokenrange).matchData map {
      m => ArgosTokenRange(
        m.group(1).toLong,
        m.group(2).toLong,
        m.group(3).split(",").map(_.trim).toList,
        m.group(4).split(",").map(_.trim).toList,
        List())
    }

    val details = ("host:([^,]+),\\sdatacenter:([^,]+),\\srack:([^\\)]+)").r.findAllIn(tokenrange).matchData map {
      m => ArgosEndpointDetails(m.group(1), m.group(2), m.group(3))
    }

    cTokenRange.next().copy(endpointDetails = details.toList)
  }

  private def detectAvailabilityIssue(range: ArgosTokenRange, connectedEndpoint: String, cl: String, ks: String) : Option[Availability] = {
    val localDC = range.endpointDetails.filter(_.host == connectedEndpoint).head.dc
    val consistencyLevel = cl.toLowerCase

    if(log.isDebugEnabled) {
      log.debug("exec detectAvailabilityIssue: token=<"+ range.start +", " + range.end +">, ks=<{}>, cl=<{}>, local-dc=<{}>, allReplicas=<{}>",
        ks, cl, localDC, range.endpoints.mkString(","))
    }
    //
    val targetEndpoints =
      if (consistencyLevel.startsWith("local")) range.endpointDetails.groupBy(_.dc)(localDC).map(_.host)
      else range.endpoints

    val unreachNodes = (targetEndpoints intersect(nodeProbe.getUnreachableNodes.asScala)).sorted

    val maxUnreachNodes = consistencyLevel match {
      case "one"|"local_one" => targetEndpoints.length - 1
      case "two" => targetEndpoints.length - 2
      case "three" => targetEndpoints.length - 3
      case "all" => 0
      case _ => (targetEndpoints.length - (1 + (targetEndpoints.length/2))) // QUORUM / LOCAL_QUORUM
    }

    if (unreachNodes.length > maxUnreachNodes) {
      if(log.isDebugEnabled) {
        log.debug("FOUND AvailabilityIssue: token=<["+ range.start +", " + range.end +"]>, ks=<"+ks+">, cl=<{}>, local-dc=<{}>, targetReplicas=<{}>, unreachables=<{}>",
          cl, localDC,targetEndpoints.mkString(","),
          unreachNodes.mkString(","))
      }
      Some(Availability(ks, cl, unreachNodes, List(range)))
    } else
      None
  }
}


