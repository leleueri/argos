package io.argos.agent.sentinels


import akka.actor.ActorRef
import io.argos.agent.SentinelConfiguration
import io.argos.agent.bean.{ActorProtocol, MetricsRequest, MetricsResponse, StorageSpaceInfo}
import io.argos.agent.util.HostnameProvider
import io.argos.agent.bean._

class StorageSpaceSentinel(val metricsProvider: ActorRef, override val conf: SentinelConfiguration) extends Sentinel {

  private lazy val dataThreshold = conf.threshold
  private lazy val commitlogThreshold = conf.commitLogThreshold

  private var nextDataReact = System.currentTimeMillis
  private var nextCommitlogReact = System.currentTimeMillis

  override def processProtocolElement: Receive = {

    case CheckMetrics() => if (System.currentTimeMillis >= nextDataReact || System.currentTimeMillis >= nextCommitlogReact) metricsProvider ! MetricsRequest(ActorProtocol.ACTION_CHECK_STORAGE_SPACE, "")

    case metrics: MetricsResponse[Array[StorageSpaceInfo]] if metrics.value.isDefined => {
      val storageInfo = metrics.value.get
      val alerts = storageInfo.filter(si => !si.commitLog && ((si.availableSpace * 100 / si.totalSpace) < dataThreshold) && (System.currentTimeMillis >= nextDataReact)) ++ storageInfo.filter(si => si.commitLog && ((si.availableSpace * 100 / si.totalSpace) < commitlogThreshold) && (System.currentTimeMillis >= nextCommitlogReact))
      if (!alerts.isEmpty) react(alerts)
    }
  }

  def react(info: Array[StorageSpaceInfo]): Unit = {
    val messageHeader =
      s"""Cassandra Node ${HostnameProvider.hostname} needs additional disk space.
         |
         |Check the used space.
         |
         |Here is some tips :
         |- Some Snapshots may have to be removed (nodetool clearsnapshot)
         |- If you added a node recently, you may have to 'clean' some partition (nodetool cleanup)
         |- You may have to increase the disk space or add some nodes.""".stripMargin

    val message = info.foldLeft(messageHeader)((acc: String, currentInfo : StorageSpaceInfo) => acc +
      s"""
         |
         | path            : ${currentInfo.path} (commitlog: ${currentInfo.commitLog})
         | Used Space      : ${currentInfo.usedSpace/(1024*1024)} MB ( ${currentInfo.usedSpace * 100 / currentInfo.totalSpace}%)
         | Available Space : ${currentInfo.availableSpace/(1024*1024)} MB ( ${currentInfo.availableSpace * 100 / currentInfo.totalSpace}%)
         | Total Space     : ${currentInfo.totalSpace/(1024*1024)} MB
       """.stripMargin)

    context.system.eventStream.publish(buildNotification(conf.messageHeader.map(h => h + " \n\n--####--\n\n" + message).getOrElse(message)))

    info.foreach { storageInfo =>
      if (storageInfo.commitLog)
        nextCommitlogReact = System.currentTimeMillis() + conf.frequency
      else
        nextDataReact = System.currentTimeMillis + conf.frequency
    }
  }
}
