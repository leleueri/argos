package io.cats.agent.sentinels

import java.util.concurrent.TimeUnit

import akka.event.EventStream
import com.typesafe.config.Config
import io.cats.agent.Constants._
import io.cats.agent.bean.{CatsEndpointDetails, CatsTokenRange, Notification, Availability}
import io.cats.agent.util.CommonLoggerFactory._
import io.cats.agent.util.HostnameProvider
import org.apache.cassandra.tools.NodeProbe

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

import scala.collection.JavaConverters._

class AvailabilitySentinel(nodeProbe: NodeProbe, stream: EventStream, override val conf: Config) extends Sentinel[List[Availability]] {

  private var nextReact = System.currentTimeMillis
  private val FREQUENCY = Try(conf.getDuration(CONF_FREQUENCY, TimeUnit.MILLISECONDS)).getOrElse(FiniteDuration(5, TimeUnit.MINUTES).toMillis)

  override def analyze(): Option[List[Availability]] = {
    if (System.currentTimeMillis <= nextReact) {
      None
    } else {
      val thisEndpoint = nodeProbe.getEndpoint
      val issuePerKeyspace = (for {
        pair <- conf.getConfigList(CONF_KEYSPACES).asScala.toList
        ks = pair.getString(CONF_KEYSPACE_NAME)
        cl = pair.getString(CONF_CONSISTENCY_LEVEL)
        result = nodeProbe.describeRing(ks).asScala.map(interpretTokenRangeString(_))
          .filter(_.endpoints.contains(thisEndpoint)) // compute only replicas set containing the current node.
          .map(detectAvailabilityIssue(_, thisEndpoint, cl, ks))
          .filter(_.isDefined) // keep endpoints with CL issue
          .map(_.get)
      } yield result).flatten

      if (issuePerKeyspace.isEmpty) None else Some(issuePerKeyspace)
    }
  }

  private def interpretTokenRangeString(tokenrange: String): CatsTokenRange = {
    val cTokenRange = ("start_token:([^,]+),\\send_token:([^,]+),\\sendpoints:\\[([^\\]]+)\\],\\srpc_endpoints:\\[([^\\]]+)").r.findAllIn(tokenrange).matchData map {
      m => CatsTokenRange(
        m.group(1).toLong,
        m.group(2).toLong,
        m.group(3).split(",").map(_.trim).toList,
        m.group(4).split(",").map(_.trim).toList,
        List())
    }

    val details = ("host:([^,]+),\\sdatacenter:([^,]+),\\srack:([^\\)]+)").r.findAllIn(tokenrange).matchData map {
      m => CatsEndpointDetails(m.group(1), m.group(2), m.group(3))
    }

    cTokenRange.next().copy(endpointDetails = details.toList)
  }

  private def detectAvailabilityIssue(range: CatsTokenRange, connectedEndpoint: String, cl: String, ks: String) : Option[Availability] = {
    val localDC = range.endpointDetails.filter(_.host == connectedEndpoint).head.dc
    val consitencyLevel = cl.toLowerCase

    if(sentinelLogger.isDebugEnabled) {
      sentinelLogger.debug(this, "detectAvailabilityIssue: token=<{}>, ks=<{}>, cl=<{}>, local-dc=<{}>, allReplicas=<{}>",
        "[" + range.start + ", " + range.end + "]",
        ks, cl, localDC, range.endpoints.mkString(","))
    }
//
    val targetEndpoints =
      if (consitencyLevel.startsWith("local")) range.endpointDetails.groupBy(_.dc)(localDC).map(_.host)
      else range.endpoints

    val unreachNodes = targetEndpoints intersect(nodeProbe.getUnreachableNodes.asScala)
    if(sentinelLogger.isDebugEnabled) {
      sentinelLogger.debug(this, "detectAvailabilityIssue: token=<{}>, ks=<{}>, cl=<{}>, local-dc=<{}>, targetReplicas=<{}>, unreachables=<{}>",
        "["+ range.start + ", " + range.end +"]",
        ks, cl, localDC,targetEndpoints.mkString(","),
        unreachNodes.mkString(","))
    }

    val maxUnreachNodes = consitencyLevel match {
      case "one"|"local_one" => targetEndpoints.length - 1
      case "two" => targetEndpoints.length - 2
      case "three" => targetEndpoints.length - 3
      case "all" => 0
      case _ => (targetEndpoints.length - (1 + (targetEndpoints.length/2))) // QUORUM / LOCAL_QUORUM
    }

    if (unreachNodes.length > maxUnreachNodes) {
      Some(Availability(ks, cl, unreachNodes, range))
    } else
      None
  }
  
  override def react(info:  List[Availability]): Unit = {

    info.foreach {
      availabilityInfo =>
        val message = s"""Application may have consistency issue (detected by cassandra node '${HostnameProvider.hostname}')
                  |
                  |Unreachable ${availabilityInfo.unreachableEndpoints.length} replica(s) for range[${availabilityInfo.tokenRange.start}, ${availabilityInfo.tokenRange.end}]
                  |
                  |Unreachable replicas : ${availabilityInfo.unreachableEndpoints.mkString("['", "', '", "']")}
                  |Keyspace : ${availabilityInfo.keyspace}
                  |Tested Consistency Level : ${availabilityInfo.consistencyLevel}
                  |Endpoints details : ${availabilityInfo.tokenRange.endpointDetails}
                """.stripMargin

        stream.publish(Notification(title, message))
    }

    nextReact = System.currentTimeMillis + FREQUENCY

    { }
  }
}
