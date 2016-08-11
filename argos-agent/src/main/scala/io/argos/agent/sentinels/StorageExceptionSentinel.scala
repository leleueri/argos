package io.argos.agent.sentinels

import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import akka.event.EventStream
import com.typesafe.config.Config
import io.argos.agent.Constants
import Constants._
import io.argos.agent.bean.{MetricsResponse, MetricsRequest, ActorProtocol}
import io.argos.agent.util.HostnameProvider
import io.argos.agent.bean._
import io.argos.agent.util.JmxClient

import scala.concurrent.duration.FiniteDuration
import scala.util.Try

class StorageExceptionSentinel(val metricsProvider: ActorRef, override val conf: Config) extends Sentinel {

  private var previousValue : Long = 0

  override def processProtocolElement: Receive = {

    case CheckMetrics() => if (System.currentTimeMillis >= nextReact) metricsProvider ! MetricsRequest(ActorProtocol.ACTION_CHECK_STORAGE_EXCEPTION, "")

    case metrics: MetricsResponse[Long] if metrics.value.isDefined => {
      val storageExc = metrics.value.get
      val exception = storageExc - previousValue
      if (exception > 0 && (System.currentTimeMillis >= nextReact)) {
        previousValue = storageExc
        react(exception)
      } else None
    }
  }

  def react(info: Long): Unit = {
    val message = s"""Cassandra Node ${HostnameProvider.hostname} has some storage exceptions.
         |
         | exceptions : ${info}
         |
         | You should check cassandra logs (see /var/log/cassandra/system.log or custom location).
       """.stripMargin

    context.system.eventStream.publish(buildNotification(message))

    nextReact = System.currentTimeMillis + FREQUENCY
  }
}
