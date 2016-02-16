package io.cats.agent.sentinels

import java.lang.management.OperatingSystemMXBean
import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import com.typesafe.config.Config
import io.cats.agent.Constants._
import io.cats.agent.bean.{StorageSpaceInfo, Notification}
import io.cats.agent.util.{JmxClient, HostnameProvider}

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.Try

class StorageSpaceSentinel(jmxAccess: JmxClient, handler: ActorRef, override val conf: Config) extends Sentinel[Array[StorageSpaceInfo]] {

  private val dataThreshold = conf.getDouble(CONF_THRESHOLD)
  private val commitlogThreshold = conf.getDouble(CONF_COMMIT_LOG_THRESHOLD)

  private var nextDataReact = System.currentTimeMillis
  private var nextCommitlogReact = System.currentTimeMillis

  private val FREQUENCY = Try(conf.getDuration(CONF_FREQUENCY, TimeUnit.MILLISECONDS)).getOrElse(FiniteDuration(4, TimeUnit.HOURS).toMillis)

  override def analyze(): Option[Array[StorageSpaceInfo]] = {
    val storageInfo = jmxAccess.getStorageSpaceInformation()
    val alerts = storageInfo.filter(si => !si.commitLog && ((si.availableSpace * 100 / si.totalSpace) < dataThreshold) && (System.currentTimeMillis >= nextDataReact)) ++ storageInfo.filter(si => si.commitLog && ((si.availableSpace * 100 / si.totalSpace) < commitlogThreshold) && (System.currentTimeMillis >= nextCommitlogReact))
    if (alerts.isEmpty) None else Some(alerts)
  }

  override def react(info: Array[StorageSpaceInfo]): Unit = {
    val messageHeader =
      s"""Cassandra Node ${HostnameProvider.hostname} needs additional disk space.
         |Check the used space.
         |Here is some tips :
         |- Some Snapshots may have to be removed (nodetool clearsnapshot)
         |- If you add a node recently, you may have to 'clean' some partition (nodetool cleanup)
         |- You may have to increase the disk space or add some nodes.""".stripMargin

    val messageBody = info.foldLeft(messageHeader)((acc: String, currentInfo : StorageSpaceInfo) => acc +
      s"""
         |
         | path            : ${currentInfo.path} (commitlog: ${currentInfo.commitLog})
         | Used Space      : ${currentInfo.usedSpace/(1024*1024)} MB ( ${currentInfo.usedSpace * 100 / currentInfo.totalSpace}%)
         | Available Space : ${currentInfo.availableSpace/(1024*1024)} MB ( ${currentInfo.availableSpace * 100 / currentInfo.totalSpace}%)
         | Total Space     : ${currentInfo.totalSpace/(1024*1024)} MB
       """.stripMargin)

    handler ! Notification(title, messageBody)

    info.foreach { storageInfo =>
      if (storageInfo.commitLog)
        nextCommitlogReact = System.currentTimeMillis() + FREQUENCY
      else
        nextDataReact = System.currentTimeMillis + FREQUENCY
    }
  }
}
