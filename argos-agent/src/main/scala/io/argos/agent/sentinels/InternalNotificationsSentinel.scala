package io.argos.agent.sentinels

import java.util
import javax.management.Notification

import com.typesafe.config.Config
import io.argos.agent.{Constants, SentinelConfiguration}
import io.argos.agent.bean.{CheckMetrics, JmxNotification}
import io.argos.agent.util.{CassandraVersion, CommonLoggerFactory, HostnameProvider}
import Constants._
import CommonLoggerFactory._
import org.apache.cassandra.streaming.{StreamEvent, StreamManagerMBean}

import scala.collection.JavaConverters._

/**
  * Created by eric on 24/02/16.
  *
  * This class add a notification listener to the JMX MBeanServer in order to follow the progress of some action (like the repair)
  *
  */
class InternalNotificationsSentinel(override val conf: SentinelConfiguration) extends Sentinel {

  // TODO use the ProgressEventType enum when the v2.1 will be depreciated
  val ERROR_STATUS = 2
  val ABORT_STATUS = 3

  this.context.system.eventStream.subscribe(this.self, classOf[JmxNotification])

  override def processProtocolElement: Receive = {
    case CheckMetrics() => {} // nothing to do on checkMetrics
    case JmxNotification(notification) if notification.getSource().equals(StreamManagerMBean.OBJECT_NAME) =>
      manageSteamNotification(notification)
    case JmxNotification(notification) if notification.getSource().toString.startsWith("bootstrap") =>
      manageNotificationWithHashMap(notification)
    case JmxNotification(notification) if notification.getSource().toString.startsWith("repair") =>
      manageRepairNotification(notification)
    case JmxNotification(unknownNotif) => log.debug("Unknown JMXNotification : source={}, type={}", unknownNotif.getSource, unknownNotif.getType);
  }

  private def manageSteamNotification(notification: Notification): Unit = {
    if ((classOf[StreamEvent].getCanonicalName + ".failure").equals(notification.getType())) {

      val message =
        s"""Stream process failed on Cassandra Node ${HostnameProvider.hostname}.
            |
         |stacktrace : ${notification.getUserData}
            |
         |notification : ${notification}
            |
         |""".stripMargin

      context.system.eventStream.publish(buildNotification(message))
    }
  }

  private def manageRepairNotification(notification: Notification): Unit = {
    if (notification.getUserData.isInstanceOf[util.HashMap[String, Int]]) {
      manageNotificationWithHashMap(notification);
    } else if (notification.getUserData.isInstanceOf[Array[Int]]) {

      // array of integer (LegacyJmxNotification)
      val data = notification.getUserData.asInstanceOf[Array[Int]]
      if (commonLogger.isDebugEnabled) {
        commonLogger.debug(this, "Receive JMX notification=<{}> with userData = <{}>", notification.getType, data.toString())
      }

      val status: Int = data(1)
      if (hasFailed(status)) {
        val msg = if (status == ERROR_STATUS) s"${notification.getSource} has failed "
        else if (status == ABORT_STATUS && CassandraVersion.version > 2.1) s"${notification.getSource} was aborted "

        val action = notification.getSource

        val message =
          s"""${msg} for Cassandra Node ${HostnameProvider.hostname}
           |
           |action   : ${action}
           |notification : ${notification}
           |
           |""".stripMargin

        context.system.eventStream.publish(buildNotification(message))
      }
    }
  }

  private def hasFailed(status: Int) : Boolean = (status == ERROR_STATUS || (status == ABORT_STATUS && CassandraVersion.version > 2.1))

  private def manageNotificationWithHashMap(notification: Notification): Unit = {
    val data = notification.getUserData.asInstanceOf[util.HashMap[String, Int]].asScala
    if (commonLogger.isDebugEnabled) {
      commonLogger.debug(this, "Receive JMX notification=<{}> with userData = <{}>", notification.getType, data.toString())
    }

    val status = data("type")
    if (hasFailed(status)) {
      val msg = if (status == ERROR_STATUS) s"${notification.getSource} has failed "
      else if (status == ABORT_STATUS && CassandraVersion.version > 2.1) s"${notification.getSource} was aborted "

      val percent = 100 * data("progressCount") / data("total")
      val action = notification.getSource

      val message =
        s"""${msg} for Cassandra Node ${HostnameProvider.hostname}.
           |
           |action   : ${action}
           |progress : ${percent}%
           |notification : ${notification}
           |
           |""".stripMargin

      context.system.eventStream.publish(buildNotification(message))
    }
  }
}

/*

HERE is the list of notification for a repair command.
"data" is a Java HashMap with :
 - total the number of progressCount
 - progressCount : the current ProgressCount
 - type : the numerical value of the ProgressEventType enum. (org.apache.cassandra.utils.progress.ProgressEventType)
"type" : the type of nofication

 Notifs      : javax.management.Notification[source=repair:8][type=progress][message=Starting repair command #8, repairing keyspace excelsior with repair options (parallelism: parallel, primary range: false, incremental: true, job threads: 1, ColumnFamilies: [], dataCenters: [], hosts: [], # of ranges: 3)]
   .msg  : Starting repair command #8, repairing keyspace excelsior with repair options (parallelism: parallel, primary range: false, incremental: true, job threads: 1, ColumnFamilies: [], dataCenters: [], hosts: [], # of ranges: 3)
   .snum : 40
   .time : 1456261572578
   .type : progress
   .data : {progressCount=0, total=100, type=0}
Notifs      : javax.management.Notification[source=repair:8][type=progress][message=Repair session 44d8d9d0-da71-11e5-bfcc-8f82886fa28e for range (3074457345618258602,-9223372036854775808] finished]
   .msg  : Repair session 44d8d9d0-da71-11e5-bfcc-8f82886fa28e for range (3074457345618258602,-9223372036854775808] finished
   .snum : 41
   .time : 1456261572607
   .type : progress
   .data : {progressCount=4, total=6, type=1}
Notifs      : javax.management.Notification[source=repair:8][type=progress][message=Repair session 44d927f1-da71-11e5-bfcc-8f82886fa28e for range (-9223372036854775808,-3074457345618258603] finished]
   .msg  : Repair session 44d927f1-da71-11e5-bfcc-8f82886fa28e for range (-9223372036854775808,-3074457345618258603] finished
   .snum : 42
   .time : 1456261572617
   .type : progress
   .data : {progressCount=5, total=6, type=1}
Notifs      : javax.management.Notification[source=repair:8][type=progress][message=Repair session 44da3960-da71-11e5-bfcc-8f82886fa28e for range (-3074457345618258603,3074457345618258602] finished]
   .msg  : Repair session 44da3960-da71-11e5-bfcc-8f82886fa28e for range (-3074457345618258603,3074457345618258602] finished
   .snum : 43
   .time : 1456261572639
   .type : progress
   .data : {progressCount=6, total=6, type=1}
Notifs      : javax.management.Notification[source=repair:8][type=progress][message=Repair completed successfully]
   .msg  : Repair completed successfully
   .snum : 44
   .time : 1456261572644
   .type : progress
   .data : {progressCount=6, total=6, type=4}
Notifs      : javax.management.Notification[source=repair:8][type=progress][message=Repair command #8 finished in 0 seconds]
   .msg  : Repair command #8 finished in 0 seconds
   .snum : 45
   .time : 1456261572644
   .type : progress
   .data : {progressCount=6, total=6, type=5}
  */