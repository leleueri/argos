package io.argos.agent.util

import java.nio.file.Paths
import javax.management._
import javax.management.remote.{JMXConnector, JMXConnectorFactory, JMXServiceURL}

import akka.actor.ActorSystem
import io.argos.agent.bean._
import org.apache.cassandra.metrics.CassandraMetricsRegistry.{JmxCounterMBean, JmxGaugeMBean}
import org.apache.cassandra.service.StorageServiceMBean

import scala.collection.JavaConverters._
import scala.util.Try
import scala.collection.mutable.Map
/**
  * Eyes of the ArgosAgent... :)
  * This class gets JMX information from the Cassandra node monitored by the Argos Agent.
  *
  * @param hostname
  * @param port
  * @param user
  * @param pwd
  */
abstract class JmxClient(hostname: String, port: Int, user: Option[String] = None, pwd: Option[String] = None) {

  var connector = createConnection()
  var mbeanServerCnx = createMBeanServer

  var storageServiceProxy = initStorageServiceProxy()

  val listeners : Map[ObjectName, NotificationListener] = Map()

  private def createConnection() : JMXConnector = {
    val url = new JMXServiceURL(s"service:jmx:rmi:///jndi/rmi://${hostname}:${port}/jmxrmi")
    (user, pwd) match {
      case (Some(login), Some(pass)) => JMXConnectorFactory.connect(url, Map(JMXConnector.CREDENTIALS -> Array(login, pass)).asJava)
      case _ => JMXConnectorFactory.connect(url)
    }
  }

  private def createMBeanServer() : MBeanServerConnection = {
    connector.getMBeanServerConnection
  }

  private def initStorageServiceProxy() = JMX.newMBeanProxy(mbeanServerCnx, new ObjectName("org.apache.cassandra.db:type=StorageService"), classOf[StorageServiceMBean])

  def addNotificationListener(objectName: ObjectName, listener: NotificationListener) : Unit = {
    listeners += (objectName -> listener)
    mbeanServerCnx.addNotificationListener(objectName,listener, null, null)
  }

  def reconnect() = {
    Try(connector.close())
    connector = createConnection()
    mbeanServerCnx = createMBeanServer()
    storageServiceProxy = initStorageServiceProxy()
    listeners.foreach {
      case (objName, listener) =>  mbeanServerCnx.addNotificationListener (objName, listener, null, null)
     }
  }

  def takeSnapshot(name : String, keyspace: String, table: Option[String], flush: Boolean) : Unit = {
    table match {
        // FIXME : Beware in the lastest Cassandra version these methods are deprecated
    case None => {
      if (flush) storageServiceProxy.forceKeyspaceFlush(keyspace)
      storageServiceProxy.takeSnapshot(name, keyspace)
      }
    case Some(table) => {
      if (flush) storageServiceProxy.forceKeyspaceFlush(keyspace, table)
      storageServiceProxy.takeTableSnapshot(keyspace, table, name)
      }
    }
  }

  def getStorageSpaceInformation() : Array[StorageSpaceInfo] = {
    def analysePath(path: String, commitLog : Boolean = false) : StorageSpaceInfo = {
      val file = Paths.get(path).toFile
      val totalSpace = file.getTotalSpace(); // total disk space in bytes.
      val freeSpace = file.getFreeSpace(); //unallocated / free disk space in bytes.

      StorageSpaceInfo(path, (totalSpace - freeSpace), freeSpace, totalSpace, commitLog)
    }

    val commitLogPath = storageServiceProxy.getCommitLogLocation
    val dataDirectories = storageServiceProxy.getAllDataFileLocations
    dataDirectories.foldLeft(Array(analysePath(commitLogPath, true)))((arr, path) => arr :+ analysePath(path) )
  }

