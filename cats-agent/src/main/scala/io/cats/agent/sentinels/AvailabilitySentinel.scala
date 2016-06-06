package io.cats.agent.sentinels

import java.io.{ByteArrayInputStream, ObjectInputStream}
import java.util.concurrent.TimeUnit

import akka.event.EventStream
import com.typesafe.config.Config
import io.cats.agent.Constants._
import io.cats.agent.bean.{CatsEndpointDetails, CatsTokenRange, Notification, Availability}
import io.cats.agent.util.{HostnameProvider, JmxClient}
import org.apache.cassandra.thrift.TokenRange
import org.apache.cassandra.tools.NodeProbe

import scala.concurrent.duration.FiniteDuration
import scala.util.Try

import scala.collection.JavaConverters._

class AvailabilitySentinel(nodeProbe: NodeProbe, stream: EventStream, override val conf: Config) extends Sentinel[List[Availability]] {

  private var nextReact = System.currentTimeMillis
  private val FREQUENCY = Try(conf.getDuration(CONF_FREQUENCY, TimeUnit.MILLISECONDS)).getOrElse(FiniteDuration(5, TimeUnit.MINUTES).toMillis)

  override def analyze(): Option[List[Availability]] = {
    val thisEndpoint = nodeProbe.getEndpoint

    val issuePerKeyspace = for {
      pair <- conf.getConfigList(CONF_KEYSPACES).asScala
      ks = pair.getString(CONF_KEYSPACE_NAME)
      cl = pair.getString(CONF_CONSISTENCY_LEVEL)
      result = nodeProbe.describeRing(ks).asScala.map(interpretTokenRangeString(_))
        .filter(_.endpoints.contains(thisEndpoint)) // compute only replicas set containing the current node.
        .filterNot(mayReachCL(_, cl)) // keep endpoints with CL issue
        .map(e => Availability(ks, cl, e.endpoints.toArray.intersect(nodeProbe.getUnreachableNodes.asScala))).toList
    } yield result

    val result = issuePerKeyspace.flatten

    if (result.isEmpty || (!result.isEmpty && System.currentTimeMillis <= nextReact)) None else Some(result.toList)
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

  private def mayReachCL(range: CatsTokenRange, cl: String) : Boolean = {
    val unreachNodes = range.endpoints.intersect(nodeProbe.getUnreachableNodes.asScala)
    val maxUnreachNodes = cl.toLowerCase match { // TODO manage multiDC
      case "one" => range.endpoints.length - 1 // local_one
      case "two" => range.endpoints.length - 2
      case "three" => range.endpoints.length - 3
      case "all" => 0
      case _ => (range.endpoints.length - (1 + (range.endpoints.length/2))) // QUORUM / LOCAL_QUORUM / LOCAL_ONE
    }
    (unreachNodes.length <= maxUnreachNodes)
  }
  
  override def react(info:  List[Availability]): Unit = {

    info.foreach {
      availabilityInfo =>
        val message = s"""Cassandra Node ${HostnameProvider.hostname} may have consistency issue
                  |
                  |${availabilityInfo.unreachableEndpoints.length} replicas are unreachable
                  |
                  |Unreachable replicas : ${availabilityInfo.unreachableEndpoints.mkString("['", "', '", "']")}
                  |Keyspace : ${availabilityInfo.keyspace}
                  |Consistency Level : ${availabilityInfo.consistencyLevel}
                  |
                """.stripMargin

        stream.publish(Notification(title, message))
    }

    nextReact = System.currentTimeMillis + FREQUENCY

    { }
  }
}
