package io.cats.agent.sentinels

import com.typesafe.config.Config
import io.cats.agent.Constants._
import io.cats.agent.bean.{CheckMetrics, JmxNotification}
import io.cats.agent.util.CommonLoggerFactory._
import io.cats.agent.util.HostnameProvider

import scala.collection.JavaConverters._
/**
 * Created by eric on 24/02/16.
 *
 * This class add a notification listener to the JMX MBeanServer in order to follow the progress of some action (like the repair)
 *
 */
class InternalNotificationsSentinel(override val conf: Config) extends Sentinel {


  // TODO use the ProgressEventType enum when the v2.1 will be depreciated
  val ERROR_STATUS = 2
  val ABORT_STATUS = 3


  this.context.system.eventStream.subscribe(this.self, classOf[JmxNotification])

  override def processProtocolElement: Receive = {
    case CheckMetrics() => {} // nothing to do on checkMetrics
    case JmxNotification(notification) =>
      val data = notification.getUserData.asInstanceOf[java.util.HashMap[String, Int]].asScala
      if (commonLogger.isDebugEnabled) {
        commonLogger.debug(this, "Receive JMX notification=<{}>", data.toString())
      }
      if (data("type") == ERROR_STATUS ) sendErrorStatus(notification, data)
      else if (data("type") == ABORT_STATUS && conf.getDouble(CONF_CASSANDRA_VERSION) > 2.1) sendAbortStatus(notification, data)
  }

  def sendErrorStatus(notification: javax.management.Notification, data: collection.mutable.Map[String, Int]) = sendStatus(notification, data, "A progess has failed ")
  def sendAbortStatus(notification: javax.management.Notification, data: collection.mutable.Map[String, Int]) = sendStatus(notification, data, "A progess was aborted ")

  def sendStatus(notification: javax.management.Notification, data: collection.mutable.Map[String, Int], msg : String) = {
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