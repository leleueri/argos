package io.cats.agent.bean

/**
 * Created by eric on 27/06/16.
 */

object ActorProtocol {
  val ACTION_CHECK_DROPPED_MESSAGES = "DROPPED_MESSAGES"
  val ACTION_CHECK_STAGE = "STAGE"
  val ACTION_CHECK_INTERNAL_STAGE = "INTERNAL_STAGE"
  val ACTION_CHECK_STORAGE_SPACE = "STORAGE_SPACE"
  val ACTION_CHECK_STORAGE_HINTS = "STORAGE_HINTS"
  val ACTION_CHECK_STORAGE_EXCEPTION = "STORAGE_EXC"
}

case class MetricsRequest(metricsName: String, param: String)
case class MetricsResponse[T] (metricsName: String, value: Option[T])

case class CheckMetrics()

case class CheckNodeStatus()
case class NodeStatus(status: String)

case class JmxNotification(notification: javax.management.Notification)

case class AvailabilityRequirements(keyspace: String, consistencyLevel: String)
case class AvailabilityIssue(issues: List[Availability])