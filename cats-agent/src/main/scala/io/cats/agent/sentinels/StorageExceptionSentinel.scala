package io.cats.agent.sentinels

import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import com.typesafe.config.Config
import io.cats.agent.Constants._
import io.cats.agent.bean.{Notification, StorageSpaceInfo}
import io.cats.agent.{HostnameProvider, JmxClient}

import scala.concurrent.duration.FiniteDuration
import scala.util.Try

class StorageExceptionSentinel(jmxAccess: JmxClient, handler: ActorRef, override val conf: Config) extends Sentinel[Long] {

  private var nextReact = System.currentTimeMillis
  private var previousValue : Long = 0
  private val FREQUENCY = Try(conf.getDuration(CONF_FREQUENCY, TimeUnit.MILLISECONDS)).getOrElse(FiniteDuration(5, TimeUnit.MINUTES).toMillis)

  override def analyze(): Option[Long] = {
    val storageExc = jmxAccess.getStorageMetricExceptions()
    val exception = storageExc - previousValue
    if( exception > 0 && (System.currentTimeMillis >= nextReact)) {
      previousValue = storageExc
      Some(exception)
    } else None
  }

  override def react(info: Long): Unit = {
    val messageBody = s"""Cassandra Node ${HostnameProvider.hostname} has some storage exceptions.
         |
         | exceptions : ${info}
         |
         | You should check cassandra logs (see /var/log/cassandra/system.log or custom location).
       """.stripMargin

    handler ! Notification(title, messageBody)

    nextReact = System.currentTimeMillis + FREQUENCY
  }
}
