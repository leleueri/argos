package io.argos.agent.sentinels

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import io.argos.agent.Constants._
import io.argos.agent.bean.ActorProtocol._
import io.argos.agent.bean._
import io.argos.agent.{Constants, Messages, SentinelConfiguration}
import org.scalatest._

/**
 * Created by eric on 05/07/16.
 */
class TestConsistencySentinel extends TestKit(ActorSystem("TestConsistencySentinel")) with FlatSpecLike with Matchers with BeforeAndAfterAll {

  val globalConfig = ConfigFactory.load()
  val configJmx = globalConfig.getConfig(CONF_OBJECT_ENTRY_METRICS)

  val metricsProviderProbe = TestProbe()

  val notificationProbe = TestProbe()

  system.eventStream.subscribe(
    notificationProbe.ref,
    classOf[Notification])

  val rrBlockingActor = system.actorOf(Props(classOf[ReadRepairBlockingSentinel], metricsProviderProbe.ref, SentinelConfiguration("test", globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_CONSISTENCY_RR_BLOCKING))))
  val rrBackgroundActor = system.actorOf(Props(classOf[ReadRepairBackgroundSentinel], metricsProviderProbe.ref, SentinelConfiguration("test", globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_CONSISTENCY_RR_BACKGROUND))))

  override def afterAll() {
    system.terminate()
  }

  "A notification " should " be sent on ReadrepairedBlocking tasks" in {
    executeTest(rrBlockingActor, Messages.READ_REPAIR_BLOCKING)
  }

  "A notification " should " be sent on ReadrepairedBackground tasks" in {
    executeTest(rrBackgroundActor, Messages.READ_REPAIR_BACKGROUND)
  }

  private def executeTest(testableActorRef : ActorRef, dmType: String): Unit = {
    testableActorRef ! CheckMetrics()

    metricsProviderProbe.expectMsg(MetricsRequest(ActorProtocol.ACTION_CHECK_READ_REPAIR, dmType))
    metricsProviderProbe.reply(MetricsResponse(ACTION_CHECK_READ_REPAIR, Some(ReadRepairStats(dmType, 0, 0, 0, 0, 0))))

    notificationProbe.expectNoMsg()

    testableActorRef ! CheckMetrics()

    metricsProviderProbe.expectMsg(MetricsRequest(ActorProtocol.ACTION_CHECK_READ_REPAIR, dmType))
    // non zero one minute rate should trigger a notification
    metricsProviderProbe.reply(MetricsResponse(ACTION_CHECK_READ_REPAIR, Some(ReadRepairStats(dmType, 1, 0.0, 0.0, 0.0, 0.1))))

    val notif = notificationProbe.expectMsgAnyClassOf(classOf[Notification])
    assert(notif.message.contains(dmType))
  }
}