  /**
    * @return Storage load in Bytes (space used by SSTables)
    */
  def getStorageMetricLoad() = initStorageMetric("Load")
  /**
    * @return Number of storage exceptions
    */
  def getStorageMetricExceptions() =  initStorageMetric("Exceptions")
  /**
    * @return Number of Hints to replay
    */
  def getStorageMetricTotalHints() =  initStorageMetric("TotalHints")
  /**
    * @return Number of Hints that are replaying
    */
  def getStorageMetricTotalHintsInProgess() =  initStorageMetric("TotalHintsInProgress")

  def getStorageHints() : Tuple2[Long, Long] = (getStorageMetricTotalHints, getStorageMetricTotalHintsInProgess())

  private def initStorageMetric(name: String) = mbeanServerCnx.getAttribute(new ObjectName(s"org.apache.cassandra.metrics:type=Storage,name=${name}"),"Count").toString.toLong

  /**
    * @return Information about the COUNTER_MUTATION ThreadPool
    */
  def getCounterMutationStageValues() = getStageValue("CounterMutationStage")
  /**
    * @return Information about the MUTATION ThreadPool
    */
  def getMutationStageValues() = getStageValue("MutationStage")
  /**
    * @return Information about the READ_REPAIR ThreadPool
    */
  def getReadRepairStageValues() = getStageValue("ReadRepairStage")
  /**
    * @return Information about the READ ThreadPool
    */
  def getReadStageValues() = getStageValue("ReadStage")
  /**
    * @return Information about the REQUEST_RESPONSE ThreadPool
    */
  def getRequestResponseStageValues() = getStageValue("RequestResponseStage")

  def getStageValue(stage: String) = initThreadPoolStageValues(stage, "request")

  /**
    * @return Information about the FlushWriter ThreadPool
    */
  def getMemtableFlushWriterValues() = getInternalStageValue("MemtableFlushWriter")
  /**
    * @return Information about the Compaction ThreadPool
    */
  def getCompactionExecutorValues() = getInternalStageValue("CompactionExecutor")
  /**
    * @return Information about the Gossip ThreadPool
    */
  def getGossipStageValues() = getInternalStageValue("GossipStage")
  /**
    * @return Information about the InternalResponse ThreadPool
    */
  def getInternalResponseStageValues() = getInternalStageValue("InternalResponseStage")

  def getInternalStageValue(stage: String) = initThreadPoolStageValues(stage, "internal")

  private def initThreadPoolStageValues(stage: String, path: String) : ThreadPoolStats =  {
    val active = mbeanServerCnx.getAttribute(new ObjectName(s"org.apache.cassandra.metrics:type=ThreadPools,path=${path},scope=${stage},name=ActiveTasks"),"Value").toString.toLong
    val completed = mbeanServerCnx.getAttribute(new ObjectName(s"org.apache.cassandra.metrics:type=ThreadPools,path=${path},scope=${stage},name=CompletedTasks"), "Value").toString.toLong
    val poolSize = mbeanServerCnx.getAttribute(new ObjectName(s"org.apache.cassandra.metrics:type=ThreadPools,path=${path},scope=${stage},name=MaxPoolSize"), "Value").toString.toLong
    val pending = mbeanServerCnx.getAttribute(new ObjectName(s"org.apache.cassandra.metrics:type=ThreadPools,path=${path},scope=${stage},name=PendingTasks"), "Value").toString.toLong

    val currentlyBlocked = mbeanServerCnx.getAttribute(new ObjectName(s"org.apache.cassandra.metrics:type=ThreadPools,path=${path},scope=${stage},name=CurrentlyBlockedTasks"), "Count").toString.toLong
    val totalBlocked = mbeanServerCnx.getAttribute(new ObjectName(s"org.apache.cassandra.metrics:type=ThreadPools,path=${path},scope=${stage},name=TotalBlockedTasks"),  "Count").toString.toLong

    ThreadPoolStats(`type` = stage,
      activeTasks = active,
      completedTasks = completed,
      currentBlockedTasks = currentlyBlocked,
      maxPoolSize = poolSize,
      pendingTasks = pending,
      totalBlockedTasks = -totalBlocked)
  }

