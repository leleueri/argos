package io.argos.agent.sentinels

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import io.argos.agent.Constants._
import io.argos.agent.Messages
import io.argos.agent.bean.ActorProtocol._
import io.argos.agent.bean._
import org.scalatest._

/**
 * Created by eric on 05/07/16.
 */
class TestTimeoutSentinel extends TestKit(ActorSystem("TestDroppedSentinel")) with FlatSpecLike with Matchers with BeforeAndAfterAll {

  val globalConfig = ConfigFactory.load()
  val configJmx = globalConfig.getConfig(CONF_OBJECT_ENTRY_METRICS)

  val metricsProviderProbe = TestProbe()

  val notificationProbe = TestProbe()

  system.eventStream.subscribe(
    notificationProbe.ref,
    classOf[Notification])

  val cnxTimeoutActor = system.actorOf(Props(classOf[ConnectionTimeoutSentinel], metricsProviderProbe.ref, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_CONNECTION_TIMEOUTS)))

  override def afterAll() {
    system.terminate()
  }

  "A notification " should " be sent on Connection Timeout " in {
    executeTest(cnxTimeoutActor)
  }


  private def executeTest(testableActorRef : ActorRef): Unit = {
    testableActorRef ! CheckMetrics()

    metricsProviderProbe.expectMsg(MetricsRequest(ActorProtocol.ACTION_CHECK_CNX_TIMEOUT, Messages.CNX_TIMEOUT))
    metricsProviderProbe.reply(MetricsResponse(ACTION_CHECK_CNX_TIMEOUT, Some(ConnectionTimeoutStats(0, 0, 0, 0, 0))))

    notificationProbe.expectNoMsg()

    testableActorRef ! CheckMetrics()

    metricsProviderProbe.expectMsg(MetricsRequest(ActorProtocol.ACTION_CHECK_CNX_TIMEOUT, Messages.CNX_TIMEOUT))
    // non zero one minute rate should trigger a notification
    metricsProviderProbe.reply(MetricsResponse(ACTION_CHECK_CNX_TIMEOUT, Some(ConnectionTimeoutStats(1, 0.0, 0.0, 0.0, 0.1))))

    val notif = notificationProbe.expectMsgAnyClassOf(classOf[Notification])
    assert(notif.message.contains("some connection timeouts"))
  }
}
