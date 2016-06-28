package io.cats.agent.sentinels

import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import akka.event.EventStream
import com.typesafe.config.Config
import io.cats.agent.Constants._
import io.cats.agent.bean._
import io.cats.agent.util.{JmxClient, HostnameProvider}

import scala.concurrent.duration.FiniteDuration
import scala.util.Try

class StorageExceptionSentinel(val metricsProvider: ActorRef, override val conf: Config) extends Sentinel {

  private var nextReact = System.currentTimeMillis
  private var previousValue : Long = 0
  private val FREQUENCY = Try(conf.getDuration(CONF_FREQUENCY, TimeUnit.MILLISECONDS)).getOrElse(FiniteDuration(5, TimeUnit.MINUTES).toMillis)

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