  /**
    * @return Information about the COUNTER_MUTATION dropped messages
    */
  def getCounterMutationDroppedMessage() = getDroppedMessages("COUNTER_MUTATION")
  /**
    * @return Information about the MUTATION dropped messages
    */
  def getMutationDroppedMessage() = getDroppedMessages("MUTATION")
  /**
    * @return Information about the PAGED_RANGE dropped messages
    */
  def getPagedRangeDroppedMessage() = getDroppedMessages("PAGED_RANGE")
  /**
    * @return Information about the RANGE_SLICE dropped messages
    */
  def getRangeSliceDroppedMessage() = getDroppedMessages("RANGE_SLICE")
  /**
    * @return Information about the READ_REPAIR dropped messages
    */
  def getReadRepairDroppedMessage() = getDroppedMessages("READ_REPAIR")
  /**
    * @return Information about the READ dropped messages
    */
  def getReadDroppedMessage() = getDroppedMessages("READ")
  /**
    * @return Information about the REQUEST_RESPONSE dropped messages
    */
  def getRequestResponseDroppedMessage() = getDroppedMessages("REQUEST_RESPONSE")

  def getDroppedMessages(scope: String) : DroppedMessageStats =  {
    val attrNames = Array("Count", "FifteenMinuteRate", "FiveMinuteRate", "MeanRate", "OneMinuteRate")
    val values = mbeanServerCnx.getAttributes(new ObjectName(s"org.apache.cassandra.metrics:type=DroppedMessage,scope=${scope},name=Dropped"), attrNames)

    DroppedMessageStats(scope,
      values.get(0).asInstanceOf[Attribute].getValue.toString.toLong,
      values.get(1).asInstanceOf[Attribute].getValue.toString.toDouble,
      values.get(2).asInstanceOf[Attribute].getValue.toString.toDouble,
      values.get(3).asInstanceOf[Attribute].getValue.toString.toDouble,
      values.get(4).asInstanceOf[Attribute].getValue.toString.toDouble)
  }

  def getReadRepairs(name: String) : ReadRepairStats =  {
    val attrNames = Array("Count", "FifteenMinuteRate", "FiveMinuteRate", "MeanRate", "OneMinuteRate")
    val values = mbeanServerCnx.getAttributes(new ObjectName(s"org.apache.cassandra.metrics:type=ReadRepair,name=${name}"), attrNames)

    ReadRepairStats(name,
      values.get(0).asInstanceOf[Attribute].getValue.toString.toLong,
      values.get(1).asInstanceOf[Attribute].getValue.toString.toDouble,
      values.get(2).asInstanceOf[Attribute].getValue.toString.toDouble,
      values.get(3).asInstanceOf[Attribute].getValue.toString.toDouble,
      values.get(4).asInstanceOf[Attribute].getValue.toString.toDouble)
  }

  def getConnectionTimeouts(  ) : ConnectionTimeoutStats =  {
    val attrNames = Array("Count", "FifteenMinuteRate", "FiveMinuteRate", "MeanRate", "OneMinuteRate")
    val values = mbeanServerCnx.getAttributes(new ObjectName("org.apache.cassandra.metrics:type=Connection,name=TotalTimeouts"), attrNames)

    ConnectionTimeoutStats(
      values.get(0).asInstanceOf[Attribute].getValue.toString.toLong,
      values.get(1).asInstanceOf[Attribute].getValue.toString.toDouble,
      values.get(2).asInstanceOf[Attribute].getValue.toString.toDouble,
      values.get(3).asInstanceOf[Attribute].getValue.toString.toDouble,
      values.get(4).asInstanceOf[Attribute].getValue.toString.toDouble)
  }


  def getGCInspector(  ) : GCState =  {
    val values = mbeanServerCnx.getAttribute(new ObjectName("org.apache.cassandra.service:type=GCInspector"), "AndResetStats")

    val dbl = values.asInstanceOf[Array[Double]]

    if (dbl.size < 7) // Cassandra 2.1 only have 6 entries (the getAllocatedDirectMemory entry is missing)
      GCState(dbl(0),dbl(1),dbl(2),dbl(3),dbl(4),dbl(5), -1)
    else
      GCState(dbl(0),dbl(1),dbl(2),dbl(3),dbl(4),dbl(5),dbl(6))
  }

