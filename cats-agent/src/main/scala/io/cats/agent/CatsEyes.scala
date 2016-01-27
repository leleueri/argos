package io.cats.agent

import javax.management._
import javax.management.remote.{JMXConnector, JMXConnectorFactory, JMXServiceURL}
import com.yammer.metrics.reporting.JmxReporter.{GaugeMBean, CounterMBean}
import io.cats.agent.bean.{ThreadPoolStats, DroppedMessageStats}

import scala.collection.JavaConverters._
class CatsEyes(hostname: String, port: Int, user: Option[String] = None, pwd: Option[String] = None) {

  lazy val mbeanServerCnx = createJMXConnection

  lazy val storageMetricsLoad = initStorageMetricLoadProxy(mbeanServerCnx)
  lazy val storageMetricsExceptions = initStorageMetricExceptionsProxy(mbeanServerCnx)
  lazy val storageMetricsTotalHints = initStorageMetricTotalHintsProxy(mbeanServerCnx)
  lazy val storageMetricsTotalHintsInProgress = initStorageMetricTotalHintsInProgessProxy(mbeanServerCnx)


  def createJMXConnection() : MBeanServerConnection = {
    // TODO manage exception
    val url = new JMXServiceURL(s"service:jmx:rmi:///jndi/rmi://${hostname}:${port}/jmxrmi")
    val connector = user match {
      case Some(login) => JMXConnectorFactory.connect(url, Map(JMXConnector.CREDENTIALS -> Array(login, pwd.get)).asJava)
      case None => JMXConnectorFactory.connect(url)
    }

    // TODO addition of a listener to monitor some action like repair : connector.addlistener...

    connector.getMBeanServerConnection
  }


  private def initStorageMetricLoadProxy(mbsc : MBeanServerConnection) = initStorageMetricProxy(mbsc, "Load")
  private def initStorageMetricExceptionsProxy(mbsc : MBeanServerConnection) =  initStorageMetricProxy(mbsc, "Exceptions")
  private def initStorageMetricTotalHintsProxy(mbsc : MBeanServerConnection) =  initStorageMetricProxy(mbsc, "TotalHints")
  private def initStorageMetricTotalHintsInProgessProxy(mbsc : MBeanServerConnection) =  initStorageMetricProxy(mbsc, "TotalHintsInProgress")
  private def initStorageMetricProxy(mbsc : MBeanServerConnection, name: String) = JMX.newMBeanProxy(mbsc, new ObjectName(s"org.apache.cassandra.metrics:type=Storage,name=${name}"), classOf[CounterMBean], true)


  def getCounterMutationStageValues() = initStageValue("CounterMutationStage")
  def getMutationStageValues() = initStageValue("MutationStage")
  def getReadRepairStageValues() = initStageValue("ReadRepairStage")
  def getReadStageValues() = initStageValue("ReadStage")
  def getRequestResponseStageValues() = initStageValue("RequestResponseStage")
  private def initStageValue(stage: String) = initThreadPoolStageValues(stage, "request")


  def getMemtableFlushWriterValues() = initInternalStageValue("MemtableFlushWriter")
  def getCompactionExecutorValues() = initInternalStageValue("CompactionExecutor")
  def getGossipStageValues() = initInternalStageValue("GossipStage")
  def getInternalResponseStageValues() = initInternalStageValue("InternalResponseStage")
  private def initInternalStageValue(stage: String) = initThreadPoolStageValues(stage, "internal")

  // TODO Heap usage & GC stats
  // TODO READ/WRITE Latency ==> see Aaron Morton video CassSubmit 2015
  // TODO READ/WRITE Throughput ==> see Aaron Morton video CassSubmit 2015

  private def initThreadPoolStageValues(stage: String, path: String) : ThreadPoolStats =  {
    val active = mbeanServerCnx.getAttribute(new ObjectName(s"org.apache.cassandra.metrics:type=ThreadPools,path=${path},scope=${stage},name=ActiveTasks"),"Value").toString.toInt
    val completed = mbeanServerCnx.getAttribute(new ObjectName(s"org.apache.cassandra.metrics:type=ThreadPools,path=${path},scope=${stage},name=CompletedTasks"), "Value").toString.toInt
    val currentlyBlocked = mbeanServerCnx.getAttribute(new ObjectName(s"org.apache.cassandra.metrics:type=ThreadPools,path=${path},scope=${stage},name=CurrentlyBlockedTasks"), "Count").toString.toLong
    val poolSize = mbeanServerCnx.getAttribute(new ObjectName(s"org.apache.cassandra.metrics:type=ThreadPools,path=${path},scope=${stage},name=MaxPoolSize"), "Value").toString.toInt
    val pending = mbeanServerCnx.getAttribute(new ObjectName(s"org.apache.cassandra.metrics:type=ThreadPools,path=${path},scope=${stage},name=PendingTasks"), "Value").toString.toInt
    val totalBlocked = mbeanServerCnx.getAttribute(new ObjectName(s"org.apache.cassandra.metrics:type=ThreadPools,path=${path},scope=${stage},name=TotalBlockedTasks"), "Count").toString.toLong

    ThreadPoolStats(`type` = stage,
      activeTasks = active,
      completedTasks = completed,
      currentBlockedTasks = currentlyBlocked,
      maxPoolSize = poolSize,
      pendingTasks = pending,
      totalBlockedTasks = totalBlocked)
  }

  /**
    * @return Storage Load in Bytes
    */
  def readStorageLoad() = storageMetricsLoad.getCount


  def getCounterMutationDroppedMessage() = initDroppedMessages("COUNTER_MUTATION")
  def getMutationDroppedMessage() = initDroppedMessages("MUTATION")
  def getPagedRangeDroppedMessage() = initDroppedMessages("PAGED_RANGE")
  def getRangeSliceDroppedMessage() = initDroppedMessages("RANGE_SLICE")
  def getReadRepairDroppedMessage() = initDroppedMessages("READ_REPAIR")
  def getReadDroppedMessage() = initDroppedMessages("READ")
  def getRequestResponseDroppedMessage() = initDroppedMessages("REQUEST_RESPONSE")


  private def initDroppedMessages(scope: String) : DroppedMessageStats =  {
    val values = mbeanServerCnx.getAttributes(
      new ObjectName(s"org.apache.cassandra.metrics:type=DroppedMessage,scope=${scope},name=Dropped"),
      Array("Count", "FifteenMinuteRate", "FiveMinuteRate", "MeanRate", "OneMinuteRate")).asScala

    DroppedMessageStats(scope,
      values(0).toString.toLong,
      values(1).toString.toDouble,
      values(2).toString.toDouble,
      values(3).toString.toDouble,
      values(4).toString.toDouble)
  }
}

object CatsEyes {

  val DEFAULT_HOSTNAME = "127.0.0.1"
  val DEFAULT_JMX_PORT = 7199

  def apply() : CatsEyes = {
    new CatsEyes(DEFAULT_HOSTNAME, DEFAULT_JMX_PORT)
  }

  def apply(hostname: String, port: Int) : CatsEyes = new CatsEyes(hostname, port)

  def apply(hostname: String, port: Int, user: String, pwd: String) : CatsEyes = new CatsEyes(hostname, port, Some(user), Some(pwd))

}
