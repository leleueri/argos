package io.cats.agent

import javax.management._
import javax.management.remote.{JMXConnector, JMXConnectorFactory, JMXServiceURL}
import com.yammer.metrics.reporting.JmxReporter.CounterMBean
import org.apache.cassandra.service.StorageServiceMBean

import scala.collection.JavaConverters._
class CatsEyes(hostname: String, port: Int, user: Option[String] = None, pwd: Option[String] = None) {

  // TODO define val (or var) attributes to keep the JMXProxy references


  def createJMXConnection() : MBeanServerConnection = {
    // TODO manage exception
    val url = new JMXServiceURL(s"service:jmx:rmi:///jndi/rmi://${hostname}:${port}/jmxrmi")
    val connector = user match {
      case Some(login) => JMXConnectorFactory.connect(url, Map(JMXConnector.CREDENTIALS -> Array(login, pwd.get)).asJava)
      case None => JMXConnectorFactory.connect(url)
    }

    // TODO addition of a listener to monitor some action like repair : connector.addlistener...

    connector.getMBeanServerConnection

    // TODO init all jmx proxies here
  }

  private def getStorageMetricLoadProxy(mbsc : MBeanServerConnection) = getStorageMetricProxy(mbsc, "Load")
  private def getStorageMetricTotalHintsProxy(mbsc : MBeanServerConnection) =  getStorageMetricProxy(mbsc, "TotalHints")
  private def getStorageMetricTotalHintsInProgessProxy(mbsc : MBeanServerConnection) =  getStorageMetricProxy(mbsc, "TotalHintsInProgress")
  private def getStorageMetricProxy(mbsc : MBeanServerConnection, name: String) = JMX.newMBeanProxy(mbsc, new ObjectName(s"org.apache.cassandra.metrics:type=Storage,name=${name}"), classOf[CounterMBean], true)

  /**
    * @return Storage Load in Bytes
    */
  def readStorageLoad() : Long = {
    getStorageMetricLoadProxy(createJMXConnection()).getCount
  }

  /**
    * @return
    */
  def getCounterMutationStage() : Map[String, Int] = {
    val attrList : AttributeList = createJMXConnection().getAttributes(new ObjectName("org.apache.cassandra.request:type=CounterMutationStage"), Array("ActiveCount", "CompletedTasks", "CoreThreads", "CurrentlyBlockedTasks", "PendingTasks", "TotalBlockedTasks"))
    attrList.asScala.foldLeft(Map[String, Int]())((acc: Map[String, Int], att : AnyRef) => acc + (att.asInstanceOf[Attribute].getName -> att.asInstanceOf[Attribute].getValue.toString.toInt))
  }

  /**
    * @return
    */
  def getMutationStage() : Map[String, Int] = {
    val attrList : AttributeList = createJMXConnection().getAttributes(new ObjectName("org.apache.cassandra.request:type=MutationStage"), Array("ActiveCount", "CompletedTasks", "CoreThreads", "CurrentlyBlockedTasks", "PendingTasks", "TotalBlockedTasks"))
    attrList.asScala.foldLeft(Map[String, Int]())((acc: Map[String, Int], att : AnyRef) => acc + (att.asInstanceOf[Attribute].getName -> att.asInstanceOf[Attribute].getValue.toString.toInt))
  }

  /**
    * @return
    */
  def getReadRepairStage() : Map[String, Int] = {
    val attrList : AttributeList = createJMXConnection().getAttributes(new ObjectName("org.apache.cassandra.request:type=ReadRepairStage"), Array("ActiveCount", "CompletedTasks", "CoreThreads", "CurrentlyBlockedTasks", "PendingTasks", "TotalBlockedTasks"))
    attrList.asScala.foldLeft(Map[String, Int]())((acc: Map[String, Int], att : AnyRef) => acc + (att.asInstanceOf[Attribute].getName -> att.asInstanceOf[Attribute].getValue.toString.toInt))
  }

  /**
    * @return
    */
  def getReadStage() : Map[String, Int] = {
    val attrList : AttributeList = createJMXConnection().getAttributes(new ObjectName("org.apache.cassandra.request:type=ReadStage"), Array("ActiveCount", "CompletedTasks", "CoreThreads", "CurrentlyBlockedTasks", "PendingTasks", "TotalBlockedTasks"))
    attrList.asScala.foldLeft(Map[String, Int]())((acc: Map[String, Int], att : AnyRef) => acc + (att.asInstanceOf[Attribute].getName -> att.asInstanceOf[Attribute].getValue.toString.toInt))
  }

  /**
    * @return
    */
  def getRequestResponseStage() : Map[String, Int] = {
    val attrList : AttributeList = createJMXConnection().getAttributes(new ObjectName("org.apache.cassandra.request:type=RequestResponseStage"), Array("ActiveCount", "CompletedTasks", "CoreThreads", "CurrentlyBlockedTasks", "PendingTasks", "TotalBlockedTasks"))
    attrList.asScala.foldLeft(Map[String, Int]())((acc: Map[String, Int], att : AnyRef) => acc + (att.asInstanceOf[Attribute].getName -> att.asInstanceOf[Attribute].getValue.toString.toInt))
  }

  /**
    * @return
    */
  def getDroppedMessage() : Map[String, Int] = {
    /*
    org.apache.cassandra.metrics:type=DroppedMessage,scope=_TRACE,name=Dropped
org.apache.cassandra.metrics:type=DroppedMessage,scope=BINARY,name=Dropped
org.apache.cassandra.metrics:type=DroppedMessage,scope=COUNTER_MUTATION,name=Dropped
org.apache.cassandra.metrics:type=DroppedMessage,scope=MUTATION,name=Dropped
org.apache.cassandra.metrics:type=DroppedMessage,scope=PAGED_RANGE,name=Dropped
org.apache.cassandra.metrics:type=DroppedMessage,scope=RANGE_SLICE,name=Dropped
org.apache.cassandra.metrics:type=DroppedMessage,scope=READ_REPAIR,name=Dropped
org.apache.cassandra.metrics:type=DroppedMessage,scope=READ,name=Dropped
org.apache.cassandra.metrics:type=DroppedMessage,scope=REQUEST_RESPONSE,name=Dropped
     */
    val attrList : AttributeList = createJMXConnection().getAttributes(new ObjectName("org.apache.cassandra.request:type=RequestResponseStage"), Array("ActiveCount", "CompletedTasks", "CoreThreads", "CurrentlyBlockedTasks", "PendingTasks", "TotalBlockedTasks"))
    attrList.asScala.foldLeft(Map[String, Int]())((acc: Map[String, Int], att : AnyRef) => acc + (att.asInstanceOf[Attribute].getName -> att.asInstanceOf[Attribute].getValue.toString.toInt))
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

object App {
  def main(args: Array[String]) {
    println("StorageLoad : " + CatsEyes(args(0), args(1).toInt).readStorageLoad())
    println("CounterMutation : " + CatsEyes(args(0), args(1).toInt).getCounterMutationStage())
    println("Mutation : " + CatsEyes(args(0), args(1).toInt).getMutationStage())
    println("ReadRepairMutation : " + CatsEyes(args(0), args(1).toInt).getReadRepairStage())
    println("ReadMutation : " + CatsEyes(args(0), args(1).toInt).getReadStage())
    println("RequestResponseMutation : " + CatsEyes(args(0), args(1).toInt).getRequestResponseStage())
  }
}