  def getJmxAttrValue(name: String, attr: String) = JmxAttrValue(name, attr, mbeanServerCnx.getAttribute(new ObjectName(name),attr).toString.toDouble)
}



class JmxClientCassandra21(hostname: String, port: Int, user: Option[String] = None, pwd: Option[String] = None) extends JmxClient(hostname, port, user, pwd)  {

  override def getStageValue(stage: String) = {
    stage match {
      case "RequestResponseStage" => initThreadPoolStageValues21 (stage, "request")
      case "ReadStage" => initThreadPoolStageValues21 (stage, "request")
      case "MutationStage" => initThreadPoolStageValues21 (stage, "request")
      case "CounterMutationStage" => initThreadPoolStageValues21 (stage, "request")
      case _ => super.getStageValue(stage)
    }
  }

  private def initThreadPoolStageValues21(stage: String, path: String) : ThreadPoolStats =  {
    val active = mbeanServerCnx.getAttribute(new ObjectName(s"org.apache.cassandra.metrics:type=ThreadPools,path=${path},scope=${stage},name=ActiveTasks"),"Value").toString.toLong
    val completed = mbeanServerCnx.getAttribute(new ObjectName(s"org.apache.cassandra.metrics:type=ThreadPools,path=${path},scope=${stage},name=CompletedTasks"), "Value").toString.toLong
    val poolSize = mbeanServerCnx.getAttribute(new ObjectName(s"org.apache.cassandra.metrics:type=ThreadPools,path=${path},scope=${stage},name=MaxPoolSize"), "Value").toString.toLong
    val pending = mbeanServerCnx.getAttribute(new ObjectName(s"org.apache.cassandra.metrics:type=ThreadPools,path=${path},scope=${stage},name=PendingTasks"), "Value").toString.toLong

    val currentlyBlocked = mbeanServerCnx.getAttribute(new ObjectName(s"org.apache.cassandra.metrics:type=ThreadPools,path=${path},scope=${stage},name=CurrentlyBlockedTasks"), "Value" ).toString.toLong
    val totalBlocked = mbeanServerCnx.getAttribute(new ObjectName(s"org.apache.cassandra.metrics:type=ThreadPools,path=${path},scope=${stage},name=TotalBlockedTasks"), "Value" ).toString.toLong

    ThreadPoolStats(`type` = stage,
      activeTasks = active,
      completedTasks = completed,
      currentBlockedTasks = currentlyBlocked,
      maxPoolSize = poolSize,
      pendingTasks = pending,
      totalBlockedTasks = -totalBlocked)
  }
}

class JmxClientCassandraUpstream(hostname: String, port: Int, user: Option[String] = None, pwd: Option[String] = None) extends JmxClient(hostname, port, user, pwd)


object JmxClient {

  var jmxClient : Option[JmxClient] = None

  def initInstance(hostname: String, port: Int)(implicit actorSystem: ActorSystem) : Unit = initInstance(hostname, port, None, None)
  def initInstance(hostname: String, port: Int, user: Option[String], pwd: Option[String])(implicit actorSystem: ActorSystem) : Unit = {
    if (jmxClient.isEmpty) {
      jmxClient = try {
        Some(
          if (CassandraVersion.version == 2.1) {
            new JmxClientCassandra21(hostname, port, user, pwd)
          } else {
            new JmxClientCassandraUpstream(hostname, port, user, pwd)
          }
        )
      } catch {
        case e => {
          CommonLoggerFactory.commonLogger.error(e, "Unable to initialize the JMX client, check the configuration. Actor system will terminate...")
          actorSystem.terminate()
          throw e;
        }
      }
    }
  }

  def getInstance() : JmxClient = jmxClient match {
    case None => throw new IllegalStateException("JmxClient must be initialized first")
    case Some(cli) => cli
  }
}
